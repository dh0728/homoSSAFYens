package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.CurrentApiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/current")
public class CurrentController {

    private final CurrentApiClient currentApiClient;

    public CurrentController(CurrentApiClient currentApiClient) {
        this.currentApiClient = currentApiClient;
    }

    @GetMapping("/test")
    public String test(@RequestParam double lat, @RequestParam double lon) {
        return currentApiClient.testCurrent(lat, lon);
    }
}
