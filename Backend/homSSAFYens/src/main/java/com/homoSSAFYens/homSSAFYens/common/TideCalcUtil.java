package com.homoSSAFYens.homSSAFYens.common;

import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.dto.TideEvent;
import com.homoSSAFYens.homSSAFYens.dto.TideHighInfo;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TideCalcUtil {
    private TideCalcUtil() {}

    /** daily.events에서 만조(로컬 최대치)만 골라 nowEpoch(초) 이후만 반환 */
    public static List<TideHighInfo> highsFromDaily(TideDailyInfo daily, ZoneId zone, long nowEpochSec) {
        if (daily == null || daily.events() == null || daily.events().isEmpty()) {
            return Collections.emptyList();
        }
        final List<TideEvent> ev = daily.events();
        final int n = ev.size();
        final String loc = daily.locationName();
        final LocalDate date = daily.date();

        List<TideHighInfo> out = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            TideEvent e = ev.get(i);
            int cur = e.levelCm();
            Integer prev = (i > 0)     ? ev.get(i - 1).levelCm() : null;
            Integer next = (i < n - 1) ? ev.get(i + 1).levelCm() : null;

            boolean isLocalMax =
                    (prev == null && next != null && cur >= next) ||
                            (prev != null && next == null && cur >= prev) ||
                            (prev != null && next != null && cur >= prev && cur >= next);

            // 보수 규칙: trend가 있다면 HIGH 후보는 RISING일 확률이 높음(데이터마다 다름) → 있으면 가산
            boolean isHigh = isLocalMax || e.trend() == TideEvent.Trend.RISING;
            if (isHigh) {
                LocalTime t = e.time(); // 이미 LocalTime
                long epoch = ZonedDateTime.of(date, t, zone).toEpochSecond();
                if (epoch >= nowEpochSec) {
                    out.add(new TideHighInfo(epoch, loc));
                }
            }
        }

        out.sort(Comparator.comparingLong(TideHighInfo::epochSecond));
        // 중복/동시각 제거(선택)
        out = dedupByEpoch(out);
        return out;
    }

    private static List<TideHighInfo> dedupByEpoch(List<TideHighInfo> in) {
        List<TideHighInfo> res = new ArrayList<>(in.size());
        Long last = null;
        for (TideHighInfo h : in) {
            if (last == null || h.epochSecond() != last) res.add(h);
            last = h.epochSecond();
        }
        return res;
    }

    /**
     * 오늘 daily에서 epoch T와 **같은 시각**의 이벤트를 찾아 |deltaCm| 반환.
     * - 보통 T는 만조(HIGH) 시각 → 이벤트 trend=FALLING일 가능성 높음 (deltaCm 음수) → 절댓값으로 비교
     * - 없으면 0
     */
    public static int deltaAt(TideDailyInfo daily, long targetEpochSec) {
        if (daily == null || daily.events() == null || daily.events().isEmpty()) return 0;

        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDate day = daily.date();
        LocalTime targetLt = Instant.ofEpochSecond(targetEpochSec).atZone(zone).toLocalTime();

        // 초 단위 일치 기준(“HH:mm:ss” 동일)
        for (TideEvent e : daily.events()) {
            if (e.time().equals(targetLt)) {
                Integer d = e.deltaCm();
                return (d == null) ? 0 : Math.abs(d);
            }
        }
        return 0;
    }

    /**
     * targetEpochSec(보통 만조 T)의 '직전 간조(LOW)' 시각을 epochSec으로 반환.
     * - 데이터 규칙: RISING = 간조(LOW), FALLING = 만조(HIGH)
     * - 탐색 범위: 같은 날(daily.date()) 안에서만 찾음
     *   (전날까지 보려면 어제 daily를 추가로 불러와 같은 로직을 반복)
     */
    public static OptionalLong findPrevLowEpoch(TideDailyInfo daily,
                                                long targetEpochSec,
                                                ZoneId zoneId) {
        if (daily == null || daily.events() == null || daily.events().isEmpty()) {
            return OptionalLong.empty();
        }

        var day = daily.date(); // LocalDate (필수로 존재)
        var lowEpochs = daily.events().stream()
                .filter(e -> e.trend() == TideEvent.Trend.FALLING) // 간조만
                .map(e -> ZonedDateTime.of(day, e.time(), zoneId).toEpochSecond())
                .sorted(Comparator.naturalOrder())
                .toList();

        // target 직전(< target) 중 가장 큰 값
        for (int i = lowEpochs.size() - 1; i >= 0; i--) {
            long ts = lowEpochs.get(i);
            if (ts < targetEpochSec) return OptionalLong.of(ts);
        }
        return OptionalLong.empty(); // 같은 날엔 직전 간조가 없음
    }
}
