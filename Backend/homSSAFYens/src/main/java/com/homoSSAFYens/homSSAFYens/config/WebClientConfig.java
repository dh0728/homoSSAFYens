package com.homoSSAFYens.homSSAFYens.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;


import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class WebClientConfig {

    private WebClient build(WebClient.Builder builder, ClientProperties.One p) {
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(
                        p.getTimeoutMs() == null ? 3000 : p.getTimeoutMs()
                ));

        return builder
                .baseUrl(p.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }

    @Bean @Qualifier("tideWebClient")
    public WebClient tideWebClient(WebClient.Builder builder, ClientProperties props) { return build(builder, props.getTide()); }

    @Bean @Qualifier("currentWebClient")
    public WebClient currentWebClient(WebClient.Builder builder, ClientProperties props) { return build(builder, props.getCurrent()); }

    @Bean @Qualifier("forecastWebClient")
    public WebClient forecastWebClient(WebClient.Builder builder, ClientProperties props) { return build(builder, props.getForecast()); }

    @Bean @Qualifier("tempWebClient")
    public WebClient tempWebClient(WebClient.Builder builder, ClientProperties props) { return build(builder, props.getTemp()); }

    @Bean @Qualifier("pointWebClient")
    public WebClient pointWebClient(WebClient.Builder builder, ClientProperties props) { return build(builder, props.getPoint()); }

    /** air 전용: WebClient의 추가 인코딩 완전히 끔 (값은 우리가 직접 인코딩해서 넣음) */
    @Bean @Qualifier("airWebClient")
    public WebClient airWebClient(WebClient.Builder builder, ClientProperties props) {
        ClientProperties.One p = props.getAir();

        // baseUrl는 인코딩되지 않은 "정상 URL"이어야 함.
        DefaultUriBuilderFactory f = new DefaultUriBuilderFactory(p.getBaseUrl());

        // 값(value)만 인코딩 → '+'도 %2B 로 인코딩됨
        f.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);


        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(p.getTimeoutMs() == null ? 3000 : p.getTimeoutMs()));

        return builder
                .uriBuilderFactory(f)
                .baseUrl(p.getBaseUrl())
//                .filter((req, next) -> {
//                    log.info("AIR REQ {} {}", req.method(), req.url()); // 최종 URL 확인
//                    return next.exchange(req);
//                })
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }

    @Bean @Qualifier("sgisWebClient")
    public WebClient sgisKeyWebClient(WebClient.Builder builder, ClientProperties props) {
        ClientProperties.Sgis p = props.getSgis();

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(p.getTimeoutMs() == null ? 3000 : p.getTimeoutMs()));

        return builder
                .baseUrl(p.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();


    }


}
