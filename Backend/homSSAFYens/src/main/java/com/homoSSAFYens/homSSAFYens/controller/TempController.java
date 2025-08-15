package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.TempApiClient;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/temp")
public class TempController {

    private final TempApiClient tempApiClient;

    public TempController(TempApiClient tempApiClient) {
        this.tempApiClient = tempApiClient;
    }

    @GetMapping("/test")
    public ApiResponse<String> test(@RequestParam double lat, @RequestParam double lon) {
        return ApiResponse.success(null, tempApiClient.testTemp(lat, lon));
    }
}
