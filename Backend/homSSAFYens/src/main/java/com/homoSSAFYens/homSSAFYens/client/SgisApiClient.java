package com.homoSSAFYens.homSSAFYens.client;

import com.homoSSAFYens.homSSAFYens.config.ClientProperties;
import com.homoSSAFYens.homSSAFYens.dto.SgisGeoCodewgs84Response;
import com.homoSSAFYens.homSSAFYens.dto.SgisKeyResponse;
import com.homoSSAFYens.homSSAFYens.dto.SgisTranscoordResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class SgisApiClient {

    private static final String REDIS_KEY = "sgisAccessToken";
    private static final String ISSUE_PATH = "/auth/authentication.json";
    private static final String TRANS_POSITION_PATH = "/transformation/transcoord.json";
    private static final String GEOCODEWGS84_PATH = "/addr/geocodewgs84.json";

    private final WebClient wc;
    private final String consumerKey;
    private final String consumerSecret;
    private final String keyParam;
    private final String SecretParam;
    private final StringRedisTemplate redis;

    public SgisApiClient(@Qualifier("sgisWebClient") WebClient wc,
                         ClientProperties props,
                         StringRedisTemplate redis) {
        this.wc = wc;
        this.consumerKey = props.getSgis().getConsumerKey();
        this.consumerSecret = props.getSgis().getConsumerSecret();
        this.keyParam = props.getSgis().getKeyParam();
        this.SecretParam = props.getSgis().getSecretParam();
        this.redis = redis;

    }

    public String getSgisAccessKey() {

        // 1) 캐시 히트
        String cashed = redis.opsForValue().get(REDIS_KEY);
        if (StringUtils.hasText(cashed)) {
            return cashed;
        }

        // 2) 외부 API 호출 - 없으면
        SgisKeyResponse res = wc.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ISSUE_PATH)
                        .queryParam(keyParam, consumerKey)
                        .queryParam(SecretParam, consumerSecret)
                        .build())
                .retrieve()
                .bodyToMono(SgisKeyResponse.class)
                .block();

        if (res == null) {
            throw new IllegalStateException("SGIS token response is null");
        }
        if (res.getErrCd() != null && res.getErrCd() != 0) {
            throw new IllegalStateException("SGIS token error: " + res.getErrCd() + " - " + res.getErrMsg());
        }
        if (res.getResult() == null || !StringUtils.hasText(res.getResult().getAccessToken())) {
            throw new IllegalStateException("SGIS token missing in response");
        }

        String token = res.getResult().getAccessToken();

        // 3) 3시간 50분(=230분) TTL로 저장
        redis.opsForValue().set(REDIS_KEY, token, Duration.ofMinutes(230));

        return token;
    }

    // 좌표 변환   EPSG:4326 to EPSG:5181
    public SgisTranscoordResponse getSgisRgeocode(double lat, double lon) {

        // 1) 토큰을 final로 확정 (캐시 없으면 발급)
        final String token = Optional.ofNullable(redis.opsForValue().get(REDIS_KEY))
                .filter(StringUtils::hasText)
                .orElseGet(this::getSgisAccessKey);

        // 2) SGIS 좌표변환 호출
        SgisTranscoordResponse res = wc.get()
                .uri(b -> b
                        .path(TRANS_POSITION_PATH)
                        .queryParam("accessToken", token)
                        .queryParam("src", 4326)
                        .queryParam("dst", 5181)
                        // 4326에서는 X=경도(lon), Y=위도(lat)
                        .queryParam("posX", lon)
                        .queryParam("posY", lat)
                        .build())
                .exchangeToMono(cr -> {
                    if (cr.statusCode().is2xxSuccessful()) {
                        return cr.bodyToMono(SgisTranscoordResponse.class);
                    } else {
                        return cr.bodyToMono(String.class)
                                .defaultIfEmpty("(empty)")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        "SGIS transcoord failed: " + cr.statusCode().value() + " body=" + body)));
                    }
                })
                .block();

        if (res == null) {
            throw new IllegalStateException("SGIS transcoord response is null");
        }
        if (res.getErrCd() != null && res.getErrCd() != 0) {
            throw new IllegalStateException("SGIS transcoord error: errCd=" + res.getErrCd()
                    + ", errMsg=" + res.getErrMsg());
        }
        if (res.getResult() == null) {
            throw new IllegalStateException("SGIS transcoord result is null");
        }

        return res;
    }


    // 입력된 주소 위치 제공 API(좌표계:WGS84, EPSG:4326)
    public Optional<SgisGeoCodewgs84Response> getGeoCodewgs84(String address) {
        final String token = Optional.ofNullable(redis.opsForValue().get(REDIS_KEY))
                .filter(StringUtils::hasText)
                .orElseGet(this::getSgisAccessKey);

        SgisGeoCodewgs84Response res = wc.get()
                .uri(b -> b
                        .path(GEOCODEWGS84_PATH)
                        .queryParam("accessToken", token)
                        .queryParam("address", address)
                        .queryParam("pagenum", 0)
                        .queryParam("resultcount", 1)
                        .build())
                .retrieve()
                .bodyToMono(SgisGeoCodewgs84Response.class)
                .block();

        if (res == null) {
            throw new IllegalStateException("SGIS geocode(WGS84) response is null");
        }

        // 검색 결과 없음 처리
        if (res.getErrCd() != null && res.getErrCd() == -100) {
            return Optional.empty();
        }

        // 그 외 에러는 예외 처리
        if (res.getErrCd() != null && res.getErrCd() != 0) {
            throw new IllegalStateException("SGIS geocode(WGS84) error: errCd=" + res.getErrCd()
                    + ", errMsg=" + res.getErrMsg());
        }

        if (res.getResult() == null || res.getResult().getResultData() == null
                || res.getResult().getResultData().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(res);
    }
}
