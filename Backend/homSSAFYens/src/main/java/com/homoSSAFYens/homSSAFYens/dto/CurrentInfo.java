package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDateTime;

public record CurrentInfo(
        String sky,             // 날씨 설명
        double rainMm,          // 강수량(mm)
        double tempC,           // 기온(℃)
        String pm25S,           // PM2.5 등급
        String pm10S,           // PM10  등급
        Integer pm10,           // PM10 농도(µg/m3)
        String windDir,         // 풍향(예: N/NE/E/SE/S/SW/W/NW)
        double waveHeightM,            // 파고(m) - pago
        String skyCode,            // 하늘 상태 코드
        double windSpeedMs,     // 풍속(m/s)
        Integer pm25,           // PM2.5 농도(µg/m3)
        LocalDateTime time,     // 관측시각
        double humidityPct      // 습도(%)
) {}
