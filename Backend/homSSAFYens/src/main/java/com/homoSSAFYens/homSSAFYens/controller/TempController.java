package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.CurrentApiClient;
import com.homoSSAFYens.homSSAFYens.client.TempApiClient;
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
    public String test(@RequestParam double lat, @RequestParam double lon) {
        return tempApiClient.testTemp(lat, lon);
    }
}
