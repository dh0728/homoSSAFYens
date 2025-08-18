package com.homoSSAFYens.homSSAFYens.dto;

import com.homoSSAFYens.homSSAFYens.common.Season;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PointInfoMapper {

    private PointInfoMapper(){}


    public static PointInfo toDomain(PointInfoMeta meta){
        if (meta == null) return null;

        // 계절별 수온
        Map<Season, WaterTemp> water = new EnumMap<>(Season.class);
        putIfNotNull(water, Season.SPRING, parseWaterTemp(meta.wtempSp));
        putIfNotNull(water, Season.SUMMER, parseWaterTemp(meta.wtempSu));
        putIfNotNull(water, Season.FALL, parseWaterTemp(meta.wtempFa));
        putIfNotNull(water, Season.WINTER, parseWaterTemp(meta.wtempWi));

        // 계절별 어종
        Map<Season, List<String>> fish = new EnumMap<>(Season.class);
        putIfNotEmpty(fish, Season.SPRING, parseFishList(meta.fishSp));
        putIfNotEmpty(fish, Season.SUMMER, parseFishList(meta.fishSu));
        putIfNotEmpty(fish, Season.FALL, parseFishList(meta.fishFa));
        putIfNotEmpty(fish, Season.WINTER, parseFishList(meta.fishWi));

        return new PointInfo(
                trim(meta.intro),
                trim(meta.forcast),
                trim(meta.ebbf),
                trim(meta.notice),
                Collections.unmodifiableMap(water),
                Collections.unmodifiableMap(fish)
        );
    }

    // "21.1", "15,3" 등 소수점 표기를 모두 허용 (콤마/점)
    private static final Pattern NUMBER = Pattern.compile("([0-9]+(?:[\\.,][0-9]+)?)");

    // "표층 : 21.1℃ 저층 : 15.3℃" → WaterTemp(21.1, 15.3)
    // 순서가 바뀌거나 텍스트가 달라도 숫자 2개만 뽑아 표층/저층으로 적용
    private static WaterTemp parseWaterTemp(String s){
        if(s == null || s.isEmpty() )return null;

        s = s.replace(',' , '.');

        Matcher m = NUMBER.matcher(s);

        Double surface = null;
        Double bottom = null;

        if (m.find()) surface = Double.parseDouble(m.group(1));
        if (m.find()) bottom  = Double.parseDouble(m.group(1));

        if (surface == null && bottom == null) return null;
        return new WaterTemp(surface, bottom);
    }

    // "붕장어, 참돔, 도다리" → ["붕장어","참돔","도다리"]
    private static List<String> parseFishList(String s){

        if(s == null || s.isEmpty()) return null;

        // 쉼표/전각쉼표/세미콜론/슬래시 등 다양한 구분자 허용
        String[] fishes = s.split("[,，;；/]");
        return Arrays.stream(fishes)
                .map(String::trim)
                .filter((t -> !t.isEmpty()))
                .collect(Collectors.toUnmodifiableList());
    }

    static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static void putIfNotNull(Map<Season, WaterTemp> map, Season season, WaterTemp wt) {
        if (wt != null) map.put(season, wt);
    }

    private static void putIfNotEmpty(Map<Season, List<String>> map, Season season, List<String> list) {
        if (list != null && !list.isEmpty()) map.put(season, list);
    }


}
