package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PointInfoMeta{

    @JsonProperty("intro") public String intro;
    @JsonProperty("forcast") public String forcast;
    @JsonProperty("ebbf") public String ebbf;
    @JsonProperty("notice") public String notice;
    @JsonProperty("wtemp_sp") public String wtempSp;
    @JsonProperty("wtemp_su") public String wtempSu;
    @JsonProperty("wtemp_fa") public String wtempFa;
    @JsonProperty("wtemp_wi") public String wtempWi;
    @JsonProperty("fish_sp") public String fishSp;
    @JsonProperty("fish_su") public String fishSu;
    @JsonProperty("fish_fa") public String fishFa;
    @JsonProperty("fish_wi") public String fishWi;

    /** 알 수 없는 키들은 여기로 들어옴 → 제로폭/봄 제거 후 정상 키에 매핑 */
    @JsonAnySetter
    public void any(String rawKey, Object value) {
        String key = stripZeroWidth(rawKey);
        String v = (value == null) ? null : String.valueOf(value);

        switch (key) {
            case "intro"  -> this.intro = v;
            case "forecast"  -> this.forcast  = v;
            case "ebbf"     -> this.ebbf     = v;
            case "notice"  -> this.notice  = v;
            case "wtemp_sp" -> this.wtempSp = v;
            case "wtemp_su"  -> this.wtempSu  = v;
            case "wtemp_fa"   -> this.wtempFa   = v;
            case "wtemp_wi"   -> this.wtempWi   = v;
            case "fish_sp"   -> this.fishSp  = v;
            case "fish_su"   -> this.fishSu   = v;
            case "fish_fa"   -> this.fishFa   = v;
            case "fish_wi"   -> this.fishWi   = v;
            // 필요하면 다른 키도 추가
        }
    }

    private static String stripZeroWidth(String s) {
        if (s == null) return null;
        // BOM(U+FEFF), zero-width space/joiner/non-joiner 제거
        return s.replaceAll("[\\uFEFF\\u200B\\u200C\\u200D]", "");
    }
}
