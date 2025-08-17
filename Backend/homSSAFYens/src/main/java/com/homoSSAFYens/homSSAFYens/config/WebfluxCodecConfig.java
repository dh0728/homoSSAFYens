package com.homoSSAFYens.homSSAFYens.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

@Configuration
public class WebfluxCodecConfig {

    @Bean
    public CodecCustomizer textJsonAsJson(ObjectMapper om) {
        MediaType TEXT_JSON = MediaType.valueOf("text/json"); // 서버가 보내는 Content-Type

        return config -> {
            // 디코더: text/json 도 JSON으로 해석
            config.defaultCodecs().jackson2JsonDecoder(
                    new Jackson2JsonDecoder(om,
                            MediaType.APPLICATION_JSON,
                            MediaType.APPLICATION_PROBLEM_JSON,
                            TEXT_JSON)
            );
            // 인코더도 맞춰 둠(요청 보낼 때 필요할 수 있음)
            config.defaultCodecs().jackson2JsonEncoder(
                    new Jackson2JsonEncoder(om,
                            MediaType.APPLICATION_JSON,
                            MediaType.APPLICATION_PROBLEM_JSON,
                            TEXT_JSON)
            );
        };
    }
}
