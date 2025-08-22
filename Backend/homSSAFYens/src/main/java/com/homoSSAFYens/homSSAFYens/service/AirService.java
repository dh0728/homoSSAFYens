package com.homoSSAFYens.homSSAFYens.service;


import com.homoSSAFYens.homSSAFYens.client.AirApiClient;
import org.springframework.stereotype.Service;

@Service
public class AirService {

    private final AirApiClient airApiClient;

    public AirService(AirApiClient airApiClient) {
        this.airApiClient = airApiClient;
    }

}
