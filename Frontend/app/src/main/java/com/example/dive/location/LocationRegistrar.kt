package com.example.dive.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.TimeUnit

/**
 * FusedLocationProvider에 PendingIntent를 등록해서
 * ↓ 아래 조건을 만족할 때 브로드캐스트(= LocationUpdateReceiver.onReceive)가 호출되도록 설정
 *  - 힌트 주기: 대략 30분 간격(정확 보장 X, OS가 배터리/상태에 따라 조절)
 *  - 이동 거리: 3km 이상일 때만 콜백 (핵심 조건)
 */
object LocationRegistrar {

    fun register(ctx: Context) {
        // FusedLocationProviderClient 얻기
        val client = LocationServices.getFusedLocationProviderClient(ctx)

        // LocationRequest 빌드
        val req = LocationRequest.Builder(
            // 위치 정확도/소비 전력 정책: BALANCED는 셀/와이파이 기반 중심 (배터리 절약, 수십 m~수백 m 오차)
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,

            // setIntervalMillis와 동일 의미(히든 네이밍): "원하는" 기본 간격 힌트.
            // 콜백 주기를 강제하지 않으며, OS/제조사가 스로틀링 가능.
            TimeUnit.MINUTES.toMillis(30)
        )
            // ★ 핵심: "최소 이동 거리" 필터.
            // 마지막 콜백 기준으로 3000m 이상 이동했을 때만 새 콜백을 허용.
            .setMinUpdateDistanceMeters(3000f)

            // 연속 업데이트 간 "최소 간격" 힌트.
            // 너무 잦은 콜백 방지용으로 OS에게 힌트를 줌(역시 강제는 아님).
            .setMinUpdateIntervalMillis(TimeUnit.MINUTES.toMillis(15))

            // OS가 배터리 절약을 위해 콜백을 모아 배치할 수 있는데, 그 "최대 지연" 상한 힌트.
            // 1시간 넘어가도록 늦게 몰아서 주지 말라는 의도.
            .setMaxUpdateDelayMillis(TimeUnit.HOURS.toMillis(1))

            // true면(정확도 대기) 더 정확한 GNSS fix를 기다리느라 첫 응답이 늦어질 수 있음.
            // 우리는 빠른 응답/저전력 우선이므로 false.
            .setWaitForAccurateLocation(false)
            .build()

        // 브로드캐스트 받을 대상(리시버)로 PendingIntent 구성
        val pi = PendingIntent.getBroadcast(
            ctx,
            0,
            Intent(ctx, LocationUpdateReceiver::class.java),
            // UPDATE_CURRENT: 동일 PendingIntent 있으면 extras 갱신
            // IMMUTABLE    : Android 12+ 보안 요건(외부에서 내용 변경 불가)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 권한 없는 경우 SecurityException이 날 수 있으므로 방어
        try {
            client.requestLocationUpdates(req, pi)
        } catch (_: SecurityException) {
            // 위치 권한 미허용 상태. Activity 쪽에서 권한 요청 필요.
        }
    }
}
