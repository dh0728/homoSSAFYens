package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.entity.DangerZone;
import com.homoSSAFYens.homSSAFYens.repo.DangerZoneRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DangerZoneService {

    private final DangerZoneRepo repo;

    /**
     * (lat,lon) 반경 rKm 내에서 [fromEpochSec, toEpochSec] 구간의 시간별 조위상승속도 반환
     */
    public List<BigDecimal> loadRiseSpeeds(double lat, double lon, double rKm,
                                           long fromEpochSec, long toEpochSec) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime from = LocalDateTime.ofInstant(Instant.ofEpochSecond(fromEpochSec), zone);
        LocalDateTime to   = LocalDateTime.ofInstant(Instant.ofEpochSecond(toEpochSec), zone);
        double radiusMeters = rKm * 1000.0;
        return repo.findSpeedsInRadiusBetween(lat, lon, radiusMeters, from, to);
    }

    // (선택) 위험구역 여부 체크가 필요하면 여기에 추가:
    public boolean isInsideRiskZone(double lat, double lon) {
        // TODO: 연안위험구역 폴리곤/버퍼 매칭 넣을 자리
        return true;
    }
}
