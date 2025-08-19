package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record FineDustResponse(
        LocalDateTime time,
        String stationName,
        Map<String, FineDustValue> data // pm10, pm25, o3, co, so2, no2, khai
) {
}
