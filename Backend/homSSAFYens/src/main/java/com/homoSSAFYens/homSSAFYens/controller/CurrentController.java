package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.CurrentApiClient;
import com.homoSSAFYens.homSSAFYens.client.TideApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.dto.CurrentInfo;
import com.homoSSAFYens.homSSAFYens.dto.CurrentResponse;
import com.homoSSAFYens.homSSAFYens.service.CurrentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/current")
public class CurrentController {

    private final CurrentService currentService;
    private final CurrentApiClient currentApiClient;


    public CurrentController(CurrentApiClient currentApiClient, CurrentService currentService) {
        this.currentService = currentService;
        this.currentApiClient = currentApiClient;
    }

    @GetMapping("/test")
    public ApiResponse<String> test(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, currentApiClient.testCurrent(lat, lon));
    }

    @GetMapping("/weather")
    public ApiResponse<CurrentResponse> weather(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, currentService.getCurrentInfo(lat, lon));
    }

    @GetMapping("/weather/6hour")
    public ApiResponse<CurrentResponse> weatherFor6hour(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, currentService.getCurrentListInfo(lat, lon));
    }
}
