package com.homoSSAFYens.homSSAFYens.repo;

public interface Keys {
    static String device(String id) { return "device:" + id; }     // HASH
    static String tide7d(String geoKey) { return "tide7d:" + geoKey; } // (다음 단계에서 사용)
    static String tide1d(String geoKey) { return "tide1d:" + geoKey; } // (다음 단계에서 사용)
}
