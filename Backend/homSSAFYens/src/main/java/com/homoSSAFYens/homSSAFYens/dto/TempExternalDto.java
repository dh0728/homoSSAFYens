package com.homoSSAFYens.homSSAFYens.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 바다타임 바다 수온 데이터 (원문 JSON 그대로 매핑
 * 외부 데이터 원문 그대로 받아오는 DTO
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class TempExternalDto {

        @JsonProperty("lat") public String lat;
        @JsonProperty("lon") public String lon;
        @JsonProperty("obs_name") public String obsName;
        @JsonProperty("obs_wt") public String obsWt;
        @JsonProperty("obs_time") public String obsTime;// 2025-07-21 13:00
        @JsonProperty("obs_dt") public String obsDt;    // 1km 미만 일경우 m, 이상일 경우 1km
                                                        // (*키로미터는 한 자리수 순으로 증가)

}
