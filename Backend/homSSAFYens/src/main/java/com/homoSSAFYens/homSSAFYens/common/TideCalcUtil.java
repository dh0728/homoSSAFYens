package com.homoSSAFYens.homSSAFYens.common;

import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.dto.TideEvent;
import com.homoSSAFYens.homSSAFYens.dto.TideHighInfo;

import java.time.*;
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
}
