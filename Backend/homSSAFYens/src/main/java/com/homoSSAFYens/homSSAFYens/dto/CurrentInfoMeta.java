package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentInfoMeta(
        @JsonProperty("city")     String city,
        @JsonProperty("cityCode") String cityCode
) {}
