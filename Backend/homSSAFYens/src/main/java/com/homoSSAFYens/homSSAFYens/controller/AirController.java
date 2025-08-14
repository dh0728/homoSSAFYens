package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.AirApiClient;
import com.homoSSAFYens.homSSAFYens.service.AirService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/air")
public class AirController {

    private final AirService airService;
    private final AirApiClient airApiClient; // 이건 api 테스트 땜시 넣은거

    public AirController(AirService airService, AirApiClient airApiClient) {
        this.airService = airService;
        this.airApiClient = airApiClient;
    }

    @GetMapping("/test")
    public String testAir() {
        return airApiClient.testAir();
    }




}
