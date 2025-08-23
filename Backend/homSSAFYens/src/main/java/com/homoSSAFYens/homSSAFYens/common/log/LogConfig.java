package com.homoSSAFYens.homSSAFYens.common.log;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfig {

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }
}
