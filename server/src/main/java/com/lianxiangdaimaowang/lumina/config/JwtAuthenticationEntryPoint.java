package com.lianxiangdaimaowang.lumina.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = Logger.getLogger(JwtAuthenticationEntryPoint.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        logger.warning("未授权访问: " + request.getRequestURI() + ", 错误: " + authException.getMessage());
        
        // 设置错误状态和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 构建错误信息
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("code", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("message", "访问此资源需要登录");
        errorDetails.put("path", request.getRequestURI());
        
        // 写入错误响应
        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }
} 