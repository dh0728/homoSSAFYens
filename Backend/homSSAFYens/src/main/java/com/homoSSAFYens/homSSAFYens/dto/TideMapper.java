package com.homoSSAFYens.homSSAFYens.dto;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class TideMapper {

    private TideMapper() {}

    // "03:10 (17) ▼ -13"
    private static final Pattern JOWI = Pattern.compile(
            "^(\\d{2}:\\d{2})\\s*\\((-?\\d+)\\)\\s*([▲▼])\\s*([+-]?\\d+)\\s*$"
    );

    /** "05:51/19:00" 형태 파싱 */
    private static LocalTime[] parsePair(String s) {
        if (s == null || !s.contains("/")) {
            return new LocalTime[]{null, null};
        }
        String[] parts = s.split("/");
        return new LocalTime[] {
                toTime(parts[0]), parts.length > 1 ? toTime(parts[1]) : null
        };
    }

    private static LocalTime toTime(String hhmm) {
        if (hhmm == null || !hhmm.contains(":")) {
            return null;
        }
        String[] p = hhmm.trim().split(":");
        return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    /** "2025-7-21-월-6-27" → (LocalDate, "월", "6-27") */
    private static ParsedDate parseDate(String s) {

        if ( s == null) {
            return new ParsedDate(null, null, null);
        }
        String[] parts = s.split("-");
        try {
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            String weekday = parts.length >= 4 ? parts[3] : null;
            String lunar = parts.length >= 6 ? (parts[4] + "-" + parts[5]) : null;
            return new ParsedDate(LocalDate.of(y, m, d), weekday, lunar);
        } catch (Exception e) {
            return new ParsedDate(null, null, null);
        }
    }

    private record ParsedDate(LocalDate date, String weekday, String lunar) {}

    /** "03:10 (17) ▼ -13" → TideEvent */
    private static TideEvent parseJowi(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        Matcher m = JOWI.matcher(s.trim());

        // 🔧 버그 수정: 매칭에 실패하면 null 반환해야 함 (기존엔 성공 시 null 반환)
        if (!m.matches()) {
            log.debug("JOWI no match: '{}'", s);
            return null;
        }

        LocalTime time = toTime(m.group(1));
        Integer level = Integer.parseInt(m.group(2));
        String arrow = m.group(3);
        Integer delta = Integer.parseInt(m.group(4));
        TideEvent.Trend trend = "▲".equals(arrow) ? TideEvent.Trend.RISING : TideEvent.Trend.FALLING;
        return new TideEvent(time, level, trend, delta);
    }

    /** 외부 1일치 → 내부 1일치 */
    public static TideDailyInfo toDaily(TideExternalDto ext) {
        var pd = parseDate(ext.pThisDate);
        var sun = parsePair(ext.pSun);
        var moon = parsePair(ext.pMoon);

        // pSelArea에 붙어오는 <br> 제거
        String area = ext.pSelArea == null ? null
                : ext.pSelArea.replaceAll("(?i)<br\\s*/?>", "").trim();

        List<TideEvent> events = new ArrayList<>(4);
        for (String s : new String[]{ext.t1, ext.t2, ext.t3, ext.t4}) {
            TideEvent ev = parseJowi(s);
            if (ev != null) events.add(ev);
        }


        return new TideDailyInfo (
                pd.date, pd.weekday, pd.lunar,
                area, ext.pMul,
                sun[0], sun[1], moon[0], moon[1],
                events
        );
    }

}
