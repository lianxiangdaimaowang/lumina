package com.lianxiangdaimaowang.lumina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LuminaApplication {
    public static void main(String[] args) {
        SpringApplication.run(LuminaApplication.class, args);
    }
} 