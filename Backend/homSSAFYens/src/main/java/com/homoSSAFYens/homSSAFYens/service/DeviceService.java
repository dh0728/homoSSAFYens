package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.LocateReq;
import com.homoSSAFYens.homSSAFYens.repo.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final StringRedisTemplate redis;

    public void register(String deviceId, String fcm) {
        Map<String, String> h = new HashMap<>();
        h.put("deviceId", deviceId);
        h.put("fcm", fcm);

        redis.opsForHash().putAll(Keys.device(deviceId), h);

    }

    public void updateLastLocation(String deviceId, String geoKey, double lat, double lon, long ts) {
        redis.opsForHash().put(Keys.device(deviceId), "geoKey", geoKey);
        redis.opsForHash().put(Keys.device(deviceId), "lastLat", String.valueOf(lat));
        redis.opsForHash().put(Keys.device(deviceId), "lastLon", String.valueOf(lon));
        redis.opsForHash().put(Keys.device(deviceId), "lastTs", String.valueOf(ts));
    }
}
