package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.ForecastApiClient;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ForecastService {

    private final ForecastApiClient forecastApiClient;

    public ForecastService(ForecastApiClient forecastApiClient) {
        this.forecastApiClient = forecastApiClient;
    }

    /**
     * 최종 응답 (days로 묶어서 반환)
     */
    public ForecastResponse getForecastInfo(double lat, double lon){
        List<ForecastExternalDto> ext = extOrEmpty(forecastApiClient.getForecast(lat, lon));
        return ForecastMapper.toResponse(ext);
    }

    private static List<ForecastExternalDto> extOrEmpty(List<ForecastExternalDto> ext) {
        return (ext == null) ? Collections.emptyList() : ext;
    }
}
