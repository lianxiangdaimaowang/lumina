package com.lianxiangdaimaowang.lumina.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {
    
    // 提供 JSON API
    @GetMapping("/api/server/info")
    public Map<String, Object> getServerInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Lumina服务");
        response.put("status", "运行中");
        response.put("version", "1.0");
        response.put("message", "欢迎访问Lumina API服务");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        Map<String, String> apiEndpoints = new HashMap<>();
        apiEndpoints.put("health", "/api/health");
        apiEndpoints.put("auth", "/api/auth");
        
        response.put("endpoints", apiEndpoints);
        return response;
    }
} 