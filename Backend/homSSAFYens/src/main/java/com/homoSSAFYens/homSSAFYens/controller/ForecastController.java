package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.ForecastApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.dto.ForecastDay;
import com.homoSSAFYens.homSSAFYens.dto.ForecastResponse;
import com.homoSSAFYens.homSSAFYens.service.ForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/forecast")
public class ForecastController {

    private final ForecastApiClient forecastApiClient;
    private final ForecastService forecastService;

    public ForecastController(ForecastApiClient forecastApiClient, ForecastService forecastService) {
        this.forecastApiClient = forecastApiClient;
        this.forecastService = forecastService;
    }


    @GetMapping("/7days")
    public ApiResponse<ForecastResponse> getForecast(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, forecastService.getForecastInfo(lat, lon));
    }

    @GetMapping("/test")
    public ApiResponse<String> test(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, forecastApiClient.testForecast(lat, lon));
    }

}
