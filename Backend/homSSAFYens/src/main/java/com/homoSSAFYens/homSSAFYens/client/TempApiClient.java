package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import com.homoSSAFYens.homSSAFYens.dto.TempExternalDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class TempApiClient {

    private final WebClient wc;
    private final String apiKey;

    public TempApiClient(@Qualifier("tempWebClient") WebClient wc, ClientProperties props) {
        this.wc = wc;
        this.apiKey = props.getTemp().getKey();
    }

    /**
     * 현재 바다수온 정보 조회 api 호출
     * 바로 리스트에 담겨있음
     * @param lat 위도
     * @param lon 경도
     * @return List<TempExternalDto>
     */
    public List<TempExternalDto> getTemp(double lat, double lon) {
        return wc.get()
                .uri(u -> u
                        .queryParam("key", apiKey)
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .build()
                )
                .accept(MediaType.ALL) // 혹시 모를 콘텐츠 협상 문제 회피
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TempExternalDto>>(){})
                .block();
    }


    /**
     * 현재 바다수온 정보 조회 api 호출 확인용
     * @param lat 위도
     * @param lon 경도
     * @return String - (json)
     */
    public String testTemp(double lat, double lon) {
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
