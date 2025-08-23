package com.homoSSAFYens.homSSAFYens.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.homoSSAFYens.homSSAFYens.dto.TyphoonPoint;

// 경로 저장
@Component
public class TyphoonPathStore {
    public static class StoredPath {
        public final String name;
        public final double bufferRadiusKm;
        public final List<TyphoonPoint> path;
        public StoredPath(String name, double bufferRadiusKm, List<TyphoonPoint> path) {
            this.name = name;
            this.bufferRadiusKm = bufferRadiusKm;
            this.path = path;
        }
    }

    private final AtomicReference<StoredPath> latest = new AtomicReference<>(null);

    public void set(StoredPath sp) { latest.set(sp); }
    public StoredPath get() { return latest.get(); }
    public boolean isEmpty() { return latest.get() == null; }
}
