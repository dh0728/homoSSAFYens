package com.homoSSAFYens.homSSAFYens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homoSSAFYens.homSSAFYens.dto.TyphoonPathRequest;
import com.homoSSAFYens.homSSAFYens.service.TyphoonAlertService;

@RestController
@RequestMapping("/api/typhoons")
public class TyphoonSimulationController {

    private final TyphoonAlertService service;

    public TyphoonSimulationController(TyphoonAlertService service) {
        this.service = service;
    }

    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestBody @Validated TyphoonPathRequest req) {
        var result = service.checkAndNotify(req);
        return ResponseEntity.ok(result);
    }
}