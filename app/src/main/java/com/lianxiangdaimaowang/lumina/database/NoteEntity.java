package com.lianxiangdaimaowang.lumina.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 笔记实体类，用于本地数据库存储
 */
@Entity(tableName = "notes")
@TypeConverters({DateConverter.class, StringListConverter.class})
public class NoteEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String content;
    private String subject;
    private Date creationDate;
    private Date lastModifiedDate;
    private List<String> attachments;
    
    public NoteEntity() {
        this.creationDate = new Date();
        this.lastModifiedDate = new Date();
        this.attachments = new ArrayList<>();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public Date getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    public List<String> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
    
    public void updateLastModifiedDate() {
        this.lastModifiedDate = new Date();
    }
} 