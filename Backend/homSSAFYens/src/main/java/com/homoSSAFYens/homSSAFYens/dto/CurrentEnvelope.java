package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentEnvelope(
        @JsonProperty("weather") List<CurrentExternalDto> weather,
        @JsonProperty("info")    CurrentInfoMeta info
) {}


