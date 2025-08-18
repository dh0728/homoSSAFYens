package com.homoSSAFYens.homSSAFYens.service;

import com.homoSSAFYens.homSSAFYens.client.TempApiClient;
import com.homoSSAFYens.homSSAFYens.dto.TempExternalDto;
import com.homoSSAFYens.homSSAFYens.dto.TempInfo;
import com.homoSSAFYens.homSSAFYens.dto.TempMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TempService {

    private final TempApiClient tempApiClient;

    public TempService(TempApiClient tempApiClient) {
        this.tempApiClient = tempApiClient;
    }

    /**
     * 현재 위치에서 가장 가까운 곳의 수온
     * obs_dt기준을 오름차순 정렬 정렬해서 첫번쨰 값만 주기
     */
    public TempInfo getTempInfo(double lat, double lon) {
        List<TempExternalDto> exts = tempApiClient.getTemp(lat, lon);

        List<TempInfo> sorted = exts.stream()
                .map(TempMapper::toDomain)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(t -> kmOrInfinity(t.obsDt())))
                .toList();
        if (sorted.isEmpty()) {
            return null;
        }

        return sorted.get(0);
    }

    /**
     * 현재 위치에서 가까운 순으로 수온 다주기
     * obs_dt기준을 오름차순 정렬 정렬해서 나오지만 혹시 모르니
     */
    public List<TempInfo> getTempList(double lat, double lon) {
        List<TempExternalDto> exts = tempApiClient.getTemp(lat, lon);

        List<TempInfo> sorted = exts.stream()
                .map(TempMapper::toDomain)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(t -> kmOrInfinity(t.obsDt())))
                .toList();
        if (sorted.isEmpty()) {
            return null;
        }

        return sorted;
    }


    /** obsDt가 NaN이면 무한대로 간주하여 정렬 시 맨 뒤로 보냄 */
    private static double kmOrInfinity(double v) {
        return Double.isNaN(v) ? Double.POSITIVE_INFINITY : v;
    }
}
