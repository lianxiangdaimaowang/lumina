package com.lianxiangdaimaowang.lumina.controller;

import com.lianxiangdaimaowang.lumina.entity.Note;
import com.lianxiangdaimaowang.lumina.entity.User;
import com.lianxiangdaimaowang.lumina.service.NoteService;
import com.lianxiangdaimaowang.lumina.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private static final Logger logger = Logger.getLogger(NoteController.class.getName());
    
    @Autowired
    private NoteService noteService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取当前用户的所有笔记
     */
    @GetMapping
    public ResponseEntity<?> getAllNotes(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.warning("获取笔记失败: 未提供认证信息");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "访问此资源需要登录");
                response.put("status", 401);
                return ResponseEntity.status(401).body(response);
            }
            
            String username = authentication.getName();
            logger.info("获取用户笔记: " + username);
            
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warning("获取笔记失败: 用户不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            List<Note> notes = noteService.getNotesByUserId(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("notes", notes);
            response.put("count", notes.size());
            response.put("status", 200);
            response.put("message", "获取笔记成功");
            
            logger.info("成功获取用户笔记: " + username + ", 笔记数量: " + notes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("获取笔记时出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "获取笔记时出错: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 创建新笔记
     */
    @PostMapping
    public ResponseEntity<?> createNote(@RequestBody Note note, Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.warning("创建笔记失败: 未提供认证信息");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "访问此资源需要登录");
                response.put("status", 401);
                return ResponseEntity.status(401).body(response);
            }
            
            String username = authentication.getName();
            logger.info("为用户创建笔记: " + username);
            
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warning("创建笔记失败: 用户不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            // 设置笔记所有者
            note.setUserId(user.getId());
            
            // 保存笔记
            Note savedNote = noteService.saveNote(note);
            
            Map<String, Object> response = new HashMap<>();
            response.put("note", savedNote);
            response.put("status", 200);
            response.put("message", "笔记创建成功");
            
            logger.info("成功创建笔记: ID=" + savedNote.getId() + ", 用户=" + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("创建笔记时出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "创建笔记时出错: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取指定ID的笔记
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getNoteById(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.warning("获取笔记失败: 未提供认证信息");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "访问此资源需要登录");
                response.put("status", 401);
                return ResponseEntity.status(401).body(response);
            }
            
            String username = authentication.getName();
            logger.info("获取笔记详情: ID=" + id + ", 用户=" + username);
            
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warning("获取笔记失败: 用户不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            Optional<Note> noteOpt = noteService.getNoteById(id);
            if (!noteOpt.isPresent()) {
                logger.warning("获取笔记失败: 笔记不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "笔记不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            Note note = noteOpt.get();
            
            // 验证笔记所有权
            if (!note.getUserId().equals(user.getId())) {
                logger.warning("获取笔记失败: 无权访问此笔记");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "无权访问此笔记");
                response.put("status", 403);
                return ResponseEntity.status(403).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("note", note);
            response.put("status", 200);
            response.put("message", "获取笔记成功");
            
            logger.info("成功获取笔记详情: ID=" + id + ", 用户=" + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("获取笔记详情时出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "获取笔记详情时出错: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 更新指定ID的笔记
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable Long id, @RequestBody Note note, Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.warning("更新笔记失败: 未提供认证信息");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "访问此资源需要登录");
                response.put("status", 401);
                return ResponseEntity.status(401).body(response);
            }
            
            String username = authentication.getName();
            logger.info("更新笔记: ID=" + id + ", 用户=" + username);
            
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warning("更新笔记失败: 用户不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            Optional<Note> existingNoteOpt = noteService.getNoteById(id);
            if (!existingNoteOpt.isPresent()) {
                logger.warning("更新笔记失败: 笔记不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "笔记不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            Note existingNote = existingNoteOpt.get();
            
            // 验证笔记所有权
            if (!existingNote.getUserId().equals(user.getId())) {
                logger.warning("更新笔记失败: 无权更新此笔记");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "无权更新此笔记");
                response.put("status", 403);
                return ResponseEntity.status(403).body(response);
            }
            
            // 设置ID和用户ID，确保这些字段不被修改
            note.setId(id);
            note.setUserId(user.getId());
            
            // 保存更新后的笔记
            Note updatedNote = noteService.saveNote(note);
            
            Map<String, Object> response = new HashMap<>();
            response.put("note", updatedNote);
            response.put("status", 200);
            response.put("message", "笔记更新成功");
            
            logger.info("成功更新笔记: ID=" + id + ", 用户=" + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("更新笔记时出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "更新笔记时出错: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 删除指定ID的笔记
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                logger.warning("删除笔记失败: 未提供认证信息");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "访问此资源需要登录");
                response.put("status", 401);
                return ResponseEntity.status(401).body(response);
            }
            
            String username = authentication.getName();
            logger.info("删除笔记: ID=" + id + ", 用户=" + username);
            
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warning("删除笔记失败: 用户不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "用户不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            Optional<Note> noteOpt = noteService.getNoteById(id);
            if (!noteOpt.isPresent()) {
                logger.warning("删除笔记失败: 笔记不存在");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "笔记不存在");
                response.put("status", 404);
                return ResponseEntity.status(404).body(response);
            }
            
            Note note = noteOpt.get();
            
            // 验证笔记所有权
            if (!note.getUserId().equals(user.getId())) {
                logger.warning("删除笔记失败: 无权删除此笔记");
                Map<String, Object> response = new HashMap<>();
                response.put("message", "无权删除此笔记");
                response.put("status", 403);
                return ResponseEntity.status(403).body(response);
            }
            
            // 删除笔记
            noteService.deleteNote(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("message", "笔记删除成功");
            
            logger.info("成功删除笔记: ID=" + id + ", 用户=" + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("删除笔记时出错: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "删除笔记时出错: " + e.getMessage());
            response.put("status", 500);
            return ResponseEntity.status(500).body(response);
        }
    }
} 