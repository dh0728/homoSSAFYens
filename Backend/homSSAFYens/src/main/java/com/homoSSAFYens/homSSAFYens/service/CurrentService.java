package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.CurrentApiClient;
import com.homoSSAFYens.homSSAFYens.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrentService {

    private final CurrentApiClient currentApiClient;

    public CurrentService(CurrentApiClient currentApiClient) {

        this.currentApiClient = currentApiClient;
    }

    /** api 호출 시간 기준 가장 가까운 시간 날씨만 제공 */
    public CurrentResponse getCurrentInfo(double lat, double lon) {
        CurrentEnvelope env = currentApiClient.getCurrent(lat, lon);
        CurrentResponse full = CurrentMapper.toResponse(env);

        List<?> list = full.weather();
        if (list == null || list.isEmpty()) {
            return new CurrentResponse(List.of(), full.info());
        }
        return new CurrentResponse(List.of(full.weather().get(0)), full.info());
    }

    /** 현재 날씨 6시간 이후 까지 모두 제공*/
    public CurrentResponse getCurrentListInfo(double lat, double lon) {
        CurrentEnvelope env = currentApiClient.getCurrent(lat,lon);
        return CurrentMapper.toResponse(env); // weather 리스트 + info
    }

}
