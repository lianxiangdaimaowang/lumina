package com.lianxiangdaimaowang.lumina.controller;

import com.lianxiangdaimaowang.lumina.entity.Favorite;
import com.lianxiangdaimaowang.lumina.entity.Post;
import com.lianxiangdaimaowang.lumina.entity.User;
import com.lianxiangdaimaowang.lumina.repository.FavoriteRepository;
import com.lianxiangdaimaowang.lumina.repository.PostRepository;
import com.lianxiangdaimaowang.lumina.repository.UserRepository;
import com.lianxiangdaimaowang.lumina.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    /**
     * 获取当前登录用户的个人信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User user = userService.getCurrentUser();
            if (user != null) {
                // 移除敏感信息
                user.setPassword(null);
                return ResponseEntity.ok(user);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User not found");
                response.put("status", "error");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 更新当前用户的个人信息
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody User updatedUser) {
        try {
            User user = userService.getCurrentUser();
            if (user != null) {
                // 只允许更新部分字段
                if (updatedUser.getUsername() != null) {
                    user.setUsername(updatedUser.getUsername());
                }
                if (updatedUser.getEmail() != null) {
                    user.setEmail(updatedUser.getEmail());
                }
                if (updatedUser.getAvatar() != null) {
                    user.setAvatar(updatedUser.getAvatar());
                }
                
                // 保存更新
                userRepository.save(user);
                
                // 移除敏感信息
                user.setPassword(null);
                return ResponseEntity.ok(user);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User not found");
                response.put("status", "error");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取用户收藏的帖子
     */
    @GetMapping("/{userId}/favorites")
    public ResponseEntity<?> getUserFavorites(@PathVariable String userId) {
        try {
            // 验证当前用户是否已登录
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 获取当前用户
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 只允许查看自己的收藏
            if (!currentUser.getId().toString().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "不允许查看其他用户的收藏");
                response.put("status", "error");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // 查询用户的收藏
            List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            
            // 提取帖子信息
            List<Post> favoritePosts = favorites.stream()
                    .map(Favorite::getPost)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(favoritePosts);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 