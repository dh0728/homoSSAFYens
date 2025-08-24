package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import com.homoSSAFYens.homSSAFYens.dto.AirKoreaResponse;
import com.homoSSAFYens.homSSAFYens.dto.AirResponse;
import com.homoSSAFYens.homSSAFYens.dto.StationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AirApiClient {

    private final WebClient wc;
    private final ClientProperties.One cfg;
    private final String apiKey; // 발급받은 키 원문(평문이면 영숫자, 인코딩키면 % 포함)

    private static final String AIR_PATH ="/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty";
    private static final String NEAREST_STATION_PATH ="/MsrstnInfoInqireSvc/getNearbyMsrstnList";

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
    public AirKoreaResponse getAirInfo(String s, String dateTerm, int numOfRows, int pageNo) {

        String station  = UriUtils.encode(s, StandardCharsets.UTF_8);
        return wc.get()
                .uri(u -> u
                        .path(AIR_PATH)
                        .queryParam("serviceKey", apiKey)   // 이름은 그대로, 값은 우리가 인코딩
                        .queryParam("returnType", "json")
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("pageNo", pageNo)
                        .queryParam("stationName", station)
                        .queryParam("dataTerm", dateTerm)
                        .queryParam("ver", "1.3")
                        .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(AirKoreaResponse.class)
                .block();
    }

    /**
     * 근접 측정소 조회 (AirKorea)
     * @param tmX EPSG:5186 X
     * @param tmY EPSG:5186 Y
     */
    public StationResponse getNearestStation(double tmX, double tmY) {

        StationResponse res = wc.get()
                .uri(u -> u
                        .path(NEAREST_STATION_PATH)
                        .queryParam("serviceKey", apiKey)   // <-- 가급적 '디코딩키(평문)' 사용
                        .queryParam("returnType", "json")
                        .queryParam("tmX", tmX)
                        .queryParam("tmY", tmY)
                        .queryParam("ver", "1.1")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(cr -> {
                    HttpStatusCode sc = cr.statusCode();
                    if (sc.is2xxSuccessful()) {
                        return cr.bodyToMono(StationResponse.class);
                    }
                    // 실패 시 에러 바디를 포함해 예외로 던져 원인 파악
                    return cr.bodyToMono(String.class)
                            .defaultIfEmpty("(empty)")
                            .flatMap(body -> Mono.error(new IllegalStateException(
                                    "getNearbyMsrstnList failed: " + sc.value() + " body=" + body)));
                })
                .block();

        if (res == null || res.getResponse() == null || res.getResponse().getHeader() == null) {
            throw new IllegalStateException("Invalid response from getNearbyMsrstnList");
        }

        String code = res.getResponse().getHeader().getResultCode();
        if (!"00".equals(code)) {
            String msg = res.getResponse().getHeader().getResultMsg();
            throw new IllegalStateException("AirKorea API error: resultCode=" + code + ", resultMsg=" + msg);
        }

        return res;
    }



}
