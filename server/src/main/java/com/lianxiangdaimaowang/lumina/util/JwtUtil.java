package com.lianxiangdaimaowang.lumina.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JwtUtil {
    private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());
    
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "从令牌中提取用户名时出错: " + e.getMessage(), e);
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "从令牌中提取过期时间时出错: " + e.getMessage(), e);
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    private Claims extractAllClaims(String token) {
        try {
            logger.info("解析JWT令牌，密钥长度: " + (secret != null ? secret.length() : 0));
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            logger.log(Level.WARNING, "JWT令牌已过期: " + e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            logger.log(Level.SEVERE, "不支持的JWT令牌: " + e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            logger.log(Level.SEVERE, "JWT令牌格式错误: " + e.getMessage());
            return null;
        } catch (SignatureException e) {
            logger.log(Level.SEVERE, "JWT签名验证失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "解析JWT令牌时出错: " + e.getMessage(), e);
            return null;
        }
    }

    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            boolean isExpired = expiration != null && expiration.before(new Date());
            logger.info("检查令牌过期: 过期时间=" + expiration + ", 当前时间=" + new Date() + ", 是否过期=" + isExpired);
            return isExpired;
        } catch (Exception e) {
            logger.log(Level.WARNING, "检查令牌过期时发生错误: " + e.getMessage(), e);
            return true;
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String token = createToken(claims, userDetails.getUsername());
        logger.info("为用户 " + userDetails.getUsername() + " 生成新令牌: " + 
                   (token != null && token.length() > 20 ? token.substring(0, 20) + "..." : token));
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // 确保默认过期时间（单位：毫秒）
        // 之前配置的是秒，现在确保是毫秒，并且提供足够长的默认值（7天）
        long expirationTimeInMillis;
        if (expiration != null) {
            // 将配置的秒转换为毫秒
            expirationTimeInMillis = expiration * 1000;
        } else {
            // 默认为7天（604800000毫秒）
            expirationTimeInMillis = 604800000;
        }
        
        Date now = new Date(System.currentTimeMillis());
        Date expiryDate = new Date(now.getTime() + expirationTimeInMillis);
        
        logger.info("创建JWT令牌，用户: " + subject + ", 过期时间: " + expiryDate + 
                   ", 配置过期时间(秒): " + expiration + 
                   ", 实际过期时间(毫秒): " + expirationTimeInMillis);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            if (username == null) {
                logger.warning("令牌验证失败 - 无法提取用户名");
                return false;
            }
            
            boolean usernameMatches = username.equals(userDetails.getUsername());
            boolean notExpired = !isTokenExpired(token);
            
            logger.info("验证令牌: 令牌用户名=" + username + ", 实际用户名=" + userDetails.getUsername() + 
                       ", 用户名匹配=" + usernameMatches + ", 未过期=" + notExpired);
            
            boolean isValid = usernameMatches && notExpired;
            if (!isValid) {
                logger.warning("令牌验证失败 - 用户名: " + username + ", 已过期: " + isTokenExpired(token));
            }
            return isValid;
        } catch (Exception e) {
            logger.log(Level.WARNING, "验证令牌时发生错误: " + e.getMessage(), e);
            return false;
        }
    }
} 