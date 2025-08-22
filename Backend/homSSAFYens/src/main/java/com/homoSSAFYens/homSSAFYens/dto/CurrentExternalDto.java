package com.homoSSAFYens.homSSAFYens.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * 바다타임 현재 날씨 서비스 api 요청 시간 기준 현재부터 6시간 이후 데이터값 제공
 * 외부 데이터를 원문 그대로 받아오는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentExternalDto {

    @JsonProperty("sky") public String sky;          // 예: 맑음
    @JsonProperty("rain") public String rain;        // 예 "0,0"
    @JsonProperty("temp") public String temp;        // 예 "32.2"
    @JsonProperty("pm25_s") public String pm25_s;    // 예 "보통",
    @JsonProperty("pm10_s") public String pm10_2;    // 에 "좋음",
    @JsonProperty("pm10") public String pm10;        // 예 "18",
    @JsonProperty("winddir") public String winddir;  // 예 "S",
    @JsonProperty("pago") public String page;        // 예 "0.28",
    @JsonProperty("sky_code") public String sky_code;// 예  "1",
    @JsonProperty("windspd") public String windspd;  // 예 "0.8",
    @JsonProperty("pm25") public String pm25;        // 예 "16",
    @JsonProperty("aplYmdt") public String aplYmdt;  // 예 "2025081714",
    @JsonProperty("humidity") public String humidity;// 예 "67.0"
}

