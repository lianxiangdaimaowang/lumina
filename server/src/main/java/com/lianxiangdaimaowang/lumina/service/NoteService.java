package com.lianxiangdaimaowang.lumina.service;

import com.lianxiangdaimaowang.lumina.entity.Note;
import com.lianxiangdaimaowang.lumina.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class NoteService {
    private static final Logger logger = Logger.getLogger(NoteService.class.getName());

    @Autowired
    private NoteRepository noteRepository;

    /**
     * 保存笔记
     */
    public Note saveNote(Note note) {
        return noteRepository.save(note);
    }

    /**
     * 根据ID获取笔记
     */
    public Optional<Note> getNoteById(Long id) {
        return noteRepository.findById(id);
    }

    /**
     * 获取用户的所有笔记（默认只返回状态为正常的笔记）
     */
    public List<Note> getNotesByUserId(Long userId) {
        return noteRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, 1);
    }

    /**
     * 根据状态获取用户的笔记
     */
    public List<Note> getNotesByUserIdAndStatus(Long userId, Integer status) {
        return noteRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, status);
    }

    /**
     * 获取用户的所有笔记，包括已删除的
     */
    public List<Note> getAllNotesByUserId(Long userId) {
        return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 删除笔记（逻辑删除，将状态改为0）
     */
    public void deleteNote(Long id) {
        Optional<Note> noteOpt = noteRepository.findById(id);
        if (noteOpt.isPresent()) {
            Note note = noteOpt.get();
            note.setStatus(0); // 0表示已删除
            noteRepository.save(note);
            logger.info("笔记已逻辑删除: ID=" + id);
        } else {
            logger.warning("删除笔记失败: 笔记不存在, ID=" + id);
        }
    }

    /**
     * 物理删除笔记
     */
    public void hardDeleteNote(Long id) {
        noteRepository.deleteById(id);
        logger.info("笔记已物理删除: ID=" + id);
    }
} 