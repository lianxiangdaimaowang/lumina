package com.lianxiangdaimaowang.lumina.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户模型类
 */
public class User {
    private String id;
    private String username;
    private String email;
    private String profileImageUrl;
    private int points;  // 虚拟积分，用于社区打赏
    private List<String> favoriteSubjects;
    private List<String> favoriteNotes;  // 收藏的笔记ID列表

    // 默认构造函数
    public User() {
        this.favoriteSubjects = new ArrayList<>();
        this.favoriteNotes = new ArrayList<>();
        this.points = 0;
    }

    // 参数构造函数
    public User(String id, String username, String email) {
        this();
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public List<String> getFavoriteSubjects() {
        return favoriteSubjects;
    }

    public void setFavoriteSubjects(List<String> favoriteSubjects) {
        this.favoriteSubjects = favoriteSubjects;
    }

    public List<String> getFavoriteNotes() {
        return favoriteNotes;
    }

    public void setFavoriteNotes(List<String> favoriteNotes) {
        this.favoriteNotes = favoriteNotes;
    }

    // 用户积分操作
    public void addPoints(int points) {
        this.points += points;
    }

    public boolean deductPoints(int points) {
        if (this.points >= points) {
            this.points -= points;
            return true;
        }
        return false;
    }

    // 收藏笔记
    public void addFavoriteNote(String noteId) {
        if (!this.favoriteNotes.contains(noteId)) {
            this.favoriteNotes.add(noteId);
        }
    }

    // 取消收藏笔记
    public void removeFavoriteNote(String noteId) {
        this.favoriteNotes.remove(noteId);
    }

    // 添加喜欢的学科
    public void addFavoriteSubject(String subject) {
        if (!this.favoriteSubjects.contains(subject)) {
            this.favoriteSubjects.add(subject);
        }
    }

    // 移除喜欢的学科
    public void removeFavoriteSubject(String subject) {
        this.favoriteSubjects.remove(subject);
    }
} 