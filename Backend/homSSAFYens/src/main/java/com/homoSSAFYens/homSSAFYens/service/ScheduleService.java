package com.homoSSAFYens.homSSAFYens.service;


import com.homoSSAFYens.homSSAFYens.common.TideCalcUtil;
import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.dto.TideHighInfo;
import com.homoSSAFYens.homSSAFYens.quartz.TideNotifyJob;
import com.homoSSAFYens.homSSAFYens.repo.Keys;
import com.homoSSAFYens.homSSAFYens.repo.TideRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * /locate 수신 시, 향후 만조시각(T)에 대해 (T + offset 분) 시점으로 Quartz 트리거를 예약한다.
 * - 이동 시: 임박(= graceSec 이내) 예약만 남기고 나머지 정리
 * - 멱등: (deviceId, tideTs, offset) 조합을 Redis SET으로 관리하여 중복 예약 방지
 * - 동시성: 디바이스 단위 락으로 취소↔스케줄 레이스 방지
 * - Quartz: addJob(durable=true) 업서트 + trigger는 별도로 schedule
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final Scheduler scheduler;
    private final TideRepo tideRepo;
    private final OffsetService offsetService;
    private final TideService tideService;
    private final CacheService cacheService;
    private final StringRedisTemplate redis;

    private long graceSec = 60;  //  임박 잡 살려둘 시간(초)
    private long indexTtlDays = 3; //  멱등 인덱스 SET TTL(일)


    /** eventId: (device + geoKey + tideTs + offset)의 해시를 간단히 문자열로 */
    private String eventId(String deviceId, String geoKey, long tideTs, int offset) {
        return "evt_" + Math.abs(Objects.hash(deviceId, geoKey, tideTs, offset));
    }

    /** 디바이스별 멱등 인덱스 키 */
    private String idxKey(String deviceId) {
        return "sched:idx:" + deviceId;
    }

    /** 디바이스 직렬화 락 키 */
    private String deviceLockKey(String deviceId) {
        return "sched:devlock:" + deviceId;
    }

    public void scheduleFor(String deviceId, String geoKey, long nowEpoch, double lat, double lon) {


        // 🔒 디바이스 단위 직렬화 락 (레이스 방지: 취소 ↔ 스케줄)
        String devLock = idxKey(deviceId);
        if (!cacheService.tryLock(devLock, Duration.ofSeconds(3))) {
            log.debug("skip scheduleFor due to device lock {}", deviceId);
            return;
        }


        // 1) 임박(graceSec) 제외하고 기존 예약 정리
        cancelFutureForDeviceExceptGrace(deviceId, nowEpoch, graceSec);

        // 1-보강) 취소 이후 남아있는 잡 기준으로 멱등 SET 재동기화 (유령 인덱스 제거)
        reconcileIdxWithScheduler(deviceId);


        // 2) 만조 시간 로딩
        List<TideHighInfo> highs = tideRepo.upcomingHighTidesFromDaily(geoKey, nowEpoch);

        if (highs.isEmpty()) {
            TideDailyInfo daily = tideService.getDaily(lat, lon); //외부 api 호출 + 캐싱
            if (daily != null) {
                highs = TideCalcUtil.highsFromDaily(daily, ZoneId.of("Asia/Seoul"), nowEpoch);
            }
        }

        if (highs.isEmpty()) {
            log.warn("No tide data available for geoKey={} (lat={}, lon={})", geoKey, lat, lon);
            return;
        }

        List<Integer> offsets = offsetService.forDevice(deviceId);
        String sIdxKey = idxKey(deviceId); // 멱등 인덱스 SET

        int testTime = 10;
        for (TideHighInfo hi : highs) {
            long T = hi.epochSecond();
            String locationName = hi.locationName();

            for (int off : offsets) {
//                long triggerAt = T + off * 60L;
                long triggerAt = nowEpoch + testTime; //테스트용
                testTime += 10;
//                if (triggerAt <= nowEpoch + 60) continue;


                // 3) 멱등 체크: (T, off) 조합이 SET에 있으면 스킵하되,
                //    트리거가 실제로 없으면 오래된 인덱스로 간주하고 SET에서 제거 후 계속 진행
                String member = T + ":" + off;
                Boolean dup = redis.opsForSet().isMember(sIdxKey, member);

                String eid = eventId(deviceId, geoKey, T, off);
                JobKey jobKey = JobKey.jobKey(eid, deviceId);
                TriggerKey trgKey = TriggerKey.triggerKey(eid, deviceId);

                if (Boolean.TRUE.equals(dup)) {
                    try {
                        if (!scheduler.checkExists(trgKey)) {
                            // 유령 인덱스 치유
                            redis.opsForSet().remove(sIdxKey, member);
                        } else {
                            log.debug("idempotent-skip {} {}", deviceId, member);
                            continue;
                        }
                    } catch (SchedulerException e) {
                        log.error("idempotent check error {}", eid, e);
                        continue;
                    }
                }

                // 🔒 동일 이벤트 중복 스케줄 방지 (race 방지)
                String lockKey = "sched:" + eid;
                if (!cacheService.tryLock(lockKey, Duration.ofSeconds(3))) {
                    log.debug("skip duplicate schedule attempt for {}", eid);
                    continue;
                }

                try {
                    // 이미 같은 잡 예약되어 있으면 스킵(중복 방지)
                    if (scheduler.checkExists(jobKey)) continue;

                    JobDataMap map = new JobDataMap();
                    map.put("deviceId", deviceId);
                    map.put("tideTs", T);
                    map.put("offset", off);
                    map.put("eventId", eid);
                    map.put("geoKey", geoKey);
                    map.put("locationName", locationName);

                    // 1) 잡은 업서트(add or replace)
                    JobDetail job = JobBuilder.newJob(TideNotifyJob.class)
                            .withIdentity(jobKey)
                            .usingJobData(map)
                            .storeDurably(true) // durable == true 해야함
                            .build();
                    scheduler.addJob(job, true); // replace = true (이미 있으면 교체)

                    // 2) 트리거 있으면 skip(또는 reschedule)
                    if (scheduler.checkExists(trgKey)) {
                        // 필요하면 다음처럼 시간만 갱신:
                        // Trigger newTrg = TriggerBuilder.newTrigger()
                        //     .withIdentity(trgKey)
                        //     .forJob(jobKey)
                        //     .startAt(Date.from(Instant.ofEpochSecond(triggerAt)))
                        //     .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        //         .withMisfireHandlingInstructionFireNow())
                        //     .build();
                        // scheduler.rescheduleJob(trgKey, newTrg);
                        continue;
                    }

                    // 3) 새 트리거만 등록
                    Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(trgKey)
                            .forJob(jobKey) // 주의: jobKey로 연결(존재 보장됨)
                            .startAt(Date.from(Instant.ofEpochSecond(triggerAt)))
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                    .withMisfireHandlingInstructionFireNow())
                            .build();

                    scheduler.scheduleJob(trigger);
                    log.info("scheduled {} @{}", eid, triggerAt);

                    // 6) 멱등 인덱스 등록 + TTL
                    redis.opsForSet().add(sIdxKey, member);
                    redis.expire(sIdxKey, Duration.ofDays(indexTtlDays));

                } catch (SchedulerException e) {
                    log.error("schedule error {}", eid, e);
                }
            }
        }
    }

    /** (옵션) 디바이스의 기존 예약을 전부 지우고 싶을 때 */
    public void cancelAllForDevice(String deviceId) {
        try {
            for (JobKey k : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(deviceId))) {
                scheduler.deleteJob(k);
            }
        } catch (SchedulerException e) {
            log.error("cancelAll error {}", deviceId, e);
        }
    }

    /**
     * 그레이스 취소:
     * - now + graceSec 이내로 실행될 예약은 유지
     * - 그 외(먼 미래) 예약은 삭제
     * - 삭제되는 예약에 대응하는 멱등 SET 멤버도 함께 제거
     */
    public void cancelFutureForDeviceExceptGrace(String deviceId, long nowEpoch, long graceSec) {
        try {
            for (JobKey jk : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(deviceId))) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jk);
                boolean keep = false;
                for (Trigger t : triggers) {
                    Date next = t.getNextFireTime();
                    if (next == null) continue;
                    long ts = next.toInstant().getEpochSecond();
                    if (ts <= nowEpoch + graceSec) {
                        keep = true; // 임박한 건 살려둔다
                        break;
                    }
                }
                if (!keep) {
                    // 삭제 전에 멱등 SET에서 멤버 제거
                    JobDetail jd = scheduler.getJobDetail(jk);
                    if (jd != null) {
                        JobDataMap m = jd.getJobDataMap();
                        Object tideTsObj = m.get("tideTs");
                        Object offObj = m.get("offset");
                        if (tideTsObj != null && offObj != null) {
                            long tideTs = (tideTsObj instanceof Number) ? ((Number) tideTsObj).longValue()
                                    : Long.parseLong(tideTsObj.toString());
                            int off = (offObj instanceof Number) ? ((Number) offObj).intValue()
                                    : Integer.parseInt(offObj.toString());
                            String member = tideTs + ":" + off;
                            redis.opsForSet().remove(idxKey(deviceId), member);
                        }
                    }
                    scheduler.deleteJob(jk);
                }
            }
        } catch (SchedulerException e) {
            log.error("cancelFuture error {}", deviceId, e);
        }
    }

    /**
     * 멱등 SET 재동기화:
     * - 현재 스케줄러에 남아있는 잡들만 기준으로 SET을 재구성
     * - 유령 인덱스(SET에는 있으나 잡/트리거 없음)는 일괄 제거
     */
    private void reconcileIdxWithScheduler(String deviceId) {
        try {
            java.util.Set<String> should = new java.util.HashSet<>();
            for (JobKey jk : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(deviceId))) {
                JobDetail jd = scheduler.getJobDetail(jk);
                if (jd == null) continue;
                JobDataMap m = jd.getJobDataMap();
                Object tideTsObj = m.get("tideTs");
                Object offObj = m.get("offset");
                if (tideTsObj != null && offObj != null) {
                    long tideTs = (tideTsObj instanceof Number)
                            ? ((Number) tideTsObj).longValue()
                            : Long.parseLong(tideTsObj.toString());
                    int off = (offObj instanceof Number)
                            ? ((Number) offObj).intValue()
                            : Integer.parseInt(offObj.toString());
                    should.add(tideTs + ":" + off);
                }
            }
            String key = idxKey(deviceId);
            redis.delete(key);
            if (!should.isEmpty()) {
                redis.opsForSet().add(key, should.toArray(String[]::new));
                redis.expire(key, Duration.ofDays(indexTtlDays));
            }
        } catch (SchedulerException e) {
            log.error("reconcileIdx error {}", deviceId, e);
        }
    }
}
