package com.lianxiangdaimaowang.lumina.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * 评论模型类
 */
public class Comment {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("username") 
    private String username;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("createTime")
    private Date createTime;
    
    @SerializedName("postId")
    private Long postId;
    
    // 默认构造函数
    public Comment() {
        this.createTime = new Date();
    }
    
    // 带参数构造函数
    public Comment(String userId, String username, String content, Long postId) {
        this();
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.postId = postId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Long getPostId() {
        return postId;
    }
    
    public void setPostId(Long postId) {
        this.postId = postId;
    }
} 