package com.homoSSAFYens.homSSAFYens.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homoSSAFYens.homSSAFYens.dto.TyphoonPathRequest;
import com.homoSSAFYens.homSSAFYens.service.TyphoonPathStore;

@RestController
@RequestMapping("/api/typhoons/admin")
public class TyphoonAdminController {
	
	// 주입
	private final TyphoonPathStore store;
	
	public TyphoonAdminController(TyphoonPathStore store) {
		this.store = store; 
	}
	
	@PostMapping("/path")
	public ResponseEntity<?> setPath(@RequestBody @Validated TyphoonPathRequest req){
		var sp = new TyphoonPathStore.StoredPath(req.name(), req.bufferRadiusKm(), req.path());
        store.set(sp);
        System.out.println(sp.path);
        return ResponseEntity.ok().body("{\"ok\":true}");
	}
}