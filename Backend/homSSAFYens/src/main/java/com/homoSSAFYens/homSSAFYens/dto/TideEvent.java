package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalTime;

public record TideEvent (

    LocalTime time,
    Integer levelCm, // 수위
    Trend trend,
    Integer deltaCm
) {
    public enum Trend {
        RISING,
        FALLING,
    }
}
