package com.example.dive.data.model

/**
 * 워치에서 지도 요청 시 보내는 데이터 구조.
 * @param point 사용자가 선택한 낚시 포인트 정보.
 * @param phoneLocation 휴대폰의 현재 위치 정보.
 */
data class FishingPointDetail(
    val point: FishingPoint,
    val phoneLocation: LocationData?
)
