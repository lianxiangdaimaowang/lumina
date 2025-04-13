package com.lianxiangdaimaowang.lumina.model;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.lianxiangdaimaowang.lumina.utils.UserIdTypeAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 笔记数据模型
 */
public class Note {
    @SerializedName("id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("subject")
    private String subject;
    
    @SerializedName("categoryId")
    private Integer categoryId;
    
    @SerializedName("tags")
    private List<String> tags;
    
    @SerializedName("userId")
    @JsonAdapter(UserIdTypeAdapter.class)
    private String userId;
    
    @SerializedName("createdDate")
    private Date createdDate;
    
    @SerializedName("lastModifiedDate")
    private Date lastModifiedDate;
    
    @SerializedName("shared")
    private boolean isShared;
    
    @SerializedName("attachmentPaths")
    private List<String> attachmentPaths;

    public Note() {
        // 不再自动生成UUID作为ID，让服务器自动生成数字ID
        // this.id = UUID.randomUUID().toString();
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
    
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
        // 同时更新subject字段
        setSubjectFromCategoryId(categoryId);
        this.lastModifiedDate = new Date();
    }
    
    /**
     * 根据服务器返回的categoryId设置笔记科目
     * @param categoryId 服务器返回的分类ID
     */
    public void setSubjectFromCategoryId(Object categoryId) {
        if (categoryId == null) {
            if (this.subject == null || this.subject.isEmpty()) {
                this.subject = "其他";
            }
            return;
        }
        
        try {
            // 获取原始科目，以便日志记录
            String originalSubject = this.subject;
            
            // 使用SyncUtils规范化categoryId
            int id = com.lianxiangdaimaowang.lumina.sync.SyncUtils.normalizeCategoryId(categoryId);
            
            // 根据ID映射到科目名称
            switch (id) {
                case 1: this.subject = "语文"; break;
                case 2: this.subject = "数学"; break;
                case 3: this.subject = "英语"; break;
                case 4: this.subject = "物理"; break;
                case 5: this.subject = "化学"; break;
                case 6: this.subject = "生物"; break;
                case 7: this.subject = "历史"; break;
                case 8: this.subject = "地理"; break;
                case 9: this.subject = "政治"; break;
                case 10: this.subject = "其他"; break; // 明确处理10为"其他"
                default: this.subject = "其他"; break;
            }
            
            // 记录科目变化日志
            if ((originalSubject != null && !originalSubject.equals(this.subject)) || originalSubject == null) {
                android.util.Log.d("Note", "科目已从 '" + originalSubject + "' 更新为 '" + 
                          this.subject + "' (categoryId: " + categoryId + ")");
            }
        } catch (Exception e) {
            // 如果处理出错，仅在未设置科目时默认设置为"其他"
            if (this.subject == null || this.subject.isEmpty()) {
                this.subject = "其他";
                android.util.Log.e("Note", "解析categoryId出错: " + e.getMessage() + ", 设置默认科目为'其他'");
            } else {
                android.util.Log.e("Note", "解析categoryId出错: " + e.getMessage() + ", 保留原科目: " + this.subject);
            }
        }
        
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
        // 在获取userId时处理可能包含小数点的情况
        if (userId != null && userId.contains(".")) {
            try {
                // 尝试转换为整数
                double doubleValue = Double.parseDouble(userId);
                long longValue = (long) doubleValue;
                return String.valueOf(longValue);
            } catch (NumberFormatException e) {
                // 如果转换失败，尝试截取小数点前的部分
                try {
                    return userId.substring(0, userId.indexOf('.'));
                } catch (Exception ex) {
                    // 如果截取也失败，返回原始值
                }
            }
        }
        return userId;
    }

    public void setUserId(String userId) {
        // 处理浮点数格式
        if (userId != null && userId.contains(".")) {
            try {
                // 尝试转换为整数
                double doubleValue = Double.parseDouble(userId);
                long longValue = (long) doubleValue;
                this.userId = String.valueOf(longValue);
                return;
            } catch (NumberFormatException e) {
                // 如果转换失败，尝试截取小数点前的部分
                try {
                    this.userId = userId.substring(0, userId.indexOf('.'));
                    return;
                } catch (Exception ex) {
                    // 如果截取也失败，使用原始值
                }
            }
        }
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
    
    /**
     * 根据科目名称获取对应的categoryId
     * @return 科目对应的分类ID，如果没有匹配则返回null
     */
    public Integer getCategoryIdFromSubject() {
        if (this.subject == null || this.subject.isEmpty()) {
            return null;
        }
        
        switch (this.subject) {
            case "语文": return 1;
            case "数学": return 2;
            case "英语": return 3;
            case "物理": return 4;
            case "化学": return 5;
            case "生物": return 6;
            case "历史": return 7;
            case "地理": return 8;
            case "政治": return 9;
            case "其他": return 10; // 为"其他"科目分配一个有效的ID
            default: return 10; // 默认情况也使用"其他"的ID
        }
    }
}