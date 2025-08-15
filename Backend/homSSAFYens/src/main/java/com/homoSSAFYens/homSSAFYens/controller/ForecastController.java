package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.ForecastApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/forecast")
public class ForecastController {

    private final ForecastApiClient forecastApiClient;

    public ForecastController(ForecastApiClient forecastApiClient) {
        this.forecastApiClient = forecastApiClient;
    }

    @GetMapping("/test")
    public ApiResponse<String> test(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, forecastApiClient.testForecast(lat, lon));
    }

}
