package com.lianxiangdaimaowang.lumina.repository;

import com.lianxiangdaimaowang.lumina.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Note> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, Integer status);
} 