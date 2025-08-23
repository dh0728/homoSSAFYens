package com.homoSSAFYens.homSSAFYens.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Component
public class FcmClient {
    // Firebase Console > Project Settings > Cloud Messaging > Server key
    private static final String FCM_SERVER_KEY = "AAA...YOUR_SERVER_KEY...";
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://fcm.googleapis.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "key=" + FCM_SERVER_KEY)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public Mono<Void> send(String token, String title, String body) {
        Map<String, Object> payload = Map.of(
                "to", token,
                "notification", Map.of(
                        "title", title,
                        "body", body
                ),
                "data", Map.of(
                        "type", "TYPHOON_ALERT"
                )
        );
        return webClient.post()
                .uri("/fcm/send")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .then();
    }
}