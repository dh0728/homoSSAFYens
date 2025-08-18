package com.homoSSAFYens.homSSAFYens.dto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PointFishingPointMapper {

    private PointFishingPointMapper() {}

    private static final Pattern NUM = Pattern.compile("([0-9]+(?:[\\.,][0-9]+)?)");

    public static PointFishingPoint toDomain(PointExternalDto e) {
        if (e == null) return null;

        return new PointFishingPoint(
                trim(e.name),
                trim(e.pointNm),
                parseDepthRangeM(e.dpwt),
                trim(e.material),
                trim(e.tideTime),
                parseTargetMap(e.target),
                toDoubleOrNaN(e.lat),
                toDoubleOrNaN(e.lon),
                trim(e.photo),
                trim(e.addr),
                toBooleanYN(e.seaside),
                parseDistanceKm(e.pointDt)
        );
    }

    public static List<PointFishingPoint> toDomainList(List<PointExternalDto> list) {
        if (list == null || list.isEmpty()) return null;

        return list.stream()
                .map(PointFishingPointMapper::toDomain)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static double toDoubleOrNaN(String s){
        if (s == null || s.isBlank()) return Double.NaN;
        s = s.replace(',', '.');
        try { return Double.parseDouble(s); } catch (Exception ex){ return Double.NaN; }
    }

    private static boolean toBooleanYN(String s){
        if (s == null || s.equals("n")) return false;
        return true;
    }

    /** "0.3~0.1m" / "0.3 m ~ 0.1 m" / "30cm~50cm" 등 → DepthRange(m) */
    private static DepthRange parseDepthRangeM(String s){
        if (s == null || s.isEmpty()) return null;
        s = s.replace(',', '.');

        // 숫자 추출 (최대 2개)
        Matcher m = NUM.matcher(s);
        Double a = null, b = null;
        if (m.find()) a = safeParseDouble(m.group(1));
        if (m.find()) b = safeParseDouble(m.group(1));

        if (a == null && b == null) return null;

        // 단위 판단: cm 가 하나라도 보이면 cm → m 변환
        boolean hasCm = s.contains("cm");
        if (a != null && hasCm) a = a / 100.0;
        if (b != null && hasCm) b = b / 100.0;

        // 하나만 있으면 단일값 범위
        if (a != null && b == null) b = a;
        if (b != null && a == null) a = b;

        // min/max 정렬
        if (a != null && b != null && a > b) {
            double t = a; a = b; b = t;
        }
        return new DepthRange(a, b);
    }

    /** "0" / "1.2" / "800 m" / "0.8km" → km */
    private static Double parseDistanceKm(String s){
        if (s == null || s.isEmpty()) return null;

        s = s.replace("㎞","km").replace(',', '.').replace(" ", "");
        Matcher m = NUM.matcher(s);
        if (!m.find()) return null;

        Double v = safeParseDouble(m.group(1));
        if (v == null) return null;

        if (s.endsWith("km") || s.contains("km")) return v;
        if (s.endsWith("m")  || s.matches(".*\\d+m.*")) return v / 1000.0; // m → km
        return v; // 단위 없으면 km 가정
    }

    private static Double safeParseDouble(String s){
        try { return Double.parseDouble(s); } catch (Exception e){ return null; }
    }

    /**
     * "붕장어-원투▶참돔-부력,잠수▶노래미-루어,릴찌,장대,원투" →
     * { "붕장어":[원투], "참돔":[부력,잠수], "노래미":[루어,릴찌,장대,원투] }
     */
    private static Map<String, List<String>> parseTargetMap(String s){
        if (s == null || s.isEmpty()) return Map.of();

        // 블록 분리: ▶ / ▷ / ➤ / > 등도 허용
        String[] pairs = s.split("[▶▷➤>]+");

        Map<String, List<String>> map = new LinkedHashMap<>(); // 순서 보존
        for (String pair : pairs){
            String p = pair.trim();
            if (p.isEmpty()) continue;

            // 어종-기법들 구분: 하이픈/대시/콜론 등 폭넓게 허용
            String[] split = p.split("\\s*[-–—−:：=]\\s*", 2);
            String fish = split[0].trim();
            if (fish.isEmpty()) continue;

            List<String> methods = List.of();
            if (split.length > 1) {
                // 기법 리스트: 쉼표/전각쉼표/슬래시/중점 등 허용
                String rhs = split[1].trim();
                String[] tokens = rhs.split("[,，/·・;；]+");
                methods = Arrays.stream(tokens)
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());
            }
            map.put(fish, methods);
        }
        return map.isEmpty() ? Map.of() : Map.copyOf(map);
    }










}
