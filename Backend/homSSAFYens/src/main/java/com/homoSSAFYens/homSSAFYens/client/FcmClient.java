package com.homoSSAFYens.homSSAFYens.client;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FCM Admin SDK로 data-only 메시지를 전송한다.
 * (클라에서 백그라운드 수신 후 현재 위치 재검증 → 로컬 알림 표시)
 */
@Component
public class FcmClient {

    public void sendData(String token, Map<String, String> data) throws Exception {
        Message msg = Message.builder()
                .setToken(token)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH).build())
                .build();
        FirebaseMessaging.getInstance().send(msg);
    }
}
