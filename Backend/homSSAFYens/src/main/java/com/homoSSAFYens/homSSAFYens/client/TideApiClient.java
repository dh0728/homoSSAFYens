package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import com.homoSSAFYens.homSSAFYens.dto.TideExternalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Component
public class TideApiClient {

    private final WebClient wc;
    private final String apiKey;

    public TideApiClient(@Qualifier("tideWebClient") WebClient wc, ClientProperties props) {
        this.wc = wc;
        this.apiKey = props.getTide().getKey();
    }

    /**
     * 특정지역의 물 떄, 간만조 시각 조회
     * @param lat 위도
     * @param lon 경도
     * @return List<TideExternalDto> 7일치 값을 TideExternalDto로 1일씩 분리
     */
    public List<TideExternalDto> getTide(double lat, double lon) {
        return wc.get()
                .uri(u -> u
                        .queryParam("key", apiKey) // 인증키(쿼리 파라미터)
                        .queryParam("lat", lat)           // 위도
                        .queryParam("lon", lon)           // 경도
                        // 키가 이미 URL-encoded면 true, 평문이면 false/생략
                        .build()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TideExternalDto>>(){} )
                .block();
    }

    /**
     * 특정지역의 물 떄, 간만조 시각 조회 api 호출 확인용
     * @param lat 위도
     * @param lon 경도
     * @return String - (json)
     */
    public String testTide(double lat, double lon) {
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
