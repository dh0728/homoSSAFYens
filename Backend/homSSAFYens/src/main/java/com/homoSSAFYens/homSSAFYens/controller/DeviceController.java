package com.homoSSAFYens.homSSAFYens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.LocateReq;
import com.homoSSAFYens.homSSAFYens.dto.RegisterReq;
import com.homoSSAFYens.homSSAFYens.service.DeviceService;
import com.homoSSAFYens.homSSAFYens.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final ScheduleService scheduleService;

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterReq req) {
        deviceService.register(req.deviceId(), req.fcmToken());
        log.info("/register deviceId={} token={}", req.deviceId(), req.fcmToken());
        return ApiResponse.success(null, "성공");
    }

    @PostMapping("/locate")
    public ApiResponse<String> locate(@RequestBody LocateReq req) {
        String g = GeoKeyUtil.geoKey(req.lat(), req.lon(), 3);
        deviceService.updateLastLocation(req.deviceId(), g, req.lat(), req.lon(), req.ts());
        log.info("/locate deviceId={} lat={} lon={} ts={}", req.deviceId(), req.lat() , req.lon(), req.ts());

        // 현재 시간 초단위로 변환
        long now = java.time.Instant.now().getEpochSecond();
        scheduleService.scheduleFor(req.deviceId(), g, now, req.lat(), req.lon());

        return ApiResponse.success(null, "성공");
    }
}
