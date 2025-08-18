package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.PointApiClient;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final PointApiClient pointApiClient;

    public PointService(PointApiClient pointApiClient) {
        this.pointApiClient = pointApiClient;
    }


    public PointResponse getPointList(double lat, double lon) {
        PointEnvelope env = pointApiClient.getPoint(lat, lon);
        if (env == null) return new PointResponse(null, java.util.List.of());

        PointInfo info = PointInfoMapper.toDomain(env.info());
        List<PointFishingPoint> points = PointFishingPointMapper.toDomainList(
                env.fishingPoint()
        );
        return new PointResponse(info, points);

    }

}
