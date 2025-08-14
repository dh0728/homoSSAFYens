package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class CurrentApiClient {

    private final WebClient wc;
    private final String apiKey;

    public CurrentApiClient(@Qualifier("currentWebClient") WebClient wc, ClientProperties props) {
        this.wc = wc;
        this.apiKey = props.getCurrent().getKey();
    }

    /**
     * 특정지역의 현재 날씨 데이터를 조회 api 테스트 호출
     * @param lat 위도
     * @param lon 경도
     * @return Stirng (json)
     */
    public String testCurrent(double lat, double lon) {
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
