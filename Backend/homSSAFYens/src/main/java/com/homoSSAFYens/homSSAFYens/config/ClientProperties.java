package com.homoSSAFYens.homSSAFYens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 'clients' 섹션을 자바 객체로 바인딩하는 설정 클래스
 *
 * 예) yml
 * clients:
 *   tide:
 *     base-url: https://...
 *     key: ${BADATIME_API_KEY}
 *     key-param: ServiceKey
 *     timeout-ms: 3000
 *     encoded-key: false
 *
 *  ↳ 위 값들이 아래 ClientProperties.One(하위 클래스)의 필드로 자동 매핑되는거임
 *    - base-url   → baseUrl
 *    - key        → key
 *    - key-param  → keyParam
 *    - timeout-ms → timeoutMs
 *    - encoded-key→ encodedKey
 *
 * 사용법:
 * 1) @EnableConfigurationProperties(ClientProperties.class)를 @Configuration에 붙이거나
 * 2) @SpringBootApplication 에 @ConfigurationPropertiesScan 을 사용.
 */
@ConfigurationProperties(prefix = "clients")
public class ClientProperties {

    /**
     * 각 외부 API 클라이언트가 공통으로 가지는 것들
     */
    public static class One {

        /** API Base URL */
        private String baseUrl;
        /** 인증키(쿼리파라미터로 보냄) */
        private String key;
        /** WebClient responseTimeout(ms) */
        private Integer timeoutMs = 3000;

        /**
         * 인증키를 전달할 "쿼리 파라미터 이름".
         * - 바다타임 계열: 보통 'ServiceKey'
         * - 공공 데이터 포털은 앞 글자 소문자임 yml에서 알아서 덮어씀
         */
        private String keyParam = "ServiceKey";

        /**
         * 키가 이미 URL-인코딩된 문자열이면 true, 평문이면 false
         * yml에서 서비스별로 덮어씀.
         */
        private Boolean encodedKey = Boolean.FALSE;

        // --- getters/setters ---
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

        public String getKeyParam() { return keyParam; }
        public void setKeyParam(String keyParam) { this.keyParam = keyParam; }

        public Boolean getEncodedKey() { return encodedKey; }
        public void setEncodedKey(Boolean encodedKey) { this.encodedKey = encodedKey; }
    }

    private One tide;
    private One current;
    private One forecast;
    private One temp;
    private One point;
    private One air;

    // --- getters/setters ---
    public One getTide() { return tide; }
    public void setTide(One tide) { this.tide = tide; }

    public One getCurrent() { return current; }
    public void setCurrent(One current) { this.current = current; }

    public One getForecast() { return forecast; }
    public void setForecast(One day7_weather) { this.forecast = day7_weather; }

    public One getTemp() { return temp; }
    public void setTemp(One temp) { this.temp = temp; }

    public One getPoint() { return point; }
    public void setPoint(One point) { this.point = point; }

    public One getAir() { return air; }
    public void setAir(One air) { this.air = air; }
}