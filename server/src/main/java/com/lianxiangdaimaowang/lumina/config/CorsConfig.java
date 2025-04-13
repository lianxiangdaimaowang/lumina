package com.lianxiangdaimaowang.lumina.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:*}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的域
        if ("*".equals(allowedOrigins)) {
            config.addAllowedOriginPattern("*");
        } else {
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                config.addAllowedOrigin(origin.trim());
            }
        }
        
        // 允许的HTTP方法
        if ("*".equals(allowedMethods)) {
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        } else {
            String[] methods = allowedMethods.split(",");
            for (String method : methods) {
                config.addAllowedMethod(method.trim());
            }
        }
        
        // 允许的头信息
        if ("*".equals(allowedHeaders)) {
            config.addAllowedHeader("*");
        } else {
            String[] headers = allowedHeaders.split(",");
            for (String header : headers) {
                config.addAllowedHeader(header.trim());
            }
        }
        
        // 允许携带凭证
        config.setAllowCredentials(true);
        
        // 预检请求的有效期
        config.setMaxAge(maxAge);
        
        // 对所有URL应用CORS配置
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 