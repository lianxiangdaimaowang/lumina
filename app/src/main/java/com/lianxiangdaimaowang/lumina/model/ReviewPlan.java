package com.lianxiangdaimaowang.lumina.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 复习计划模型
 */
public class ReviewPlan {
    private String id;
    private String noteId;
    private String userId;
    private String noteTitle;
    private String noteContent;
    private List<Date> reviewDates;
    private int currentStage;
    private Date lastReviewDate;
    private Date nextReviewDate;
    private boolean isCompleted;

    public ReviewPlan() {
        this.id = UUID.randomUUID().toString();
        this.reviewDates = new ArrayList<>();
        this.currentStage = 0;
        this.isCompleted = false;
    }

    public ReviewPlan(String noteId, String noteTitle, String noteContent) {
        this();
        this.noteId = noteId;
        this.noteTitle = noteTitle;
        this.noteContent = noteContent;
        this.calculateNextReviewDate();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public List<Date> getReviewDates() {
        return reviewDates;
    }

    public void setReviewDates(List<Date> reviewDates) {
        this.reviewDates = reviewDates;
    }

    public void addReviewDate(Date reviewDate) {
        if (this.reviewDates == null) {
            this.reviewDates = new ArrayList<>();
        }
        this.reviewDates.add(reviewDate);
        this.lastReviewDate = reviewDate;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    public Date getLastReviewDate() {
        return lastReviewDate;
    }

    public void setLastReviewDate(Date lastReviewDate) {
        this.lastReviewDate = lastReviewDate;
    }

    public Date getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(Date nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    /**
     * 完成当前阶段复习，并计算下一次复习日期
     */
    public void completeCurrentReview() {
        Date now = new Date();
        addReviewDate(now);
        currentStage++;
        calculateNextReviewDate();
        
        // 如果达到最大阶段，标记为已完成
        if (currentStage >= 5) {
            setCompleted(true);
        }
    }

    /**
     * 计算下一次复习日期
     * 使用艾宾浩斯遗忘曲线
     * 第1次：当天
     * 第2次：1天后
     * 第3次：2天后
     * 第4次：4天后
     * 第5次：7天后
     */
    private void calculateNextReviewDate() {
        Date now = new Date();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(now);
        
        switch (currentStage) {
            case 0: // 第一次复习就是当天
                break;
            case 1: // 第二次复习，1天后
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                break;
            case 2: // 第三次复习，2天后
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 2);
                break;
            case 3: // 第四次复习，4天后
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 4);
                break;
            case 4: // 第五次复习，7天后
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 7);
                break;
            default: // 已经完成所有复习
                calendar.add(java.util.Calendar.MONTH, 1); // 设为一个月后，实际上已经完成
                setCompleted(true);
                break;
        }
        
        nextReviewDate = calendar.getTime();
    }
} 