package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDateTime;

public record TempInfo(
        double lat,
        double lon,
        String obsName,
        double obsWt,
        LocalDateTime obsTime,
        double obsDt // 다 km 미터로 변환 해서 넣을거임
) {
}
