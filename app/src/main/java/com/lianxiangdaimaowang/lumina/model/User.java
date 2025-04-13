package com.lianxiangdaimaowang.lumina.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Expose;

/**
 * 用户模型类
 */
public class User {
    @Expose
    @SerializedName("id")
    private Long id;
    
    @Expose
    @SerializedName("username")
    private String username;
    
    @Expose
    @SerializedName("password")
    private String password;
    
    @Expose
    @SerializedName("email")
    private String email;
    
    @Expose
    @SerializedName("avatar")
    private String avatar;
    
    @Expose
    @SerializedName("studentType")
    private String studentType;
    
    @Expose
    @SerializedName("status")
    private Integer status;
    
    private String profileImageUrl;
    private int points;  // 虚拟积分，用于社区打赏
    private List<String> favoriteSubjects;
    private List<String> favoriteNotes;  // 收藏的笔记ID列表

    // 默认构造函数
    public User() {
        this.favoriteSubjects = new ArrayList<>();
        this.favoriteNotes = new ArrayList<>();
        this.points = 0;
        this.status = 1;  // 设置默认状态为1（正常）
    }

    // 带参数构造函数
    public User(String username, String password, String email) {
        this();  // 调用默认构造函数
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getter和Setter
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getStudentType() {
        return studentType;
    }
    
    public void setStudentType(String studentType) {
        this.studentType = studentType;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
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