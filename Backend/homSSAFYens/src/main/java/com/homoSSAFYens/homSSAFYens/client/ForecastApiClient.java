package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ForecastApiClient {

    private final WebClient wc;
    private final String apiKey;

    public ForecastApiClient(@Qualifier("forecastWebClient") WebClient wc, ClientProperties props) {
        this.wc = wc;
        this.apiKey = props.getForecast().getKey();
    }

    /**
     * 특정지역의 7일간 날씨데이터를 조회 api 호출 확인용
     * @param lat 위도
     * @param lon 경도
     * @return String - (json)
     */
    public String testForecast(double lat, double lon) {
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
