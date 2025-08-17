package com.homoSSAFYens.homSSAFYens.dto;


import java.time.LocalDate;
import java.util.List;

public record ForecastDay(

        LocalDate date,
        List<ForecastInfo> hours // 3시간 단위 날씨 데이터


) {
}
