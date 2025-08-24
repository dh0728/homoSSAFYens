package com.homoSSAFYens.homSSAFYens.service;


import com.homoSSAFYens.homSSAFYens.common.TideAlertPlanner;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
    private final DangerZoneService dangerZoneService;

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


        // highs 에는 TideHighInfo가 한개일수도 두개 일수도 있음 각각 만조 시간에 대한 리스트를 뽑아와야함
        TideDailyInfo tideDailyInfo = tideService.getDaily(lat, lon);
        // tideDatlyInfo 에는 간만조 시간이 순서대로 들어있음
        // 간조와 만조 중에 뭐가 먼저 인지는 모름
//      데이터 예시 tideDailyInfo.events();
//        "events": [
//        {
//            "time": "00:11:00",
//                "levelCm": 91,     물높이
//                "trend": "RISING",
//                "deltaCm": 57      간조와 비교했을 때 물이 올라온 정도
//        },
//        {
//            "time": "06:24:00",
//                "levelCm": 35,
//                "trend": "FALLING",
//                "deltaCm": -56
//        },
//        {
//            "time": "13:11:00",
//                "levelCm": 91,
//                "trend": "RISING",
//                "deltaCm": 56
//        },
//        {
//            "time": "18:54:00",
//                "levelCm": 44,
//                "trend": "FALLING",
//                "deltaCm": -47
//        }
//        ]

        // 정확하게 할려면 전에 간조 시간부터 디비에서 조회한 조위상승 속도로 계산했을 때 다음 만조시간까지에 올라간 cm와
        // 다음 만조시에 deltaCm 보다 크면 좀더 일찍 알려주는 이런식으로 해야 정확한게 아닌가 하는 생각이듬
        // 근데 전날 물때 시간은 알수 없어서 현재일 기준 만조전 간조가 없는 만조는 그냥 30분 과 10분 알림
        // 앞에 간조가 있는건 디비에서 조회후 속도로 계산한 값보다 deltaCm이 크면? 작으면? 할때 좀 일찍하거나 하는게
        // 좋지 않을까 늦게 하는 경우는 안전상 없게 하고



        List<Integer> offsets = offsetService.forDevice(deviceId);

        for (TideHighInfo hi : highs) {
            long T = hi.epochSecond();
            String locationName = hi.locationName();

            // === case 분기: 오늘자 직전 간조 유무만 본다 ===
            var prevLowOpt = TideCalcUtil.findPrevLowEpoch(tideDailyInfo, T, ZoneId.of("Asia/Seoul"));

            List<Integer> offsetsToUse;

            if (prevLowOpt.isEmpty()) {
                // case 2: 앞 간조가 없음 → 기본 오프셋
                planWithOffsets(deviceId, geoKey, nowEpoch, hi, locationName, offsets);

            } else {
                // case 1: 앞 간조 L 있음 → L~T 구간 속도 기반 동적 오프셋
                long L = prevLowOpt.getAsLong();

                // 반경 5km, L~T 구간 시간별 상승속도(BigDecimal) 조회
                List<BigDecimal> speeds = dangerZoneService.loadRiseSpeeds(lat, lon, 5.0, L, T);

                // 통계/적분
                var stats = TideAlertPlanner.statsBD(speeds);      // vmax, vmean(+)
                double riseEstimateCm = TideAlertPlanner.integrateRiseCm(speeds, L, T);

                // 바다타임 deltaCm(T) 읽기 (오늘 데이터만 사용)
                int deltaCm = TideCalcUtil.deltaAt(tideDailyInfo, T); // 없으면 0 처리되도록 구현

                double ratio = (deltaCm <= 0) ? 1.0 : (riseEstimateCm / deltaCm);

                log.info("[OffsetCalc] T={}, L={}, riseCm={}, deltaCm={}, ratio={}",
                        T, L, riseEstimateCm, deltaCm, String.format("%.2f", ratio));

                // 속도 기반 동적 오프셋 (앞당김만, 뒤로 미루지 않음)
                offsetsToUse = TideAlertPlanner.adjustOffsets(stats.vmax(), stats.vmeanPos(), riseEstimateCm, deltaCm);
                planWithOffsets(deviceId, geoKey, nowEpoch, hi, locationName, offsetsToUse);
            }


        }
    }

    private void planWithOffsets(String deviceId, String geoKey, long nowEpoch,
                                 TideHighInfo hi, String locationName, List<Integer> offsets) {
        long T = hi.epochSecond();
        String sIdxKey = idxKey(deviceId);
//        int i = 10;
        for (int off : offsets) {
            long triggerAt = T + off * 60L;
//            long triggerAt = nowEpoch + i;
            if (triggerAt <= nowEpoch + 60) continue; // 임박 스킵(옵션)

            LocalDateTime triggerTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(triggerAt),
                    ZoneId.of("Asia/Seoul")
            );

            log.info("[OffsetCalc] T={} ({}), offset={} → triggerAt={}",
                    T,
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(T), ZoneId.of("Asia/Seoul")),
                    off,
                    triggerTime);

            String member = T + ":" + off;
            Boolean dup = redis.opsForSet().isMember(sIdxKey, member);

            String eid = eventId(deviceId, geoKey, T, off);
            JobKey jobKey = JobKey.jobKey(eid, deviceId);
            TriggerKey trgKey = TriggerKey.triggerKey(eid, deviceId);

            try {
                if (Boolean.TRUE.equals(dup) && scheduler.checkExists(trgKey)) {
                    log.debug("idempotent-skip {} {}", deviceId, member);
                    continue;
                }
                if (Boolean.TRUE.equals(dup) && !scheduler.checkExists(trgKey)) {
                    redis.opsForSet().remove(sIdxKey, member); // 유령 SET 치유
                }

                String lockKey = "sched:" + eid;
                if (!cacheService.tryLock(lockKey, Duration.ofSeconds(3))) {
                    log.debug("skip duplicate schedule attempt for {}", eid);
                    continue;
                }

                if (!scheduler.checkExists(jobKey)) {
                    JobDataMap map = new JobDataMap();
                    map.put("deviceId", deviceId);
                    map.put("tideTs", T);
                    map.put("offset", off);
                    map.put("eventId", eid);
                    map.put("geoKey", geoKey);
                    map.put("locationName", locationName);

                    JobDetail job = JobBuilder.newJob(TideNotifyJob.class)
                            .withIdentity(jobKey)
                            .usingJobData(map)
                            .storeDurably(true)
                            .build();
                    scheduler.addJob(job, true);
                }

                if (!scheduler.checkExists(trgKey)) {
                    Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(trgKey)
                            .forJob(jobKey)
                            .startAt(Date.from(Instant.ofEpochSecond(triggerAt)))
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                    .withMisfireHandlingInstructionFireNow())
                            .build();
                    scheduler.scheduleJob(trigger);
                    log.info("scheduled {} @{}", eid, triggerAt);

                    redis.opsForSet().add(sIdxKey, member);
                    redis.expire(sIdxKey, Duration.ofDays(indexTtlDays));
                }

            } catch (SchedulerException e) {
                log.error("schedule error {}", eid, e);
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
