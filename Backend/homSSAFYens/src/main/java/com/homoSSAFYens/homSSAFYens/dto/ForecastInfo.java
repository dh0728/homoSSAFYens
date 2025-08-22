package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDateTime;

public record ForecastInfo(

        LocalDateTime time,   // yyyy-MM-ddTHH:mm
        String sky,
        String skyCode,
        int rain,             // mm/h
        double rainAmt,       // mm
        double temp,          // ℃
        String winddir,
        double windspd,       // m/s
        double humidity,      // %
        double wavePrd,       // sec
        double waveHt,        // m
        String waveDir


) {
}
