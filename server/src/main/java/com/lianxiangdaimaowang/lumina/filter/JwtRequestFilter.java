package com.lianxiangdaimaowang.lumina.filter;

import com.lianxiangdaimaowang.lumina.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class JwtRequestFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    
    // 允许不带令牌访问的路径列表
    private final List<String> publicPaths = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register", 
            "/api/auth/refresh",
            "/api/health",
            "/api/server/info",
            "/",
            "/index.html"
    );

    // 允许不带令牌访问的路径前缀
    private final List<String> publicPathPrefixes = Arrays.asList(
            "/static/",
            "/swagger-ui",
            "/webjars/",
            "/v2/api-docs",
            "/v3/api-docs",
            "/swagger-resources"
    );

    public JwtRequestFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        
        // 对于完全匹配的公共API路径，不应用过滤器
        boolean isPublicPath = publicPaths.contains(path);
        
        // 对于以特定前缀开头的路径，不应用过滤器
        boolean hasPublicPrefix = false;
        for (String prefix : publicPathPrefixes) {
            if (path.startsWith(prefix)) {
                hasPublicPrefix = true;
                break;
            }
        }
        
        boolean shouldSkip = isPublicPath || hasPublicPrefix;
        
        if (shouldSkip) {
            logger.info("跳过JWT过滤器: " + path);
        } else {
            logger.info("应用JWT过滤器: " + path);
        }
        
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();
        
        logger.info("JWT过滤器处理请求: " + requestURI + ", Authorization头: " + 
                (authorizationHeader != null ? 
                (authorizationHeader.length() > 15 ? 
                 authorizationHeader.substring(0, 15) + "..." : authorizationHeader) : "null"));
        
        String username = null;
        String jwt = null;

        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                username = jwtUtil.extractUsername(jwt);
                logger.info("从JWT令牌提取的用户名: " + (username != null ? username : "未识别") + 
                           ", 令牌前15个字符: " + (jwt.length() > 15 ? jwt.substring(0, 15) + "..." : jwt));
                
                if (username == null) {
                    logger.error("无法从令牌中提取用户名，令牌可能无效");
                }
            } else {
                logger.info("JWT过滤器处理请求: " + requestURI + ", 未提供认证令牌或格式不正确");
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.info("尝试加载用户详情: " + username);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.info("用户详情加载成功: " + username);

                boolean isValid = jwtUtil.validateToken(jwt, userDetails);
                logger.info("JWT令牌验证结果: " + (isValid ? "有效" : "无效") + " 用户: " + username);
                
                if (isValid) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    logger.info("用户 " + username + " 成功通过认证，权限: " + userDetails.getAuthorities());
                } else {
                    logger.error("用户 " + username + " 的令牌验证失败");
                }
            }
        } catch (Exception e) {
            logger.error("JWT认证过程中发生错误: " + e.getMessage(), e);
        }
        
        chain.doFilter(request, response);
    }
} 