package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.PointApiClient;
import com.homoSSAFYens.homSSAFYens.client.TempApiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/point")
public class PointController {

    private final PointApiClient pointApiClient;

    public PointController(PointApiClient pointApiClient) {
        this.pointApiClient = pointApiClient;
    }

    @GetMapping("/test")
    public String test(@RequestParam double lat, @RequestParam double lon) {
        return pointApiClient.testPoint(lat, lon);
    }
}
