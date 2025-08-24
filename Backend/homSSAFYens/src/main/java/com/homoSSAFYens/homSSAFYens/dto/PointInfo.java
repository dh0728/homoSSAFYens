package com.homoSSAFYens.homSSAFYens.dto;

import com.homoSSAFYens.homSSAFYens.common.Season;

import java.util.List;
import java.util.Map;

public record PointInfo (
        String intro,
        String forecast,
        String ebbf,
        String notice,
        Map<Season, WaterTemp> waterTemps,     // wtemp_sp/su/fa/wi
        Map<Season, List<String>> fishBySeason // fish_sp/su/fa/wi
)
{}
