package com.homoSSAFYens.homSSAFYens.repo;

import com.homoSSAFYens.homSSAFYens.entity.DangerZone;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DangerZoneRepo extends Repository<DangerZone, Long> {

    @Query(value = """
        SELECT tdlv_rsng_ve
        FROM danger_zone
        WHERE ymdh BETWEEN :from AND :to
          AND ST_Distance_Sphere(POINT(sta_lo, sta_la), POINT(:lon, :lat)) <= :radiusMeters
        ORDER BY ymdh
        """, nativeQuery = true)
    List<java.math.BigDecimal> findSpeedsInRadiusBetween(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

}
