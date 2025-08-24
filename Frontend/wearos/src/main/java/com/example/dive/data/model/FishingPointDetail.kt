package com.example.dive.data.model

/**
 * 낚시 포인트 상세 화면에 필요한 모든 데이터를 담는 클래스.
 * @param point 사용자가 선택한 낚시 포인트 정보.
 * @param phoneLocation 휴대폰의 현재 위치 정보.
 */
data class FishingPointDetail(
    val point: FishingPoint,
    val phoneLocation: LocationData?
)
