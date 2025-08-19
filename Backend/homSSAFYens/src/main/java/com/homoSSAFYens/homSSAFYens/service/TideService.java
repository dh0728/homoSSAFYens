package com.homoSSAFYens.homSSAFYens.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homoSSAFYens.homSSAFYens.client.TideApiClient;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.dto.TideExternalDto;
import com.homoSSAFYens.homSSAFYens.dto.TideMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TideService {

    private final TideApiClient tideApiClient;
    private final CacheService cacheService;
    private final ObjectMapper om;

    // --- 캐시 정책 ---
    private static final int GEO_DECIMALS = 3;
    private static final Duration TTL = Duration.ofHours(6);// ≈110m

    public TideService(TideApiClient tideApiClient,
                       CacheService cacheService,
                       StringRedisTemplate redis,
                       ObjectMapper objectMapper) {
        this.tideApiClient = tideApiClient;
        this.om = objectMapper;
        this.cacheService = cacheService;
    }

    /** 7일치 모두 전달 하기 */
    public List<TideDailyInfo> getWeekly(double lat, double lon) {

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "tide7d:" + geo + ":" + GeoKeyUtil.dayKST();

        // 1) 캐시 조회
        List<TideDailyInfo> cached = cacheService.get(key, new TypeReference<List<TideDailyInfo>>() {});
        if (cached != null) return cached;

        // 2) 원본 호출
        List<TideExternalDto> ext = tideApiClient.getTide(lat, lon);
        if (ext == null) return null;

        List<TideDailyInfo> mapped = ext.stream()
                .map(TideMapper::toDaily)
                .collect(java.util.stream.Collectors.toList());

        // 3) 캐시 저장
        cacheService.set(key, mapped, TTL);

        return mapped;
    }

    /** 오늘자만  */
    public TideDailyInfo getDaily(double lat, double lon) {
        LocalDate today = LocalDate.now();
        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "tide1d:" + geo + ":" + GeoKeyUtil.dayKST(today);

        // 1) 일간 캐시
        TideDailyInfo one = cacheService.get(key, TideDailyInfo.class);
        if (one != null) return one;

        // 2) 주간에서 필터
        List<TideDailyInfo> weekly = getWeekly(lat, lon);
        if (weekly == null) return null;

        TideDailyInfo todayInfo = weekly.stream()
                .filter(d -> today.equals(d.date()))
                .findFirst()
                .orElse(null);

        // 3) 일간 캐시에 저장
        if (todayInfo != null) {
            cacheService.set(key, todayInfo, TTL);
        }
        return todayInfo;
    }
}
