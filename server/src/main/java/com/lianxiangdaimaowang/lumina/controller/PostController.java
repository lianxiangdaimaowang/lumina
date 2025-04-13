package com.lianxiangdaimaowang.lumina.controller;

import com.lianxiangdaimaowang.lumina.entity.Favorite;
import com.lianxiangdaimaowang.lumina.entity.Like;
import com.lianxiangdaimaowang.lumina.entity.Post;
import com.lianxiangdaimaowang.lumina.entity.User;
import com.lianxiangdaimaowang.lumina.repository.FavoriteRepository;
import com.lianxiangdaimaowang.lumina.repository.LikeRepository;
import com.lianxiangdaimaowang.lumina.repository.PostRepository;
import com.lianxiangdaimaowang.lumina.repository.UserRepository;
import com.lianxiangdaimaowang.lumina.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LikeRepository likeRepository;
    
    @Autowired
    private FavoriteRepository favoriteRepository;

    // 获取所有帖子
    @GetMapping
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }
    
    // 获取热门帖子（按点赞数排序，取前N个）
    @GetMapping("/hot")
    public List<Post> getHotPosts(@RequestParam(defaultValue = "3") int limit) {
        return postRepository.findTopPostsByLikeCount(limit);
    }
    
    // 获取单个帖子
    @GetMapping("/{id}")
    public ResponseEntity<?> getPost(@PathVariable Long id) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isPresent()) {
            return ResponseEntity.ok(post.get());
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "帖子不存在");
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    // 创建帖子
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Post post) {
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            if (user != null) {
                post.setUser(user);
                post.setViewCount(0);
                post.setLikeCount(0);
                post.setCommentCount(0);
                
                Post savedPost = postRepository.save(post);
                return ResponseEntity.ok(savedPost);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户未找到");
                response.put("status", "error");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 点赞帖子
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable("id") Long postId, @RequestBody Map<String, String> params) {
        try {
            // 获取当前用户
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 查找帖子
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (!optionalPost.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Post post = optionalPost.get();
            
            // 检查是否已点赞
            Like existingLike = likeRepository.findByUserIdAndPostId(currentUser.getId(), postId);
            if (existingLike != null) {
                // 已点赞，返回成功
                return ResponseEntity.ok().build();
            }
            
            // 创建点赞记录
            Like like = new Like();
            like.setUser(currentUser);
            like.setPost(post);
            likeRepository.save(like);
            
            // 更新帖子点赞数
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 取消点赞帖子
    @PostMapping("/{id}/unlike")
    public ResponseEntity<?> unlikePost(@PathVariable("id") Long postId, @RequestBody Map<String, String> params) {
        try {
            // 获取当前用户
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 查找帖子
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (!optionalPost.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Post post = optionalPost.get();
            
            // 查找点赞记录
            Like existingLike = likeRepository.findByUserIdAndPostId(currentUser.getId(), postId);
            if (existingLike == null) {
                // 未点赞，返回成功
                return ResponseEntity.ok().build();
            }
            
            // 删除点赞记录
            likeRepository.delete(existingLike);
            
            // 更新帖子点赞数
            if (post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                postRepository.save(post);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 收藏帖子
    @PostMapping("/{id}/favorite")
    public ResponseEntity<?> favoritePost(@PathVariable("id") Long postId, @RequestBody Map<String, String> params) {
        try {
            // 获取当前用户
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 查找帖子
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (!optionalPost.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Post post = optionalPost.get();
            
            // 检查是否已收藏
            Favorite existingFavorite = favoriteRepository.findByUserIdAndPostId(currentUser.getId(), postId);
            if (existingFavorite != null) {
                // 已收藏，返回成功
                return ResponseEntity.ok().build();
            }
            
            // 创建收藏记录
            Favorite favorite = new Favorite();
            favorite.setUser(currentUser);
            favorite.setPost(post);
            favoriteRepository.save(favorite);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 取消收藏帖子
    @PostMapping("/{id}/unfavorite")
    public ResponseEntity<?> unfavoritePost(@PathVariable("id") Long postId, @RequestBody Map<String, String> params) {
        try {
            // 获取当前用户
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 查找帖子
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (!optionalPost.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // 查找收藏记录
            Favorite existingFavorite = favoriteRepository.findByUserIdAndPostId(currentUser.getId(), postId);
            if (existingFavorite == null) {
                // 未收藏，返回成功
                return ResponseEntity.ok().build();
            }
            
            // 删除收藏记录
            favoriteRepository.delete(existingFavorite);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // 更新帖子
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody Post updatedPost) {
        try {
            Optional<Post> optionalPost = postRepository.findById(id);
            if (optionalPost.isPresent()) {
                Post post = optionalPost.get();
                
                // 检查是否是帖子作者
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                User user = userRepository.findByUsername(username);
                
                if (user != null && post.getUser().getId().equals(user.getId())) {
                    post.setTitle(updatedPost.getTitle());
                    post.setContent(updatedPost.getContent());
                    
                    Post savedPost = postRepository.save(post);
                    return ResponseEntity.ok(savedPost);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "您没有权限编辑此帖子");
                    response.put("status", "error");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "帖子不存在");
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
    
    // 删除帖子
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            Optional<Post> optionalPost = postRepository.findById(id);
            if (optionalPost.isPresent()) {
                Post post = optionalPost.get();
                
                // 检查是否是帖子作者
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                User user = userRepository.findByUsername(username);
                
                if (user != null && post.getUser().getId().equals(user.getId())) {
                    postRepository.delete(post);
                    return ResponseEntity.ok().build();
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "您没有权限删除此帖子");
                    response.put("status", "error");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "帖子不存在");
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
} 