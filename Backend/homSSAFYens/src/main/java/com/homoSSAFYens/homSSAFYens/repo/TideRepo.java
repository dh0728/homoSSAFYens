package com.homoSSAFYens.homSSAFYens.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.dto.TideEvent;
import com.homoSSAFYens.homSSAFYens.dto.TideHighInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TideRepo {

    private final StringRedisTemplate redis;
    private final ObjectMapper om; // 주의: JavaTimeModule 등록된 Bean 주입 권장
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 일간 캐시(JSON: TideDailyInfo)에서 앞으로 도래할 만조 시각(epoch, 초)를 계산해서 반환.
     * - 오늘 키에서 먼저 계산하고, 남은 게 없으면 내일 키도 확인(야간 보강)
     */
    public List<TideHighInfo> upcomingHighTidesFromDaily(String geoKey, long nowEpoch) {
        LocalDate today = LocalDate.now(KST);
        List<TideHighInfo> out = new ArrayList<>();

        // 1) 오늘 데이터에서 계산
        out.addAll(readAndExtract(todayKey(geoKey, today), nowEpoch));

        // 2) 오늘 남은 게 없으면, 내일 것도 확인(다음 날 첫 만조 대비)
        if (out.isEmpty()) {
            out.addAll(readAndExtract(todayKey(geoKey, today.plusDays(1)), nowEpoch));
        }

        return out.stream().distinct().sorted(Comparator.comparingLong(TideHighInfo::epochSecond)).toList();
    }

    private String todayKey(String geoKey, LocalDate dateKst) {
        String day = dateKst.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        return "tide1d:" + geoKey + ":" + day;
    }

    private List<TideHighInfo> readAndExtract(String key, long nowEpoch) {
        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) return List.of();

        try {
            // TideDailyInfo로 역직렬화 (ObjectMapper에 JavaTimeModule 필요)
            TideDailyInfo d = om.readValue(json, TideDailyInfo.class);
            if (d == null || d.events() == null || d.events().isEmpty()) return List.of();

            List<Long> highs = extractHighEpochs(d);
            return highs.stream()
                    .filter(ts -> ts > nowEpoch)
                    .map(ts -> new TideHighInfo(ts,d.locationName()))
                    .toList();

        } catch (Exception e) {
            // log.debug("parse fail for {}", key, e);
            return List.of();
        }
    }

    /** 로컬 최대(이웃보다 수위 높음) → 만조. 없으면 수위 상위 2개 백업 */
    private List<Long> extractHighEpochs(TideDailyInfo d) {
        List<TideEvent> ev = d.events();
        int n = ev.size();
        if (n == 0) return List.of();

        List<TideEvent> peaks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            TideEvent cur = ev.get(i);
            TideEvent prev = ev.get((i - 1 + n) % n);
            TideEvent next = ev.get((i + 1) % n);
            if (prev.levelCm() <= cur.levelCm() && next.levelCm() <= cur.levelCm()) {
                peaks.add(cur);
            }
        }
        if (peaks.isEmpty()) {
            peaks = ev.stream()
                    .sorted(java.util.Comparator.comparingInt(TideEvent::levelCm).reversed())
                    .limit(2)
                    .toList();
        }

        return peaks.stream()
                .map(p -> toEpochSecondsKST(d.date(), p.time()))
                .sorted()
                .distinct()
                .toList();
    }

    private long toEpochSecondsKST(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, KST).toEpochSecond();
    }
}
