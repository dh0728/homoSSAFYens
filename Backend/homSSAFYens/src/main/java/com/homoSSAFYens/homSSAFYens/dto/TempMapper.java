package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TempMapper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TempMapper() {}

    public static TempInfo toDomain(TempExternalDto e) {
        if (e == null) return null;

        return new TempInfo(
                toDouble(e.lat),
                toDouble(e.lon),
                trim(e.obsName),
                toDouble(e.obsWt),
                parseObsTime(e.obsTime),
                parseDistanceKm(e.obsDt)
        );
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
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

    // obs_time 파싱: 여러 포맷을 순서대로 시도
    private static LocalDateTime parseObsTime(String s) {
        s = trim(s);
        if (s == null || s.isEmpty()) return null;

        try {
            return LocalDateTime.parse(s, dateTimeFormatter);
        } catch (Exception ignore) { /* 다음 포맷 시도 */ }
            return null; // 실패 시 null (필드가 reference type 이라 NPE 없음)
    }

    // obs_dt 파싱: "X km" 또는 "Y m" → km 단위 double
    private static double parseDistanceKm(String s) {
        s = trim(s);
        if (s == null || s.isEmpty()) return Double.NaN;

        // 단위 표기 정규화
        s = s.replace("㎞", "km");          // 유니코드 km
        s = s.replace(',', '.');            // 0,8 → 0.8
        s = s.replaceAll("\\s+", ""); // 공백 제거: "4 km" -> "4km"

        // 숫자 추출
        var m = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(s);
        if (!m.find()) return Double.NaN;

        double value = Double.parseDouble(m.group(1));

        // 단위 판단
        if (s.endsWith("km") || s.contains("km")) {
            return value; // km 그대로
        }
        if (s.endsWith("m") || s.matches(".*\\d+m.*")) {
            return value / 1000.0; // m → km
        }
        // 단위 표기가 없으면 km로 간주
        return value;
    }


}
