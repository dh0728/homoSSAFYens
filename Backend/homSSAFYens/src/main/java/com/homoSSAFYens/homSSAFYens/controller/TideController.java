package com.homoSSAFYens.homSSAFYens.controller;

import com.homoSSAFYens.homSSAFYens.client.TideApiClient;
import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.service.TideService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/tide")
@Validated
public class TideController {

    private final TideService tideService;
    private final TideApiClient tideApiClient; // 디버그 raw용으로만 사용할 때

    public TideController(TideService tideService, TideApiClient tideApiClient) {
        this.tideService = tideService;
        this.tideApiClient = tideApiClient;
    }


    @GetMapping("/test")
    public String test(@RequestParam double lat, @RequestParam double lon) {
        return tideApiClient.testTide(lat, lon);
    }

    /** 7일치 전체 조회 */
    @GetMapping("/weekly")
    public List<TideDailyInfo> weekly(@RequestParam Double lat, @RequestParam Double lon) {
        return tideService.getWeekly(lat, lon);
    }

    /** 하루치 조회 */
    @GetMapping("/today")
    public TideDailyInfo today(@RequestParam Double lat, @RequestParam Double lon) {
        return tideService.getDaily(lat, lon);
    }
}
