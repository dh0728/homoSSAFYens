package com.homoSSAFYens.homSSAFYens.dto;

import java.util.List;

public record PointResponse (
        PointInfo info,
        List<PointFishingPoint> points
) {}
