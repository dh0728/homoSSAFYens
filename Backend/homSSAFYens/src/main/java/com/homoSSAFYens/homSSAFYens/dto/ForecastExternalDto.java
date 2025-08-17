package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 바다타임 7일 날씨 조회 api 날씨 제공 값
 * 외부 데이터를 원문 그대로 받아오는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastExternalDto {

    @JsonProperty("ymdt") public String ymdt;        // 예 "2025081714",
    @JsonProperty("sky") public String sky;          // 예: 맑음
    @JsonProperty("skycode") public String skyCode;// 예  "1",
    @JsonProperty("rain") public String rain;        // 예 "0,0"

    @JsonProperty("rainAmt") public String rainAmt;  // 예 "0,0"

    @JsonProperty("temp") public String temp;        // 예 "32.2"
    @JsonProperty("winddir") public String winddir;  // 예 "SW",
    @JsonProperty("windspd") public String windspd;  // 예 "2.5",
    @JsonProperty("humidity") public String humidity;// 예 "67.0"
    @JsonProperty("wavePrd") public String wavePrd;  // 예 "6.10",\
    @JsonProperty("waveHt") public String waveHt;    // 예 "0.18",
    @JsonProperty("waveDir") public String waveDir;  // 예 "S",

    /** 알 수 없는 키들은 여기로 들어옴 → 제로폭/봄 제거 후 정상 키에 매핑 */
    @JsonAnySetter
    public void any(String rawKey, Object value) {
        String key = stripZeroWidth(rawKey);
        String v = (value == null) ? null : String.valueOf(value);

        switch (key) {
            case "winddir"  -> this.winddir = v;
            case "rainAmt"  -> this.rainAmt  = v;
            case "temp"     -> this.temp     = v;
            case "windspd"  -> this.windspd  = v;
            case "humidity" -> this.humidity = v;
            case "wavePrd"  -> this.wavePrd  = v;
            case "waveHt"   -> this.waveHt   = v;
            // 필요하면 다른 키도 추가
        }
    }

    private static String stripZeroWidth(String s) {
        if (s == null) return null;
        // BOM(U+FEFF), zero-width space/joiner/non-joiner 제거
        return s.replaceAll("[\\uFEFF\\u200B\\u200C\\u200D]", "");
    }
}
