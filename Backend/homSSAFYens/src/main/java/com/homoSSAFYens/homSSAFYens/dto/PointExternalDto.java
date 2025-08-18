package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 바다타임 날씨 포인트 서비스 api
 * 외부 데이터를 원문 그대로 받아오는 DTO
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class PointExternalDto {

    @JsonProperty("name") public String name;
    @JsonProperty("point_nm") public String pointNm;
    @JsonProperty("dpwt") public String dpwt;
    @JsonProperty("material") public String material;
    @JsonProperty("tide_time") public String tideTime;
    @JsonProperty("target") public String target;
    @JsonProperty("lat") public String lat;
    @JsonProperty("lon") public String lon;
    @JsonProperty("photo") public String photo;
    @JsonProperty("addr") public String addr;
    @JsonProperty("seaside") public String seaside;
    @JsonProperty("point_dt") public String pointDt;

    /** 알 수 없는 키들은 여기로 들어옴 → 제로폭/봄 제거 후 정상 키에 매핑 */
    @JsonAnySetter
    public void any(String rawKey, Object value) {
        String key = stripZeroWidth(rawKey);
        String v = (value == null) ? null : String.valueOf(value);

        switch (key) {
            case "addr"  -> this.addr = v;
            case "seaside"  -> this.seaside  = v;
            case "point_dt"     -> this.pointDt     = v;
            // 필요하면 다른 키도 추가
        }
    }

    private static String stripZeroWidth(String s) {
        if (s == null) return null;
        // BOM(U+FEFF), zero-width space/joiner/non-joiner 제거
        return s.replaceAll("[\\uFEFF\\u200B\\u200C\\u200D]", "");
    }
}
