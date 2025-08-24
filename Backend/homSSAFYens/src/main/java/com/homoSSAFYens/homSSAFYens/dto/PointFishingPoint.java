package com.homoSSAFYens.homSSAFYens.dto;

import java.util.List;
import java.util.Map;

public record PointFishingPoint(
        String name,                           // 구역 이름 (예: 수영만 부근)
        String pointName,                      // 포인트명 (point_nm)
        DepthRange depth,                      // 수심 범위(m)
        String material,                       // 지형/재질 (암 등)
        String tideTime,                       // 물때 (원문 유지)
        Map<String, List<String>> targetByFish,// 어종 → 기법 리스트 (순서 보존)
        double lat,
        double lon,
        String photo,                          // 파일명
        String addr,
        boolean seaside,                       // "y" → true
        Double pointDtKm                       // 거리(km)로 통일, 파싱 실패 시 null
) {}
