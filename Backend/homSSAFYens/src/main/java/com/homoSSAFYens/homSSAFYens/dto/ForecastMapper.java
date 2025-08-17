package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ForecastMapper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private ForecastMapper() {}

    public static ForecastInfo toDomain(ForecastExternalDto e) {
        if (e == null) return null;

        return new ForecastInfo(
                parseTime(e.ymdt),
                trim(e.sky),
                trim(e.skyCode),
                toInt(e.rain),
                toDouble(e.rainAmt),
                toInt(e.temp),
                trim(e.winddir),
                toDouble(e.windspd),
                toDouble(e.humidity),
                toDouble(e.wavePrd),
                toDouble(e.waveHt),
                trim(e.waveDir)
        );
    }


    public static List<ForecastInfo> toDomainList(List<ForecastExternalDto> list) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream()
                .map(ForecastMapper::toDomain)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ForecastInfo::time)) // 시간 오름차순
                .toList();
    }

    /* ===== 날짜별 그룹핑 (외부 리스트 → 일자별) ===== */
    public static List<ForecastDay> groupByDateFromExternal(List<ForecastExternalDto> raw) {
        var hourly = toDomainList(raw);
        return groupByDateFromHourly(hourly);
    }

    /* ===== 날짜별 그룹핑 (내부 시간 리스트 → 일자별) ===== */
    public static List<ForecastDay> groupByDateFromHourly(List<ForecastInfo> hourly) {
        if (hourly == null || hourly.isEmpty()) return List.of();

        Map<LocalDate, List<ForecastInfo>> grouped = hourly.stream()
                .collect(Collectors.groupingBy(
                        h -> h.time().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(e -> new ForecastDay(e.getKey(), e.getValue()))
                .toList();
    }

    /* ===== 최종 응답 래핑 ===== */
    public static ForecastResponse toResponse(List<ForecastExternalDto> raw) {
        return new ForecastResponse(groupByDateFromExternal(raw));
    }


    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static Integer toInt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static double toDouble(String s) {
        if (s == null || s.isBlank()) return Double.NaN;

        // 0,0 같은 콤마 표기로 대비 - 이럴 일이 있나 싶지만 혹시나~~
        s = s.trim().replace(',', '.');

        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static LocalDateTime parseTime(String ymdh) {
        if (ymdh == null || ymdh.length() != 10) return null;

        try{
            return LocalDateTime.parse(ymdh, dateTimeFormatter);

        } catch (Exception e) {
            return null;
        }
    }

}
