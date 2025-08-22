package com.homoSSAFYens.homSSAFYens.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화.
 * - 로컬: classpath 리소스에서 서비스계정 JSON 읽기
 * - 배포: GoogleCredentials.getApplicationDefault()로 교체
 */
@Component
public class FirebaseConfig {

    @PostConstruct
    public void init() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return;

        // 방법 A) classpath에서 읽기(로컬 테스트)
        String path = "badaTime-dev-firebase-key.json"; // resources/firebase/ 아래에 둬
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            FirebaseOptions opts = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();
            FirebaseApp.initializeApp(opts);
        }

        // 방법 B) 환경변수 사용(배포 시 권장)
        // FirebaseOptions opts = FirebaseOptions.builder()
        //      .setCredentials(GoogleCredentials.getApplicationDefault())
        //      .build();
        // FirebaseApp.initializeApp(opts);
    }
}
