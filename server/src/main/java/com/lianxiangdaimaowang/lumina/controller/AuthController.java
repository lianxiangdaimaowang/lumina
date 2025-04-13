package com.lianxiangdaimaowang.lumina.controller;

import com.lianxiangdaimaowang.lumina.entity.User;
import com.lianxiangdaimaowang.lumina.service.UserService;
import com.lianxiangdaimaowang.lumina.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = Logger.getLogger(AuthController.class.getName());
    
    // 状态码常量
    private static final int STATUS_SUCCESS = 200;
    private static final int STATUS_ERROR = 500;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        try {
            String username = registerRequest.get("username");
            String password = registerRequest.get("password");
            String email = registerRequest.get("email");
            String status = registerRequest.get("status");
            
            logger.info("接收到注册请求: " + username);
            
            // 验证请求数据
            if (username == null || username.trim().isEmpty()) {
                logger.warning("注册失败: 用户名为空");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户名不能为空");
                response.put("status", STATUS_ERROR);
                return ResponseEntity.badRequest().body(response);
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.warning("注册失败: 密码为空");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "密码不能为空");
                response.put("status", STATUS_ERROR);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建用户对象
            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(password.trim());
            user.setEmail(email != null ? email.trim() : username.trim() + "@example.com");
            user.setStatus(status != null ? Integer.parseInt(status) : 1);
            
            // 处理注册
            User registeredUser = userService.register(user);
            logger.info("用户注册成功: " + username);
            
            // 返回成功响应，不包括密码
            registeredUser.setPassword(null);
            Map<String, Object> response = new HashMap<>();
            response.put("user", registeredUser);
            response.put("message", "注册成功");
            response.put("status", STATUS_SUCCESS);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("注册错误: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", STATUS_ERROR);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            logger.info("接收到登录请求: " + loginRequest);
            
            String username = loginRequest.get("username");
            if (username == null || username.isEmpty()) {
                username = loginRequest.get("email");
                logger.info("使用邮箱作为用户名: " + username);
            }
            
            String password = loginRequest.get("password");
            
            if (username == null || password == null) {
                logger.warning("登录失败: 用户名/邮箱和密码是必需的");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户名/邮箱和密码是必需的");
                response.put("status", STATUS_ERROR);
                return ResponseEntity.badRequest().body(response);
            }
            
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            password
                    )
            );
            final UserDetails userDetails = userService.loadUserByUsername(username);
            final String jwt = jwtUtil.generateToken(userDetails);

            logger.info("用户登录成功: " + username);
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("status", STATUS_SUCCESS);
            response.put("message", "登录成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("登录错误: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户名或密码无效: " + e.getMessage());
            response.put("status", STATUS_ERROR);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 刷新令牌端点
     * 允许客户端使用当前有效的令牌获取新令牌
     */
    @GetMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("接收到刷新令牌请求, 认证头: " + 
                       (authHeader != null ? 
                       (authHeader.length() > 15 ? authHeader.substring(0, 15) + "..." : authHeader) : "null"));
            
            // 检查认证头是否存在
            if (authHeader == null || authHeader.isEmpty()) {
                logger.warning("刷新令牌失败: 请求中没有认证头");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "认证头是必需的");
                response.put("status", STATUS_ERROR);
                return ResponseEntity.status(401).body(response);
            }
            
            // 提取当前令牌
            String token = authHeader;
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            logger.info("刷新令牌: " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
            
            // 验证当前令牌
            try {
                String username = jwtUtil.extractUsername(token);
                if (username == null) {
                    logger.warning("刷新令牌失败: 无法从令牌中提取用户名");
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "无效的令牌");
                    response.put("status", STATUS_ERROR);
                    return ResponseEntity.status(401).body(response);
                }
                
                // 检查令牌是否过期
                if (jwtUtil.isTokenExpired(token)) {
                    logger.warning("刷新令牌失败: 令牌已过期");
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "令牌已过期");
                    response.put("status", STATUS_ERROR);
                    return ResponseEntity.status(401).body(response);
                }
                
                logger.info("令牌验证通过，准备为用户 " + username + " 生成新令牌");
                
                try {
                    // 加载用户信息
                    final UserDetails userDetails = userService.loadUserByUsername(username);
                    logger.info("成功加载用户详情: " + username);
                    
                    // 生成新令牌
                    final String newToken = jwtUtil.generateToken(userDetails);
                    if (newToken == null || newToken.isEmpty()) {
                        logger.severe("生成新令牌失败: 令牌为空");
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "生成新令牌失败");
                        response.put("status", STATUS_ERROR);
                        return ResponseEntity.status(500).body(response);
                    }
                    
                    logger.info("用户刷新令牌成功: " + username + ", 新令牌前20个字符: " + 
                               (newToken.length() > 20 ? newToken.substring(0, 20) + "..." : newToken));
                    Map<String, Object> response = new HashMap<>();
                    response.put("token", newToken);
                    response.put("status", STATUS_SUCCESS);
                    response.put("message", "令牌刷新成功");
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    logger.severe("加载用户信息或生成新令牌时出错: " + e.getMessage());
                    e.printStackTrace();
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "服务器内部错误: " + e.getMessage());
                    response.put("status", STATUS_ERROR);
                    return ResponseEntity.status(500).body(response);
                }
            } catch (Exception e) {
                logger.severe("处理令牌时出错: " + e.getMessage());
                e.printStackTrace();
                Map<String, Object> response = new HashMap<>();
                response.put("message", "令牌处理失败: " + e.getMessage());
                response.put("status", STATUS_ERROR);
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            logger.severe("刷新令牌错误: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "令牌刷新失败: " + e.getMessage());
            response.put("status", STATUS_ERROR);
            return ResponseEntity.status(401).body(response);
        }
    }
} 