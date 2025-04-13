package com.lianxiangdaimaowang.lumina.repository;

import com.lianxiangdaimaowang.lumina.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);
    List<Favorite> findByPostId(Long postId);
    Favorite findByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
} 