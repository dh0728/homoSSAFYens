package com.homoSSAFYens.homSSAFYens.service;


import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.auth.oauth2.GoogleCredentials;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class FcmClient2 {

    private final String projectId = "badawatch-a11c4";
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://fcm.googleapis.com")
            .build();

    private String accessToken() throws Exception {
        // 클래스패스에서 읽는 쪽이 배포에 안전합니다.
        try (var is = new ClassPathResource("badaTime-dev-firebase-key.json").getInputStream()) {
            var creds = GoogleCredentials.fromStream(is)
                    .createScoped("https://www.googleapis.com/auth/firebase.messaging");
            creds.refreshIfExpired();
            return creds.getAccessToken().getTokenValue();
        }
    }

    public Mono<String> send(String token, String title, String body) {
        return Mono.fromCallable(this::accessToken)
                .flatMap(tok -> {
                    var payload = Map.of(
                            "message", Map.of(
                                    "token", token,
                                    "notification", Map.of("title", title, "body", body),
                                    "android", Map.of(
                                            "priority", "HIGH",
                                            // <-- camelCase 로 수정
                                            "notification", Map.of("channelId", "typhoon_alerts")
                                    ),
                                    "data", Map.of("type", "TYPHOON_ALERT")
                            )
                    );

                    return webClient.post()
                            .uri("/v1/projects/{pid}/messages:send", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tok)
                            .bodyValue(payload)
                            // 에러 바디 로깅
                            .retrieve()
                            .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), resp ->
                                    resp.bodyToMono(String.class).flatMap(bodyStr -> {
                                        log.error("FCM v1 error {}: {}", resp.statusCode(), bodyStr);
                                        return Mono.error(new RuntimeException("FCM send failed: " + bodyStr));
                                    })
                            )
                            .bodyToMono(String.class)
                            .doOnNext(resp -> log.info("FCM v1 resp: {}", resp));
                });
    }
}