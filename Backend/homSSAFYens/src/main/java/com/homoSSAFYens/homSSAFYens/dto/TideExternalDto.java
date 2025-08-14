package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 바다타임 1일치 물 때 데이터 (원문 JSON 그대로 매핑)
 * 외부 데이터를 원문 그대로 받아오는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TideExternalDto {

    @JsonProperty("pThisDate") public String pThisDate; // 예: "2025-7-21-월-6-27"
    @JsonProperty("pSelArea") public String pSelArea;         // 예: "남해 해운대<br>"
    @JsonProperty("pMul") public String pMul;           // 예: "4물"
    @JsonProperty("pSun") public String pSun;           // "05:51/19:00"
    @JsonProperty("pMoon") public String pMoon;         // "07:32/19:59"

    // 간/만조 + 수위/증감
    @JsonProperty("pTime1") public String t1;         // 예: "03:10 (127) ▼ -13"
    @JsonProperty("pTime2") public String t2;         // 예: "09:36 (127) ▲ +46"
    @JsonProperty("pTime3") public String t3;         // 예: "09:36 (127) ▲ +46"
    @JsonProperty("pTime4") public String t4;         // 예: "09:36 (127) ▲ +46"




}
