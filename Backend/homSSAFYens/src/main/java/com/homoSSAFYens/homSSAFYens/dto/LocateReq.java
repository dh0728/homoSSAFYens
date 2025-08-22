package com.homoSSAFYens.homSSAFYens.dto;

import com.google.firebase.database.annotations.NotNull;

public record LocateReq (
        @NotNull String deviceId,
        double lat,
        double lon,
        long ts // epoch seconds (UTC)
)
{}
