package com.lianxiangdaimaowang.lumina.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = Logger.getLogger(CustomAccessDeniedHandler.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        logger.warning("访问被拒绝: " + request.getRequestURI() + ", 错误: " + accessDeniedException.getMessage());
        
        // 设置错误状态和内容类型
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 构建错误信息
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpServletResponse.SC_FORBIDDEN);
        errorDetails.put("code", HttpServletResponse.SC_FORBIDDEN);
        errorDetails.put("message", "您没有权限访问此资源");
        errorDetails.put("path", request.getRequestURI());
        
        // 写入错误响应
        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }
} 