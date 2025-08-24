package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import com.homoSSAFYens.homSSAFYens.dto.PointEnvelope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PointApiClient {

    private final WebClient wc;
    private final String apiKey;

    public PointApiClient(@Qualifier("pointWebClient") WebClient wc, ClientProperties props) {
        this.wc = wc;
        this.apiKey = props.getPoint().getKey();
    }

    /**
     * 특정구역의 낚시포인트 정보를 조회하는 서비스 api 테스트
     * @param lat 위도
     * @param lon 경도
     * @return PointEnvelope
     */
    public PointEnvelope getPoint(double lat, double lon) {
        return wc.get()
                .uri(u -> u
                        .queryParam("key", apiKey)
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .build()
                )
                .retrieve()
                .bodyToMono(PointEnvelope.class)
                .block();
    }

    /**
     * 특정구역의 낚시포인트 정보를 조회하는 서비스 api 테스트
     * @param lat 위도
     * @param lon 경도
     * @return String - (json)
     */
    public String testPoint(double lat, double lon) {
        return wc.get()
                .uri(u -> u
                        .queryParam("key", apiKey)
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .build()
                )
                .accept(MediaType.ALL) // 혹시 모를 콘텐츠 협상 문제 회피
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
