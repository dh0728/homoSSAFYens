package com.example.dive.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dive.data.api.RetrofitProvider
import com.example.dive.data.model.LocateReq
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * /locate 호출 담당
 */
class LocateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var lat = inputData.getDouble("lat", 0.0)
        var lon = inputData.getDouble("lon", 0.0)
        val ts  = inputData.getLong("ts", System.currentTimeMillis() / 1000L)

        // 입력 좌표가 없을 때만, 내부에서 한 번 위치를 취득 (하트비트 등)
        if (lat == 0.0 && lon == 0.0) {
            val hasFine = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            // (Q+ 전용) 백그라운드 위치 권한 체크 — Worker는 보통 백그라운드이므로 권장
            val hasBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            if (hasFine || hasCoarse) {
                try {
                    val fused = LocationServices.getFusedLocationProviderClient(applicationContext)

                    // 우선 getCurrentLocation 시도
                    val loc = try {
                        Tasks.await(
                            fused.getCurrentLocation(
                                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                null
                            ),
                            10, TimeUnit.SECONDS
                        )
                    } catch (e: Exception) {
                        null
                    }

                    if (loc != null) {
                        lat = loc.latitude
                        lon = loc.longitude
                    } else {
                        // 폴백: lastLocation (캐시 위치)
                        val last = try {
                            Tasks.await(fused.lastLocation, 3, TimeUnit.SECONDS)
                        } catch (e: Exception) {
                            null
                        }
                        if (last != null) {
                            lat = last.latitude
                            lon = last.longitude
                        }
                    }
                } catch (se: SecurityException) {
                    Log.w("LocateWorker", "SecurityException: location permission denied in background", se)
                } catch (e: Exception) {
                    Log.w("LocateWorker", "Failed to obtain location in worker", e)
                }
            } else {
                Log.d("LocateWorker", "No location permission (FINE/COARSE). Skip fetching.")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBg) {
                Log.d("LocateWorker", "No ACCESS_BACKGROUND_LOCATION (Q+). Background fetch may be limited.")
            }
        }

        // 여전히 좌표가 없으면 (0,0)으로 잘못 보내지 말고 스킵
        if (lat == 0.0 && lon == 0.0) {
            Log.d("LocateWorker", "No coordinates available. Skip /locate.")
            return@withContext Result.success()
        }

        val deviceId = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        return@withContext try {
            Log.d("LocateWorker", "POST /locate lat=$lat lon=$lon ts=$ts")
            val resp = RetrofitProvider.api.locate(LocateReq(deviceId, lat, lon, ts)).execute()
            Log.d("LocateWorker", "resp=${resp.code()}")
            Result.success()
        } catch (e: Exception) {
            Log.e("LocateWorker", "Network error: ${e.message}", e)
            Result.retry()
        }
    }
}
