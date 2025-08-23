package com.homoSSAFYens.homSSAFYens.dto;

public record DeviceRegisterRequest(
        String deviceId,
        String fcmToken,
        LocationDto location
) {}