package com.homoSSAFYens.homSSAFYens.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PointEnvelope(

        @JsonProperty("fishing_point") List<PointExternalDto> fishingPoint,
        @JsonProperty("info") PointInfoMeta info
) {}
