package com.homoSSAFYens.homSSAFYens.dto;

import com.google.firebase.database.annotations.NotNull;

public record RegisterReq(

        @NotNull String deviceId,
        @NotNull String fcmToken
) {
}
