package com.homoSSAFYens.homSSAFYens.dto;

public record FineDustValue(
        Double value,
        String level, // "좋음" | "보통" | "나쁨" | 모름
        String unit   // "㎍/m³" | "ppm" | "지수"
) {
}
