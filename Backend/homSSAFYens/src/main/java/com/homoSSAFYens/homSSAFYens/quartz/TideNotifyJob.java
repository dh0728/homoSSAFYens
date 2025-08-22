package com.homoSSAFYens.homSSAFYens.quartz;

import com.homoSSAFYens.homSSAFYens.client.FcmClient;
import com.homoSSAFYens.homSSAFYens.repo.Keys;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Quartz가 트리거 시점에 호출하는 Job.
 * 지금 단계는 '발송 준비' 로그만. (3단계에서 FCM 호출을 붙인다)
 */
@Slf4j
@Component
public class TideNotifyJob implements Job {

    private final StringRedisTemplate redis;
    private final FcmClient fcm;

    public TideNotifyJob(StringRedisTemplate redis, FcmClient fcm) {
        this.redis = redis;
        this.fcm = fcm;
    }

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data = ctx.getMergedJobDataMap();

        String deviceId = data.getString("deviceId");
        long tideTs    = data.getLong("tideTs");
        int offset     = data.getInt("offset");
        String eventId = data.getString("eventId");
        String geoKey  = data.getString("geoKey");
        String locationName = data.getString("locationName");

        try {
            // 1) 디바이스 fcm 토큰 조회
            String token = (String) redis.opsForHash().get(Keys.device(deviceId), "fcm");
            if (token == null || token.isBlank()) {
                log.warn("no fcm token: device={}", deviceId);
                return;
            }

            // 2) data-only 메시지 전송
            String msg = locationName + " 만조 " + Math.abs(offset) + "분 전";
            fcm.sendData(token, Map.of(
                    "type", "TIDE_SOON",
                    "eventId", eventId,
                    "tideTs", String.valueOf(tideTs),
                    "offset", String.valueOf(offset),
                    "geoKey", geoKey,
                    "message", msg
            ));

            log.info("FCM sent: device={} evt={} tideTs={} offset={}",
                    deviceId, eventId, tideTs, offset);

        } catch (Exception e) {
            log.error("FCM send error device={} evt={}", deviceId, eventId, e);
            throw new JobExecutionException(e, false);
        }
    }
}

