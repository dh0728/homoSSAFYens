package com.homoSSAFYens.homSSAFYens.dto;

import java.util.List;

public record ForecastResponse(

        List<ForecastDay> days
) {
}
