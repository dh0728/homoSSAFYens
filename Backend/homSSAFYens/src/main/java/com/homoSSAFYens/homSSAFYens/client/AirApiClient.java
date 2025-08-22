package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AirApiClient {

    private final WebClient wc;
    private final ClientProperties.One cfg;
    private final String apiKey; // 발급받은 키 원문(평문이면 영숫자, 인코딩키면 % 포함)

    public AirApiClient(@Qualifier("airWebClient") WebClient wc, ClientProperties props) {
        this.wc = wc;
        this.cfg = props.getAir();
        this.apiKey = cfg.getKey() == null ? null : cfg.getKey().trim(); // 공백 제거
    }

    /**
     * 대기오염정보 측정소별 측정정보
     * @return String (json)
     */
    public String testAir() {
        String station  = UriUtils.encode("강남구", StandardCharsets.UTF_8);
        return wc.get()
                .uri(u -> u
                        .queryParam("serviceKey", apiKey)   // 이름은 그대로, 값은 우리가 인코딩
                        .queryParam("returnType", "json")
                        .queryParam("numOfRows", "10")
                        .queryParam("pageNo", "1")
                        .queryParam("stationName", station)
                        .queryParam("dataTerm", "DAILY")
                        .queryParam("ver", "1.3")
                        .build()
                )
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     *
     * @param s             // 측정소 평문
     * @param dateTerm      // 요청 데이터 기간 (1일: DAILY, 1개월: MONTH, 3개월: 3MONTH)
     * @param numOfRows     // 한 페이지 결과 수
     * @param pageNo        // 페이지번호
     * @return
     */
    public String getAirInfo(String s, String dateTerm, int numOfRows, int pageNo) {

        String station  = UriUtils.encode(s, StandardCharsets.UTF_8);
        return wc.get()
                .uri(u -> u
                        .queryParam("serviceKey", apiKey)   // 이름은 그대로, 값은 우리가 인코딩
                        .queryParam("returnType", "json")
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", pageNo)
                        .queryParam("stationName", station)
                        .queryParam("dataTerm", dateTerm)
                        .queryParam("ver", "1.3")
                        .build()
                )
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
