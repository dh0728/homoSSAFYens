package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.ForecastApiClient;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class ForecastService {

    private final ForecastApiClient forecastApiClient;
    private final CacheService cacheService;

    private static final int GEO_DECIMALS = 2;
    private static final Duration TTL = Duration.ofMinutes(10);


    public ForecastService(ForecastApiClient forecastApiClient,
                           CacheService cacheService) {
        this.forecastApiClient = forecastApiClient;
        this.cacheService = cacheService;
    }

    /**
     * 최종 응답 (days로 묶어서 반환)
     */
    public ForecastResponse getForecastInfo(double lat, double lon){

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "forecast:" + geo + ":" + GeoKeyUtil.dayKST();

        // 1) 캐시 히트 시 바로 반환
        ForecastResponse cached = cacheService.get(key, ForecastResponse.class);
        if (cached != null) return cached;

        // 2) 원본 호출 → 매핑
        List<ForecastExternalDto> ext = extOrEmpty(forecastApiClient.getForecast(lat, lon));
        ForecastResponse resp = ForecastMapper.toResponse(ext);

        // 3) 캐시 저장 (데이터 있으면 정상 캐시, 없으면 네거티브 캐시)
        if (ext.isEmpty()) {
            // 빈 결과도 캐시하고 싶다면 setNull 사용(짧은 TTL) — 트래픽 폭주 방지용
            //cacheService.setNull(key, Duration.ofSeconds(5));
        } else {
            cacheService.set(key, resp, TTL); // 10분
        }
        return resp;
    }

    private static List<ForecastExternalDto> extOrEmpty(List<ForecastExternalDto> ext) {
        return (ext == null) ? Collections.emptyList() : ext;
    }
}
