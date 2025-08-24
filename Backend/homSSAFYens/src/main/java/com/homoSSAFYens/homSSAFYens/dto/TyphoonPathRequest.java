package com.homoSSAFYens.homSSAFYens.dto;

import java.util.List;

public record TyphoonPathRequest(
        String name,
        Double bufferRadiusKm, // e.g., 5km
        List<TyphoonPoint> path
) {}