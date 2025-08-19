package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.PointApiClient;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PointService {

    private final PointApiClient pointApiClient;
    private final CacheService cacheService;
    private static final int GEO_DECIMALS = 3;


    private static final Duration TTL = Duration.ofHours(6);

    public PointService(PointApiClient pointApiClient,
                        CacheService cacheService) {
        this.pointApiClient = pointApiClient;
        this.cacheService = cacheService;
    }


    public PointResponse getPointList(double lat, double lon) {

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String key = "point:" + geo + ":" + GeoKeyUtil.dayKST();


        // 1) 캐시 히트 시 바로 반환
        PointResponse cached = cacheService.get(key, PointResponse.class);
        if (cached != null) return cached;

        // 1-1) 직전 '없음' 네거티브 캐시가 있으면 바로 빈 응답
        if (cacheService.isNull(key)) {
            return new PointResponse(null, List.of());
        }

        // --- (옵션) 도그파일 방지 락 ---
        // if (!cacheService.tryLock(key, Duration.ofSeconds(5))) {
        //     PointResponse again = cacheService.get(key, PointResponse.class);
        //     if (again != null) return again;
        // }

        // 2) 원본 호출
        PointEnvelope env = pointApiClient.getPoint(lat, lon);
        if (env == null) {
            //cacheService.setNull(key, Duration.ofSeconds(30)); // 없으면 잠깐만 막아두기
            return new PointResponse(null, List.of());
        }

        // 3) 매핑
        PointInfo info = PointInfoMapper.toDomain(env.info());
        List<PointFishingPoint> points = PointFishingPointMapper.toDomainList(env.fishingPoint());
        PointResponse resp = new PointResponse(info, points == null ? List.of() : points);

        // 4) 캐시 저장 (데이터 유무에 따라 분기)
        if (resp.points().isEmpty()) {
            //cacheService.setNull(key, Duration.ofSeconds(30));     // 없을 땐 짧게
        } else {
            cacheService.set(key, resp, TTL);        // 정상 데이터는 6h
        }

        return resp;

    }

}
