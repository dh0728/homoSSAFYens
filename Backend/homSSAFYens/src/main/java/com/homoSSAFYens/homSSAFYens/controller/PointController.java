package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.PointApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.dto.PointResponse;
import com.homoSSAFYens.homSSAFYens.service.PointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/point")
public class PointController {

    private final PointApiClient pointApiClient;
    private final PointService pointService;

    public PointController(PointApiClient pointApiClient, PointService pointService) {
        this.pointApiClient = pointApiClient;
        this.pointService = pointService;
    }

    @GetMapping("/list")
    public ApiResponse<PointResponse> getPoint(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, pointService.getPointList(lat, lon));
    }

    @GetMapping("/test")
    public ApiResponse<String> test(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, pointApiClient.testPoint(lat, lon));
    }
}
