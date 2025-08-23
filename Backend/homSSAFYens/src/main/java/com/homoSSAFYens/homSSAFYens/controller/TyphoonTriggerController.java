package com.homoSSAFYens.homSSAFYens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homoSSAFYens.homSSAFYens.service.TyphoonAlertService;

@RestController
@RequestMapping("/api/typhoons")
public class TyphoonTriggerController {
    private final TyphoonAlertService service;

    public TyphoonTriggerController(TyphoonAlertService service) {
        this.service = service;
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> trigger() {
        var result = service.checkAndNotifyUsingStoredPath();
        if (!Boolean.TRUE.equals(result.ok())) {
            return ResponseEntity.badRequest().body("{\"ok\":false, \"reason\":\"no-stored-path\"}");
        }
        return ResponseEntity.ok(result);
    }
}
