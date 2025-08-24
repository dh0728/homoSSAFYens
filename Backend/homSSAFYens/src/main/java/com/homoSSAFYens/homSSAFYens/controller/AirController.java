package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.AirApiClient;
import com.homoSSAFYens.homSSAFYens.client.SgisApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.dto.FineDustResponse;
import com.homoSSAFYens.homSSAFYens.service.AirService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/air")
public class AirController {

    private final AirService airService;
    private final AirApiClient airApiClient; // 이건 api 테스트 땜시 넣은거
    private final SgisApiClient sgisApiClient;

    public AirController(AirService airService,
                         AirApiClient airApiClient,
                         SgisApiClient sgisApiClient) {
        this.airService = airService;
        this.airApiClient = airApiClient;
        this.sgisApiClient = sgisApiClient;
    }

    @GetMapping("/fine_dust")
    public ApiResponse<FineDustResponse> fineDust(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, airService.getAirInfo(lat, lon));
    }

    @GetMapping("/test")
    public ApiResponse<String> testAir() {
        return ApiResponse.success(null, airApiClient.testAir());
    }

    @GetMapping("/sgistest")
    public ApiResponse<String> gistestAir() {

        String token = sgisApiClient.getSgisAccessKey();



        return ApiResponse.success(null, token);
    }


}
