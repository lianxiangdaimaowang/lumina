package com.lianxiangdaimaowang.lumina.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.model.ReviewPlan;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 本地数据管理类
 * 简单的本地数据存储实现，替代云服务
 */
public class LocalDataManager {
    private static final String TAG = "LocalDataManager";
    private static final String PREFS_NAME = "lumina_prefs";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_SIGNED_IN = "signed_in";
    public static final String KEY_PROVIDER = "login_provider";
    public static final String KEY_AVATAR_URL = "avatar_url";
    
    // 内存中缓存笔记数据
    private final Map<String, Note> notesCache = new HashMap<>();
    // 内存中缓存复习计划数据
    private final Map<String, ReviewPlan> reviewPlansCache = new HashMap<>();
    
    private static LocalDataManager instance;
    private final SharedPreferences prefs;
    private final File dataDir;
    
    private LocalDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // 确保用户ID存在
        if (!prefs.contains(KEY_USER_ID)) {
            prefs.edit().putString(KEY_USER_ID, UUID.randomUUID().toString()).apply();
        }
        
        // 创建数据目录
        dataDir = new File(context.getFilesDir(), "lumina_data");
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                Log.e(TAG, "无法创建数据目录");
            }
        }
        
        // 从本地存储加载数据
        loadDataFromLocal();
    }
    
    public static synchronized LocalDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDataManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 获取用户ID
     */
    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }
    
    /**
     * 获取用户名
     */
    public String getCurrentUsername() {
        return prefs.getString(KEY_USERNAME, "用户");
    }
    
    /**
     * 获取用户邮箱
     */
    public String getCurrentEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }
    
    /**
     * 获取登录提供商
     */
    public String getLoginProvider() {
        return prefs.getString(KEY_PROVIDER, "");
    }
    
    /**
     * 获取用户头像URL
     */
    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, "");
    }
    
    /**
     * 设置用户名
     */
    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }
    
    /**
     * 设置用户邮箱
     */
    public void setEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }
    
    /**
     * 设置登录提供商
     */
    public void setLoginProvider(String provider) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply();
    }
    
    /**
     * 设置用户头像URL
     */
    public void setAvatarUrl(String avatarUrl) {
        prefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply();
    }
    
    /**
     * 判断用户是否已登录
     */
    public boolean isSignedIn() {
        return prefs.getBoolean(KEY_SIGNED_IN, false);
    }
    
    /**
     * 登录
     */
    public void signIn() {
        prefs.edit().putBoolean(KEY_SIGNED_IN, true).apply();
    }
    
    /**
     * 登出
     */
    public void signOut() {
        prefs.edit().putBoolean(KEY_SIGNED_IN, false).apply();
    }
    
    /**
     * 保存键值对数据
     */
    public void saveValue(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
    
    /**
     * 读取键值对数据
     */
    public String getValue(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }
    
    /**
     * 移除数据
     */
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }
    
    /**
     * 清空所有数据
     */
    public void clearAll() {
        prefs.edit().clear().apply();
        
        // 删除数据目录下的所有文件
        if (dataDir.exists()) {
            File[] files = dataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        Log.e(TAG, "无法删除文件: " + file.getName());
                    }
                }
            }
        }
        
        // 清空缓存
        notesCache.clear();
        reviewPlansCache.clear();
    }
    
    /**
     * 保存笔记
     */
    public void saveNote(Note note) {
        if (note == null) return;
        
        // 确保笔记有ID
        if (note.getId() == null || note.getId().isEmpty()) {
            note.setId(UUID.randomUUID().toString());
        }
        
        // 确保有用户ID
        if (note.getUserId() == null || note.getUserId().isEmpty()) {
            note.setUserId(getCurrentUserId());
        }
        
        // 更新修改时间
        note.setLastModifiedDate(new Date());
        
        // 保存到缓存
        notesCache.put(note.getId(), note);
        
        // 保存到本地存储
        saveNotesToLocal();
    }
    
    /**
     * 获取笔记
     */
    public Note getNote(String noteId) {
        return notesCache.get(noteId);
    }
    
    /**
     * 获取当前用户的所有笔记
     */
    public List<Note> getAllNotes() {
        List<Note> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        for (Note note : notesCache.values()) {
            if (note.getUserId() != null && note.getUserId().equals(userId)) {
                result.add(note);
            }
        }
        
        return result;
    }
    
    /**
     * 按主题筛选笔记
     */
    public List<Note> getNotesBySubject(String subject) {
        List<Note> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        for (Note note : notesCache.values()) {
            if (note.getUserId() != null && note.getUserId().equals(userId) 
                    && note.getSubject() != null && note.getSubject().equals(subject)) {
                result.add(note);
            }
        }
        
        return result;
    }
    
    /**
     * 按标签筛选笔记
     */
    public List<Note> getNotesByTag(String tag) {
        List<Note> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        for (Note note : notesCache.values()) {
            if (note.getUserId() != null && note.getUserId().equals(userId) 
                    && note.getTags() != null && note.getTags().contains(tag)) {
                result.add(note);
            }
        }
        
        return result;
    }
    
    /**
     * 搜索笔记
     */
    public List<Note> searchNotes(String query) {
        if (query == null || query.isEmpty()) {
            return getAllNotes();
        }
        
        List<Note> result = new ArrayList<>();
        String userId = getCurrentUserId();
        String lowerQuery = query.toLowerCase();
        
        for (Note note : notesCache.values()) {
            if (note.getUserId() != null && note.getUserId().equals(userId)) {
                // 在标题、内容和主题中搜索
                if ((note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery)) ||
                        (note.getSubject() != null && note.getSubject().toLowerCase().contains(lowerQuery))) {
                    result.add(note);
                    continue;
                }
                
                // 在标签中搜索
                if (note.getTags() != null) {
                    for (String tag : note.getTags()) {
                        if (tag.toLowerCase().contains(lowerQuery)) {
                            result.add(note);
                            break;
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 删除笔记
     */
    public void deleteNote(String noteId) {
        notesCache.remove(noteId);
        
        // 同时删除关联的复习计划
        List<ReviewPlan> plansToRemove = new ArrayList<>();
        for (ReviewPlan plan : reviewPlansCache.values()) {
            if (plan.getNoteId() != null && plan.getNoteId().equals(noteId)) {
                plansToRemove.add(plan);
            }
        }
        
        for (ReviewPlan plan : plansToRemove) {
            reviewPlansCache.remove(plan.getId());
        }
        
        // 保存变更到本地存储
        saveNotesToLocal();
        saveReviewPlansToLocal();
    }
    
    /**
     * 保存复习计划
     */
    public void saveReviewPlan(ReviewPlan plan) {
        if (plan == null) return;
        
        // 确保计划有ID
        if (plan.getId() == null || plan.getId().isEmpty()) {
            plan.setId(UUID.randomUUID().toString());
        }
        
        // 确保有用户ID
        if (plan.getUserId() == null || plan.getUserId().isEmpty()) {
            plan.setUserId(getCurrentUserId());
        }
        
        // 保存到缓存
        reviewPlansCache.put(plan.getId(), plan);
        
        // 保存到本地存储
        saveReviewPlansToLocal();
    }
    
    /**
     * 兼容Map形式的数据保存（旧接口）
     */
    public void saveReviewPlan(String planId, Map<String, String> planData) {
        ReviewPlan plan = convertMapToReviewPlan(planData);
        if (plan != null) {
            plan.setId(planId);
            saveReviewPlan(plan);
        }
    }
    
    /**
     * 获取复习计划
     */
    public ReviewPlan getReviewPlan(String planId) {
        return reviewPlansCache.get(planId);
    }
    
    /**
     * 获取用户的所有复习计划
     */
    public List<ReviewPlan> getAllReviewPlans() {
        List<ReviewPlan> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        for (ReviewPlan plan : reviewPlansCache.values()) {
            if (plan.getUserId() != null && plan.getUserId().equals(userId)) {
                result.add(plan);
            }
        }
        
        return result;
    }
    
    /**
     * 获取今日复习计划
     */
    public List<ReviewPlan> getTodayReviewPlans() {
        List<ReviewPlan> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        Date now = new Date();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(now);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        Date todayStart = calendar.getTime();
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        Date tomorrowStart = calendar.getTime();
        
        for (ReviewPlan plan : reviewPlansCache.values()) {
            if (plan.getUserId() != null && plan.getUserId().equals(userId) 
                    && !plan.isCompleted() && plan.getNextReviewDate() != null
                    && plan.getNextReviewDate().after(todayStart)
                    && plan.getNextReviewDate().before(tomorrowStart)) {
                result.add(plan);
            }
        }
        
        return result;
    }
    
    /**
     * 获取未来复习计划
     */
    public List<ReviewPlan> getUpcomingReviewPlans() {
        List<ReviewPlan> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        Date now = new Date();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(now);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        Date tomorrowStart = calendar.getTime();
        
        for (ReviewPlan plan : reviewPlansCache.values()) {
            if (plan.getUserId() != null && plan.getUserId().equals(userId) 
                    && !plan.isCompleted() && plan.getNextReviewDate() != null
                    && plan.getNextReviewDate().after(tomorrowStart)) {
                result.add(plan);
            }
        }
        
        return result;
    }
    
    /**
     * 获取已完成复习计划
     */
    public List<ReviewPlan> getCompletedReviewPlans() {
        List<ReviewPlan> result = new ArrayList<>();
        String userId = getCurrentUserId();
        
        for (ReviewPlan plan : reviewPlansCache.values()) {
            if (plan.getUserId() != null && plan.getUserId().equals(userId) 
                    && plan.isCompleted()) {
                result.add(plan);
            }
        }
        
        return result;
    }
    
    /**
     * 删除复习计划
     */
    public void deleteReviewPlan(String planId) {
        reviewPlansCache.remove(planId);
        // 保存变更到本地存储
        saveReviewPlansToLocal();
    }
    
    /**
     * 将Map转换为ReviewPlan对象
     */
    private ReviewPlan convertMapToReviewPlan(Map<String, String> planData) {
        if (planData == null || !planData.containsKey("noteId")) {
            return null;
        }
        
        try {
            String noteId = planData.get("noteId");
            String noteTitle = planData.get("noteTitle");
            String noteContent = planData.get("noteContent");
            
            ReviewPlan plan = new ReviewPlan(noteId, noteTitle, noteContent);
            
            if (planData.containsKey("id")) {
                plan.setId(planData.get("id"));
            }
            
            if (planData.containsKey("userId")) {
                plan.setUserId(planData.get("userId"));
            } else {
                plan.setUserId(getCurrentUserId());
            }
            
            if (planData.containsKey("currentStage")) {
                try {
                    int stage = Integer.parseInt(planData.get("currentStage"));
                    plan.setCurrentStage(stage);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "解析currentStage失败", e);
                }
            }
            
            if (planData.containsKey("completed")) {
                plan.setCompleted(Boolean.parseBoolean(planData.get("completed")));
            }
            
            if (planData.containsKey("nextReviewDate")) {
                try {
                    long time = Long.parseLong(planData.get("nextReviewDate"));
                    plan.setNextReviewDate(new Date(time));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "解析nextReviewDate失败", e);
                }
            }
            
            if (planData.containsKey("lastReviewDate")) {
                try {
                    long time = Long.parseLong(planData.get("lastReviewDate"));
                    plan.setLastReviewDate(new Date(time));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "解析lastReviewDate失败", e);
                }
            }
            
            return plan;
        } catch (Exception e) {
            Log.e(TAG, "convertMapToReviewPlan失败", e);
            return null;
        }
    }
    
    /**
     * 兼容旧接口 - 获取笔记数据Map形式
     */
    public Map<String, String> getNote(String noteId, boolean useMap) {
        Note note = notesCache.get(noteId);
        if (note == null) return null;
        
        Map<String, String> noteData = new HashMap<>();
        noteData.put("id", note.getId());
        noteData.put("userId", note.getUserId());
        noteData.put("title", note.getTitle());
        noteData.put("content", note.getContent());
        noteData.put("subject", note.getSubject());
        
        return noteData;
    }
    
    /**
     * 兼容旧接口 - 获取所有笔记Map形式
     */
    public List<Map<String, String>> getAllNotesMap() {
        List<Map<String, String>> result = new ArrayList<>();
        List<Note> notes = getAllNotes();
        
        for (Note note : notes) {
            Map<String, String> noteData = new HashMap<>();
            noteData.put("id", note.getId());
            noteData.put("userId", note.getUserId());
            noteData.put("title", note.getTitle());
            noteData.put("content", note.getContent());
            noteData.put("subject", note.getSubject());
            result.add(noteData);
        }
        
        return result;
    }
    
    /**
     * 兼容旧接口 - 获取所有复习计划Map形式
     */
    public List<Map<String, String>> getAllReviewPlansMap() {
        List<Map<String, String>> result = new ArrayList<>();
        List<ReviewPlan> plans = getAllReviewPlans();
        
        for (ReviewPlan plan : plans) {
            Map<String, String> planData = convertReviewPlanToMap(plan);
            result.add(planData);
        }
        
        return result;
    }
    
    /**
     * 将ReviewPlan对象转换为Map
     */
    private Map<String, String> convertReviewPlanToMap(ReviewPlan plan) {
        if (plan == null) return null;
        
        Map<String, String> planData = new HashMap<>();
        planData.put("id", plan.getId());
        planData.put("noteId", plan.getNoteId());
        planData.put("userId", plan.getUserId());
        planData.put("noteTitle", plan.getNoteTitle());
        planData.put("noteContent", plan.getNoteContent());
        planData.put("currentStage", String.valueOf(plan.getCurrentStage()));
        planData.put("completed", String.valueOf(plan.isCompleted()));
        
        if (plan.getNextReviewDate() != null) {
            planData.put("nextReviewDate", String.valueOf(plan.getNextReviewDate().getTime()));
        }
        
        if (plan.getLastReviewDate() != null) {
            planData.put("lastReviewDate", String.valueOf(plan.getLastReviewDate().getTime()));
        }
        
        return planData;
    }
    
    /**
     * 从本地加载所有数据
     */
    private void loadDataFromLocal() {
        try {
            // 加载笔记数据
            loadNotesFromLocal();
            
            // 加载复习计划数据
            loadReviewPlansFromLocal();
            
            Log.d(TAG, "从本地加载数据成功: " + notesCache.size() + " 个笔记, " 
                    + reviewPlansCache.size() + " 个复习计划");
        } catch (Exception e) {
            Log.e(TAG, "从本地加载数据失败", e);
        }
    }
    
    /**
     * 保存所有复习计划到本地文件
     */
    private void saveReviewPlansToLocal() {
        try {
            File reviewPlansFile = new File(dataDir, "review_plans.json");
            
            // 将复习计划转换为JSON格式
            org.json.JSONArray plansArray = new org.json.JSONArray();
            for (ReviewPlan plan : reviewPlansCache.values()) {
                org.json.JSONObject planJson = new org.json.JSONObject();
                planJson.put("id", plan.getId());
                planJson.put("noteId", plan.getNoteId());
                planJson.put("userId", plan.getUserId());
                planJson.put("noteTitle", plan.getNoteTitle());
                planJson.put("noteContent", plan.getNoteContent());
                planJson.put("currentStage", plan.getCurrentStage());
                planJson.put("completed", plan.isCompleted());
                
                if (plan.getNextReviewDate() != null) {
                    planJson.put("nextReviewDate", plan.getNextReviewDate().getTime());
                }
                
                if (plan.getLastReviewDate() != null) {
                    planJson.put("lastReviewDate", plan.getLastReviewDate().getTime());
                }
                
                // 保存复习日期列表
                if (plan.getReviewDates() != null && !plan.getReviewDates().isEmpty()) {
                    org.json.JSONArray datesArray = new org.json.JSONArray();
                    for (Date date : plan.getReviewDates()) {
                        datesArray.put(date.getTime());
                    }
                    planJson.put("reviewDates", datesArray);
                }
                
                plansArray.put(planJson);
            }
            
            // 写入文件
            java.io.FileWriter writer = new java.io.FileWriter(reviewPlansFile);
            writer.write(plansArray.toString());
            writer.flush();
            writer.close();
            
            Log.d(TAG, "保存复习计划到本地成功: " + reviewPlansCache.size() + " 个计划");
        } catch (Exception e) {
            Log.e(TAG, "保存复习计划到本地失败", e);
        }
    }
    
    /**
     * 从本地文件加载复习计划
     */
    private void loadReviewPlansFromLocal() {
        try {
            File reviewPlansFile = new File(dataDir, "review_plans.json");
            if (!reviewPlansFile.exists()) {
                Log.d(TAG, "本地复习计划文件不存在,跳过加载");
                return;
            }
            
            // 读取文件内容
            java.io.FileReader reader = new java.io.FileReader(reviewPlansFile);
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            reader.close();
            
            // 解析JSON
            org.json.JSONArray plansArray = new org.json.JSONArray(content.toString());
            for (int i = 0; i < plansArray.length(); i++) {
                org.json.JSONObject planJson = plansArray.getJSONObject(i);
                
                ReviewPlan plan = new ReviewPlan();
                
                if (planJson.has("id")) plan.setId(planJson.getString("id"));
                if (planJson.has("noteId")) plan.setNoteId(planJson.getString("noteId"));
                if (planJson.has("userId")) plan.setUserId(planJson.getString("userId"));
                if (planJson.has("noteTitle")) plan.setNoteTitle(planJson.getString("noteTitle"));
                if (planJson.has("noteContent")) plan.setNoteContent(planJson.getString("noteContent"));
                if (planJson.has("currentStage")) plan.setCurrentStage(planJson.getInt("currentStage"));
                if (planJson.has("completed")) plan.setCompleted(planJson.getBoolean("completed"));
                
                if (planJson.has("nextReviewDate")) {
                    plan.setNextReviewDate(new Date(planJson.getLong("nextReviewDate")));
                }
                
                if (planJson.has("lastReviewDate")) {
                    plan.setLastReviewDate(new Date(planJson.getLong("lastReviewDate")));
                }
                
                // 加载复习日期列表
                if (planJson.has("reviewDates")) {
                    org.json.JSONArray datesArray = planJson.getJSONArray("reviewDates");
                    List<Date> reviewDates = new ArrayList<>();
                    for (int j = 0; j < datesArray.length(); j++) {
                        reviewDates.add(new Date(datesArray.getLong(j)));
                    }
                    plan.setReviewDates(reviewDates);
                }
                
                // 添加到缓存
                reviewPlansCache.put(plan.getId(), plan);
            }
            
            Log.d(TAG, "从本地加载复习计划成功: " + reviewPlansCache.size() + " 个计划");
        } catch (Exception e) {
            Log.e(TAG, "从本地加载复习计划失败", e);
        }
    }
    
    /**
     * 保存所有笔记到本地文件
     */
    private void saveNotesToLocal() {
        try {
            File notesFile = new File(dataDir, "notes.json");
            
            // 将笔记转换为JSON格式
            org.json.JSONArray notesArray = new org.json.JSONArray();
            for (Note note : notesCache.values()) {
                org.json.JSONObject noteJson = new org.json.JSONObject();
                noteJson.put("id", note.getId());
                noteJson.put("userId", note.getUserId());
                noteJson.put("title", note.getTitle());
                noteJson.put("content", note.getContent());
                noteJson.put("subject", note.getSubject());
                
                if (note.getCreatedDate() != null) {
                    noteJson.put("createdDate", note.getCreatedDate().getTime());
                }
                
                if (note.getLastModifiedDate() != null) {
                    noteJson.put("lastModifiedDate", note.getLastModifiedDate().getTime());
                }
                
                // 保存标签列表
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    org.json.JSONArray tagsArray = new org.json.JSONArray();
                    for (String tag : note.getTags()) {
                        tagsArray.put(tag);
                    }
                    noteJson.put("tags", tagsArray);
                }
                
                // 保存附件列表
                if (note.getAttachmentPaths() != null && !note.getAttachmentPaths().isEmpty()) {
                    org.json.JSONArray attachmentsArray = new org.json.JSONArray();
                    for (String attachment : note.getAttachmentPaths()) {
                        attachmentsArray.put(attachment);
                    }
                    noteJson.put("attachments", attachmentsArray);
                }
                
                notesArray.put(noteJson);
            }
            
            // 写入文件
            java.io.FileWriter writer = new java.io.FileWriter(notesFile);
            writer.write(notesArray.toString());
            writer.flush();
            writer.close();
            
            Log.d(TAG, "保存笔记到本地成功: " + notesCache.size() + " 个笔记");
        } catch (Exception e) {
            Log.e(TAG, "保存笔记到本地失败", e);
        }
    }
    
    /**
     * 从本地文件加载笔记
     */
    private void loadNotesFromLocal() {
        try {
            File notesFile = new File(dataDir, "notes.json");
            if (!notesFile.exists()) {
                Log.d(TAG, "本地笔记文件不存在,跳过加载");
                return;
            }
            
            // 读取文件内容
            java.io.FileReader reader = new java.io.FileReader(notesFile);
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            reader.close();
            
            // 解析JSON
            org.json.JSONArray notesArray = new org.json.JSONArray(content.toString());
            for (int i = 0; i < notesArray.length(); i++) {
                org.json.JSONObject noteJson = notesArray.getJSONObject(i);
                
                String title = noteJson.optString("title", "");
                String noteContent = noteJson.optString("content", "");
                String subject = noteJson.optString("subject", "");
                
                Note note = new Note(title, noteContent, subject);
                
                if (noteJson.has("id")) note.setId(noteJson.getString("id"));
                if (noteJson.has("userId")) note.setUserId(noteJson.getString("userId"));
                
                if (noteJson.has("createdDate")) {
                    note.setCreatedDate(new Date(noteJson.getLong("createdDate")));
                }
                
                if (noteJson.has("lastModifiedDate")) {
                    note.setLastModifiedDate(new Date(noteJson.getLong("lastModifiedDate")));
                }
                
                // 加载标签列表
                if (noteJson.has("tags")) {
                    org.json.JSONArray tagsArray = noteJson.getJSONArray("tags");
                    for (int j = 0; j < tagsArray.length(); j++) {
                        note.addTag(tagsArray.getString(j));
                    }
                }
                
                // 加载附件列表
                if (noteJson.has("attachments")) {
                    org.json.JSONArray attachmentsArray = noteJson.getJSONArray("attachments");
                    for (int j = 0; j < attachmentsArray.length(); j++) {
                        note.addAttachmentPath(attachmentsArray.getString(j));
                    }
                }
                
                // 添加到缓存
                notesCache.put(note.getId(), note);
            }
            
            Log.d(TAG, "从本地加载笔记成功: " + notesCache.size() + " 个笔记");
        } catch (Exception e) {
            Log.e(TAG, "从本地加载笔记失败", e);
        }
    }
} 