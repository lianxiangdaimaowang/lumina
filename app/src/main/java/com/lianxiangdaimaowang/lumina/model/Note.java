package com.lianxiangdaimaowang.lumina.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 笔记数据模型
 */
public class Note {
    private String id;
    private String title;
    private String content;
    private String subject;
    private List<String> tags;
    private String userId;
    private Date createdDate;
    private Date lastModifiedDate;
    private boolean isShared;
    private List<String> attachmentPaths;

    public Note() {
        this.id = UUID.randomUUID().toString();
        this.createdDate = new Date();
        this.lastModifiedDate = new Date();
        this.tags = new ArrayList<>();
        this.attachmentPaths = new ArrayList<>();
        this.isShared = false;
    }

    public Note(String title, String content, String subject) {
        this();
        this.title = title;
        this.content = content;
        this.subject = subject;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.lastModifiedDate = new Date();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.lastModifiedDate = new Date();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
        this.lastModifiedDate = new Date();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        this.lastModifiedDate = new Date();
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
        this.lastModifiedDate = new Date();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
        this.lastModifiedDate = new Date();
    }

    public List<String> getAttachmentPaths() {
        return attachmentPaths;
    }

    public void setAttachmentPaths(List<String> attachmentPaths) {
        this.attachmentPaths = attachmentPaths;
        this.lastModifiedDate = new Date();
    }

    public void addAttachmentPath(String path) {
        if (this.attachmentPaths == null) {
            this.attachmentPaths = new ArrayList<>();
        }
        this.attachmentPaths.add(path);
        this.lastModifiedDate = new Date();
    }
}