package com.homoSSAFYens.homSSAFYens.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.homoSSAFYens.homSSAFYens.dto.LocationDto;
import com.homoSSAFYens.homSSAFYens.dto.TyphoonPathRequest;
import com.homoSSAFYens.homSSAFYens.utill.GeoUtil;

@Service
public class TyphoonAlertService {

    private final DeviceRegistry deviceRegistry;
    private final FcmClient fcmClient;
    private final TyphoonPathStore pathStore; // ★ 추가

    public TyphoonAlertService(DeviceRegistry deviceRegistry,
                               FcmClient fcmClient,
                               TyphoonPathStore pathStore) { // ★ 생성자 주입
        this.deviceRegistry = deviceRegistry;
        this.fcmClient = fcmClient;
        this.pathStore = pathStore;
    }
    
    public Result checkAndNotifyUsingStoredPath() {
        var stored = pathStore.get();
        if (stored == null) {
            return new Result(false, List.of(), null);
        }
        // 기존 checkAndNotify(req) 재사용을 위해 얇게 래핑
        var req = new TyphoonPathRequest(stored.name, stored.bufferRadiusKm, stored.path);
        return checkAndNotify(req);
    }
    
    

    public Result checkAndNotify(TyphoonPathRequest req) {
        // path -> double[][]
        double[][] path = req.path().stream()
                .map(p -> new double[]{p.lat(), p.lon()})
                .toArray(double[][]::new);

        var snapshot = deviceRegistry.snapshot();
        List<String> matched = new ArrayList<>();
        double minDist = Double.MAX_VALUE;

        for (var dev : snapshot.values()) {
            LocationDto loc = dev.lastLocation;
            if (loc == null) continue; // NPE 방지 (혹시 위치가 없는 기기)

            double d = GeoUtil.minDistanceToPointsKm(loc.lat(), loc.lon(), path);
            if (d < minDist) minDist = d;

            if (d <= req.bufferRadiusKm()) {
                matched.add(dev.deviceId);
                // 데모용 fire-and-forget
                fcmClient.send(
                        dev.fcmToken,
                        "태풍 경로 경보",
                        String.format("[%s] 경로가 내 위치 %.2fkm 이내로 통과", req.name(), d)
                ).subscribe();
            }
        }

        return new Result(true, matched, minDist == Double.MAX_VALUE ? null : minDist);
    }

    public record Result(boolean ok, List<String> matchedDevices, Double minDistanceKm) {}
}
