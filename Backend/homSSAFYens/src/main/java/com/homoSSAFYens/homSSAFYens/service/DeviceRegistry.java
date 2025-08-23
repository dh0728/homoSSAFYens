package com.homoSSAFYens.homSSAFYens.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.homoSSAFYens.homSSAFYens.dto.LocationDto;

@Component
public class DeviceRegistry {

    public static class DeviceInfo {
        public final String deviceId;
        public final String fcmToken;
        public volatile LocationDto lastLocation;

        public DeviceInfo(String deviceId, String fcmToken, LocationDto loc) {
            this.deviceId = deviceId;
            this.fcmToken = fcmToken;
            this.lastLocation = loc;
        }
    }

    private final Map<String, DeviceInfo> devices = new ConcurrentHashMap<>();

    public void upsert(String deviceId, String token, LocationDto loc) {
        devices.put(deviceId, new DeviceInfo(deviceId, token, loc));
    }

    public Map<String, DeviceInfo> snapshot() {
        return Map.copyOf(devices);
    }
}
