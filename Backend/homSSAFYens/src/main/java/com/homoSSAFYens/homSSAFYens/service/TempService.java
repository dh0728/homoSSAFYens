package com.homoSSAFYens.homSSAFYens.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.homoSSAFYens.homSSAFYens.client.TempApiClient;
import com.homoSSAFYens.homSSAFYens.common.GeoKeyUtil;
import com.homoSSAFYens.homSSAFYens.dto.TempExternalDto;
import com.homoSSAFYens.homSSAFYens.dto.TempInfo;
import com.homoSSAFYens.homSSAFYens.dto.TempMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TempService {

    private final TempApiClient tempApiClient;
    private final CacheService cacheService;
    private static final int GEO_DECIMALS = 3;

    private static final Duration TTL = Duration.ofMinutes(5);

    public TempService(TempApiClient tempApiClient,
                       CacheService cacheService) {
        this.tempApiClient = tempApiClient;
        this.cacheService = cacheService;
    }

    /**
     * 현재 위치에서 가장 가까운 곳의 수온
     * obs_dt기준을 오름차순 정렬 정렬해서 첫번쨰 값만 주기
     */
    public TempInfo getTempInfo(double lat, double lon) {

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String keyOne = "temp:" + geo + ":" + GeoKeyUtil.dayKST();;
        final String keyAll = "tempAll:" + geo + ":" + GeoKeyUtil.dayKST();;


        // 0) 단건 캐시
        TempInfo cachedOne = cacheService.get(keyOne, TempInfo.class);
        if (cachedOne != null) return cachedOne;
        if (cacheService.isNull(keyOne)) return null;

        // 0-1) 목록 캐시가 있으면 거기서 첫번째를 뽑아 재사용
        List<TempInfo> cachedList = cacheService.get(keyAll, new TypeReference<>() {});
        if (cachedList != null && !cachedList.isEmpty()) {
            TempInfo first = cachedList.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingDouble(t -> kmOrInfinity(t.obsDt())))
                    .findFirst().orElse(null);
            if (first != null) {
                cacheService.set(keyOne, first, TTL);
                return first;
            }
        } else if (cacheService.isNull(keyAll)) {
            cacheService.setNull(keyOne, TTL);
            return null;
        }

        // 1) 원본 호출
        List<TempExternalDto> exts = tempApiClient.getTemp(lat, lon);

        // 2) 매핑 + 정렬
        List<TempInfo> sorted = exts.stream()
                .map(TempMapper::toDomain)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(t -> kmOrInfinity(t.obsDt())))
                .toList();

        // 빈 결과도 캐시하고 싶다면 setNull 사용(짧은 TTL) — 트래픽 폭주 방지용
//        if (sorted.isEmpty()) {
//            // 네거티브 캐시(잠깐만)
//            cacheService.setNull(keyAll, Duration.ofSeconds(5));
//            cacheService.setNull(keyOne, Duration.ofSeconds(5));
//            return null;
//        }

        // 3) 캐시 저장(목록/단건 동시 저장)
        cacheService.set(keyAll, sorted, TTL);
        TempInfo first = sorted.get(0);
        cacheService.set(keyOne, first, TTL);
        return first;
    }

    /**
     * 현재 위치에서 가까운 순으로 수온 다주기
     * obs_dt기준을 오름차순 정렬 정렬해서 나오지만 혹시 모르니
     */
    public List<TempInfo> getTempList(double lat, double lon) {

        final String geo = GeoKeyUtil.geoKey(lat, lon, GEO_DECIMALS);
        final String keyAll = "tempAll:" + geo + ":" + GeoKeyUtil.dayKST();;

        // 0) 목록 캐시
        List<TempInfo> cached = cacheService.get(keyAll, new TypeReference<>() {});
        if (cached != null) return cached;
        if (cacheService.isNull(keyAll)) return null;

        // 1) 원본 호출
        List<TempExternalDto> exts = tempApiClient.getTemp(lat, lon);

        // 2) 매핑 + 정렬
        List<TempInfo> sorted = exts.stream()
                .map(TempMapper::toDomain)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(t -> kmOrInfinity(t.obsDt())))
                .toList();

        // 빈 결과도 캐시하고 싶다면 setNull 사용(짧은 TTL) — 트래픽 폭주 방지용
//        if (sorted.isEmpty()) {
//            cacheService.setNull(keyAll, Duration.ofSeconds(5));
//            return null;
//        }

        // 3) 캐시 저장
        cacheService.set(keyAll, sorted, TTL);
        return sorted;
    }


    /** obsDt가 NaN이면 무한대로 간주하여 정렬 시 맨 뒤로 보냄 */
    private static double kmOrInfinity(double v) {
        return Double.isNaN(v) ? Double.POSITIVE_INFINITY : v;
    }
}
