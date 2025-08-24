package com.homoSSAFYens.homSSAFYens.service;


import com.homoSSAFYens.homSSAFYens.client.AirApiClient;
import com.homoSSAFYens.homSSAFYens.client.SgisApiClient;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AirService {

    private final AirApiClient airApiClient;
    private final SgisApiClient sgisApiClient;
    private final CacheService cacheService;

    private static final int GEO_DECIMALS = 3; // ≈110m
    private static final Duration TTL = Duration.ofMinutes(30);


    public AirService(AirApiClient airApiClient ,
                      SgisApiClient sgisApiClient,
                      CacheService cacheService
                      ) {
        this.airApiClient = airApiClient;
        this.sgisApiClient = sgisApiClient;
        this.cacheService = cacheService;

    }

    public FineDustResponse getAirInfo(double lat, double lon) {

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "air:" + geo + ":" + GeoKeyUtil.dayKST();

        FineDustResponse cached = cacheService.get(key, FineDustResponse.class);
        if (cached != null) return cached;

        // 1) 좌표 변환 (WGS84 → EPSG:5186 TM)
        SgisTranscoordResponse transcoordRes = sgisApiClient.getSgisRgeocode(lat, lon);
        if (transcoordRes == null || transcoordRes.getResult() == null) {
            throw new IllegalStateException("SGIS 좌표변환 결과가 없습니다.");
        }
        final double tmX = transcoordRes.getResult().getPosX();
        final double tmY = transcoordRes.getResult().getPosY();

        // 2) TM 좌표로 가장 가까운 측정소 조회
        StationResponse stationRes = airApiClient.getNearestStation(tmX, tmY);
        if (stationRes == null || stationRes.getResponse() == null
                || stationRes.getResponse().getHeader() == null) {
            throw new IllegalStateException("근처 측정소 조회 응답이 올바르지 않습니다.");
        }
        // AirKorea 헤더 코드 체크
        if (!"00".equals(stationRes.getResponse().getHeader().getResultCode())) {
            throw new IllegalStateException("근처 측정소 조회 실패: "
                    + stationRes.getResponse().getHeader().getResultMsg());
        }

        // 2-1) 근처 측정소가 있는지 확인 (count/items 기준)
        StationResponse.Body body = stationRes.getResponse().getBody();
        List<StationResponse.Item> items = (body != null) ? body.getItems() : null;
        if (body == null || body.getTotalCount() <= 0 || items == null || items.isEmpty()) {
            throw new IllegalStateException("근처 측정소가 없습니다.");
        }

        // 2-2) tm(거리 km) 최소인 측정소 선택
        StationResponse.Item nearest = items.stream()
                .min(Comparator.comparingDouble(StationResponse.Item::getTm))
                .orElseThrow(() -> new IllegalStateException("근처 측정소가 없습니다.(빈 목록)"));

        final String stationName = nearest.getStationName();

        // 3) 선택한 측정소명으로 대기질 정보 조회
        //    dataTerm: Daily(하루), numOfRows: 100, pageNo: 1
        AirKoreaResponse res = airApiClient.getAirInfo(stationName, "Daily", 100, 1);
        AirHeader header = res.response().header();
        if (!"00".equals(header.resultCode())) {
            throw new IllegalStateException("AirKorea error: " + header.resultMsg());
        }

        List<AirItemDto> airItems = Optional.ofNullable(res.response().body())
                .map(AirBody::items).orElse(List.of());

        // 최신 1건
        AirItemDto latest = airItems.stream()
                .filter(i -> i.dataTime() != null)
                .max(Comparator.comparing(AirItemDto::dataTime)) // "yyyy-MM-dd HH:mm" 문자열 비교로도 정렬 가능
                .orElse(null);

        FineDustResponse resp = toFineDustResponse(latest, stationName);

        // ── 4) air 캐시 저장 (geoKey 기준) ─────────────────────────────
        cacheService.set(key, resp, TTL);

        return resp;
    }

    private FineDustResponse toFineDustResponse(AirItemDto item, String stationName) {

        LocalDateTime time = parseObsTime(item.dataTime());

        Map<String, FineDustValue> m = new LinkedHashMap<>();

        // 미세먼지(PM10)
        put(m, "pm10", item.pm10Value(), item.pm10Grade(), "㎍/m³");
        // 초미세먼지(PM2.5)
        put(m, "pm25", item.pm25Value(), item.pm25Grade(), "㎍/m³" );
        // 가스류
        put(m, "o3",  item.o3Value(),  item.o3Grade(),  "ppm");
        put(m, "co",  item.coValue(),  item.coGrade(),  "ppm");
        put(m, "so2", item.so2Value(), item.so2Grade(), "ppm");
        put(m, "no2", item.no2Value(), item.no2Grade(), "ppm");
        // 통합대기지수(KHAI)
        put(m, "khai", item.khaiValue(), item.khaiGrade(), "지수");

        return new FineDustResponse(time, stationName, m);

    }

    /**
     *
     * @param m
     * @param key
     * @param valueStr
     * @param gradeStr
     * @param unit
     */
    private void put(Map<String, FineDustValue> m, String key, String valueStr,
                     String gradeStr, String unit){
        Double v = parseDoubleOrNull(valueStr);
        String level = levelFrom(gradeStr, v, key);
        m.put(key, new FineDustValue(v, level, unit));
    }

    private String levelFrom(String gradeStr, Double value, String key) {
        Integer g = parseIntOrNull(gradeStr);

        // grade가 있으면 그걸로 등급 매김
        if (g != null) return mapGradeTo3(g);

        // 없으면
        if (value == null) return "모름";

        // 값 기준(공식 4단계를 3단계로 압축)
        return switch (key) {
            case "pm10" -> lvl(value, 30.0, 80.0);
            case "pm25" -> lvl(value, 15.0, 35.0);
            case "o3"   -> lvl(value, 0.030, 0.090);
            case "co"   -> lvl(value, 2.0,   9.0);
            case "so2"  -> lvl(value, 0.020, 0.050);
            case "no2"  -> lvl(value, 0.030, 0.060);
            case "khai" -> lvl(value, 50.0, 100.0);
            default     -> "모름";
        };
    }

    // 임계값 a(좋음 상한), b(보통 상한)
    private String lvl(double v, double a, double b) {
        if (v <= a) {
            return "좋음";
        } else if (v <= b) {
            return "보통";
        } else {
            return "나쁨";
        }

    }

    private String mapGradeTo3(int g) {
        return (g == 1) ? "좋음" : (g == 2) ? "보통" : (g == 3 || g == 4) ? "나쁨" : "모름";
    }

    // obs_time 파싱: 여러 포맷을 순서대로 시도
    private LocalDateTime parseObsTime(String s) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        s.trim();
        if (s == null || s.isEmpty()) return null;

        try {
            return LocalDateTime.parse(s, dateTimeFormatter);
        } catch (Exception ignore) { /* 다음 포맷 시도 */ }
        return null; // 실패 시 null (필드가 reference type 이라 NPE 없음)
    }

    // ====== 파싱 유틸 ======
    private static Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank() || "-".equals(s)) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }
    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank() || "-".equals(s)) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

}
