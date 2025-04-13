package com.lianxiangdaimaowang.lumina.repository;

import com.lianxiangdaimaowang.lumina.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // 根据点赞数降序获取帖子
    List<Post> findAllByOrderByLikeCountDesc();
    
    // 获取热门帖子（点赞数前N的帖子）
    @Query(value = "SELECT * FROM posts ORDER BY like_count DESC LIMIT :limit", nativeQuery = true)
    List<Post> findTopPostsByLikeCount(@Param("limit") int limit);
} 