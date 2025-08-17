package com.homoSSAFYens.homSSAFYens.service;


import com.homoSSAFYens.homSSAFYens.client.TideApiClient;
import com.homoSSAFYens.homSSAFYens.dto.TideDailyInfo;
import com.homoSSAFYens.homSSAFYens.dto.TideExternalDto;
import com.homoSSAFYens.homSSAFYens.dto.TideMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TideService {

    private final TideApiClient tideApiClient;

    public TideService(TideApiClient tideApiClient) {
        this.tideApiClient = tideApiClient;
    }

    /** 7일치 모두 전달 하기 */
    public List<TideDailyInfo> getWeekly(double lat, double lon) {
        List<TideExternalDto> ext = tideApiClient.getTide(lat, lon);
        if (ext == null) return null;
        return ext.stream().map(TideMapper::toDaily).collect(Collectors.toList());
    }

    /** 오늘자만  */
    public TideDailyInfo getDaily(double lat, double lon) {
        LocalDate today = LocalDate.now();
        return getWeekly(lat, lon).stream()
                .filter(d -> today.equals(d.date()))
                .findFirst()
                .orElse(null);
    }
}
