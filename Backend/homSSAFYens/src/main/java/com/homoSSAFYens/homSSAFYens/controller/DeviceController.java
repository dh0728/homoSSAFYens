package com.homoSSAFYens.homSSAFYens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homoSSAFYens.homSSAFYens.dto.DeviceRegisterRequest;
import com.homoSSAFYens.homSSAFYens.service.DeviceRegistry;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceRegistry registry;

    public DeviceController(DeviceRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Validated DeviceRegisterRequest req) {
        registry.upsert(req.deviceId(), req.fcmToken(), req.location());
        return ResponseEntity.ok().body("{\"ok\":true}");
    }
}