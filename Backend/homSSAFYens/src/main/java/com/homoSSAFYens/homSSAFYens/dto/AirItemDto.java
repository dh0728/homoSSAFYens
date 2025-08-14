package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AirItemDto (
    @JsonProperty("dataTime")   String dataTime,
    @JsonProperty("mangName")   String mangName,
    @JsonProperty("so2Value")   String so2Value,
    @JsonProperty("coValue")    String coValue,
    @JsonProperty("o3Value")    String o3Value,
    @JsonProperty("no2Value")   String no2Value,
    @JsonProperty("pm10Value")  String pm10Value,
    @JsonProperty("pm10Value24") String pm10Value24,
    @JsonProperty("pm25Value")  String pm25Value,
    @JsonProperty("pm25Value24") String pm25Value24,

    @JsonProperty("khaiValue")  String khaiValue,
    @JsonProperty("khaiGrade")  String khaiGrade,
    @JsonProperty("so2Grade")   String so2Grade,
    @JsonProperty("coGrade")    String coGrade,
    @JsonProperty("o3Grade")    String o3Grade,
    @JsonProperty("no2Grade")   String no2Grade,
    @JsonProperty("pm10Grade")  String pm10Grade,
    @JsonProperty("pm25Grade")  String pm25Grade,   // 옵션

    @JsonProperty("pm10Grade1h") String pm10Grade1h, // 옵션
    @JsonProperty("pm25Grade1h") String pm25Grade1h, // 옵션

    @JsonProperty("so2Flag")    String so2Flag,     // 옵션(null 가능)
    @JsonProperty("coFlag")     String coFlag,      // 옵션
    @JsonProperty("o3Flag")     String o3Flag,      // 옵션
    @JsonProperty("no2Flag")    String no2Flag,     // 옵션
    @JsonProperty("pm10Flag")   String pm10Flag,    // 옵션
    @JsonProperty("pm25Flag")   String pm25Flag     // 옵션
) {}
