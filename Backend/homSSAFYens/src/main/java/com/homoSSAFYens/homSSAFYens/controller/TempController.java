package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.TempApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.dto.TempInfo;
import com.homoSSAFYens.homSSAFYens.service.TempService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/temp")
public class TempController {

    private final TempApiClient tempApiClient;
    private final TempService tempService;

    public TempController(TempApiClient tempApiClient, TempService tempService) {
        this.tempApiClient = tempApiClient;
        this.tempService = tempService;
    }

    @GetMapping("/nearest")
    public ApiResponse<TempInfo> nearestTemp(@RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        return ApiResponse.success(null,  tempService.getTempInfo(lat, lon));
    }

    @GetMapping("/all")
    public ApiResponse<List<TempInfo>> tempAll(@RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        return ApiResponse.success(null,  tempService.getTempList(lat, lon));
    }


    @GetMapping("/test")
    public ApiResponse<String> test(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, tempApiClient.testTemp(lat, lon));
    }
}
