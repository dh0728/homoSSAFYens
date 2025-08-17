package com.homoSSAFYens.homSSAFYens.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CurrentMapper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private CurrentMapper() {}

    public static CurrentInfo toDomain(CurrentExternalDto e) {
        if (e == null) return null;

        return new CurrentInfo(
                trim(e.sky),
                toDouble(e.rain),
                toDouble(e.temp),
                trim(e.pm25_s),
                trim(e.pm10_2),
                toInt(e.pm10),
                trim(e.winddir),
                toDouble(e.page),
                trim(e.sky_code),
                toDouble(e.windspd),
                toInt(e.pm25),
                parseTime(e.aplYmdt),
                toDouble(e.humidity)
        );
    }

    public static List<CurrentInfo> toDomainList(List<CurrentExternalDto> list) {
        return (list == null || list.isEmpty())
                ? List.of() :
                list.stream().map(CurrentMapper::toDomain).toList();
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

    /**
     * 숫자 문자열을 {@code Integer}로 변환한다.
     *
     * <p>특징:
     * <ul>
     *   <li>앞뒤 공백 제거</li>
     *   <li>파싱 실패/빈 문자열/널이면 {@code null} 반환</li>
     * </ul>
     *
     * @param s 숫자 형태의 문자열(예: "18", " 42 ")
     * @return 변환된 {@code Integer} 값, 실패 시 {@code null}
     */
    private static Integer toInt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 관측시각 문자열(yyyyMMddHH)을 {@link LocalDateTime}으로 변환한다.
     *
     * <p>예: "2025081714" → 2025-08-17T14:00
     * <br>길이가 다르거나 파싱 실패/널이면 {@code null} 반환.
     *
     * @param ymdh 문자열 시각 (예: "2025081714")
     * @return 변환된 {@code LocalDateTime} 또는 {@code null}
     */
    private static LocalDateTime parseTime(String ymdh) {
        if (ymdh == null || ymdh.length() != 10) return null;

        try{
            return LocalDateTime.parse(ymdh, dateTimeFormatter);

        } catch (Exception e) {
            return null;
        }
    }

    public static CurrentResponse toResponse(CurrentEnvelope env) {
        if (env == null) {
            return new CurrentResponse(List.of(), null);
        }
        var list = toDomainList(env.weather()); // 이미 만든 외부→내부 리스트 매핑
        CurrentCity city = null;
        if (env.info() != null) {
            city = new CurrentCity(
                    trim(env.info().city()),
                    trim(env.info().cityCode())
            );
        }
        return new CurrentResponse(list, city);
    }
}
