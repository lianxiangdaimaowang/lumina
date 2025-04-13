package com.lianxiangdaimaowang.lumina.controller;

import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleError(WebRequest webRequest, HttpServletResponse response) {
        Map<String, Object> errorMap = new HashMap<>();
        
        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        
        errorMap.put("status", status.value());
        errorMap.put("code", status.value());
        
        if (status == HttpStatus.NOT_FOUND) {
            errorMap.put("message", "请求的资源不存在");
        } else if (status == HttpStatus.FORBIDDEN) {
            errorMap.put("message", "您没有权限访问此资源");
        } else if (status == HttpStatus.UNAUTHORIZED) {
            errorMap.put("message", "请先登录后再访问此资源");
        } else if (status == HttpStatus.BAD_REQUEST) {
            errorMap.put("message", "无效的请求参数");
        } else {
            errorMap.put("message", "服务器发生错误");
        }
        
        return new ResponseEntity<>(errorMap, status);
    }
} 