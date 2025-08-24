package com.homoSSAFYens.homSSAFYens.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homoSSAFYens.homSSAFYens.client.CurrentApiClient;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class CurrentService {

    private final CurrentApiClient currentApiClient;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private static final int GEO_DECIMALS = 2;

    private static final Duration TTL = Duration.ofMinutes(5);

    public CurrentService(CurrentApiClient currentApiClient,
                          CacheService cacheService,
                          ObjectMapper objectMapper) {

        this.currentApiClient = currentApiClient;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    /** api 호출 시간 기준 가장 가까운 시간 날씨만 제공 */
    public CurrentResponse getCurrentInfo(double lat, double lon) {
        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "current1h:" + geo + ":" + GeoKeyUtil.dayKST();


        // 1) 캐시 히트 시 바로 반환
        CurrentResponse cached = cacheService.get(key, CurrentResponse.class);
        if (cached != null) return cached;

        // 2) 원본 호출 → 맵핑
        CurrentEnvelope env = currentApiClient.getCurrent(lat, lon);
        CurrentResponse full = CurrentMapper.toResponse(env);

        // 3) 1개만 추려서 응답 구성
        java.util.List<?> list = full.weather();
        CurrentResponse onlyOne;
        if (list == null || list.isEmpty()) {
            // 빈 결과면 그냥 빈 리스트로 응답 (negative cache는 선택 사항)
            onlyOne = new CurrentResponse(java.util.List.of(), full.info());
            // 필요하면: cache.setNull(key, Duration.ofMinutes(2));
        } else {
            onlyOne = new CurrentResponse(java.util.List.of(full.weather().get(0)), full.info());
            cacheService.set(key, onlyOne, TTL); // 4) 캐시 저장
        }

        return onlyOne;
    }

    /** 현재 날씨 6시간 이후 까지 모두 제공*/
    public CurrentResponse getCurrentListInfo(double lat, double lon) {

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "current6h:" + geo + ":" + GeoKeyUtil.dayKST();

        // 1) 캐시
        CurrentResponse cached = cacheService.get(key, CurrentResponse.class);
        if (cached != null) return cached;

        // 2) 원본
        CurrentEnvelope env = currentApiClient.getCurrent(lat, lon);
        CurrentResponse full = CurrentMapper.toResponse(env); // weather 리스트 + info

        // 3) 캐시 저장 후 반환
        // 빈 결과도 캐시하고 싶다면 setNull 사용(짧은 TTL) — 트래픽 폭주 방지용
        if (full != null && full.weather() != null && !full.weather().isEmpty()) {
            cacheService.set(key, full, TTL);
        } else {
            // 선택: cache.setNull(key, Duration.ofMinutes(2));
        }
        return full;


    }

}
