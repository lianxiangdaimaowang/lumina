package com.lianxiangdaimaowang.lumina.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", "error");
        
        // 记录错误日志
        System.err.println("Global error: " + ex.getMessage());
        ex.printStackTrace();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    public final ResponseEntity<Object> handleAuthenticationExceptions(Exception ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invalid username or password");
        response.put("status", "error");
        
        // 记录认证错误
        System.err.println("Authentication error: " + ex.getMessage());
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public final ResponseEntity<Object> handleBadRequestExceptions(Exception ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", "error");
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
} 