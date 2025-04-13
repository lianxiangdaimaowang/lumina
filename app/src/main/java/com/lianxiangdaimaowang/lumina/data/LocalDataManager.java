package com.lianxiangdaimaowang.lumina.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.model.Post;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Type;
import java.util.Random;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
    private static final String KEY_ACCOUNT_PREFIX = "account_";
    private static final String KEY_PASSWORD_PREFIX = "password_";
    private static final String KEY_ACCOUNT_LIST = "account_list";
    private static final String KEY_POSTS = "posts";
    public static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_LOCAL_POSTS = "local_posts"; // 本地帖子存储键
    
    // 内存中缓存笔记数据
    private final Map<String, Note> notesCache = new HashMap<>();
    
    private static LocalDataManager instance;
    private final SharedPreferences prefs;
    private final File dataDir;
    private final Context context; // 添加Context实例变量
    
    private List<Post> posts;
    private final Gson gson = new Gson();
    
    private LocalDataManager(Context context) {
        this.context = context.getApplicationContext(); // 保存Context
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
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
     * 获取当前用户ID
     * 返回服务器分配的用户ID，用于API请求和数据同步
     */
    public String getCurrentUserId() {
        String userId = prefs.getString(KEY_USER_ID, null);
        
        if (userId == null || userId.isEmpty()) {
            // 返回null，表示用户ID未设置
            // 让服务器分配用户ID
            Log.d(TAG, "本地没有用户ID，返回null");
            return null;
        } else {
            // 返回服务器分配的用户ID
            // 注意：不再检查ID是否为数字，因为服务器可能使用其他格式的ID
            Log.d(TAG, "返回服务器分配的用户ID: " + userId);
            return userId;
        }
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
     * 设置登录状态
     */
    public void setLoggedIn(boolean isLoggedIn) {
        prefs.edit().putBoolean(KEY_SIGNED_IN, isLoggedIn).apply();
    }
    
    /**
     * 保存当前用户信息
     */
    public void saveCurrentUser(String username) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .apply();
        
        // 不再将用户名作为用户ID保存
        // 用户ID应该由服务器分配，或者在登录时从JWT令牌中提取
    }
    
    /**
     * 保存当前用户信息（包含服务器分配的用户ID）
     * @param username 用户名
     * @param userId 服务器分配的用户ID
     */
    public void saveCurrentUserWithId(String username, String userId) {
        Log.d(TAG, "保存用户信息: 用户名=" + username + ", 用户ID=" + userId);
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_ID, userId)
            .apply();
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
     * 保存账号密码
     */
    public void saveAccount(String account, String password) {
        // 获取现有账号列表
        String accountList = prefs.getString(KEY_ACCOUNT_LIST, "");
        if (!accountList.contains(account)) {
            // 添加新账号到列表
            accountList = accountList.isEmpty() ? account : accountList + "," + account;
            prefs.edit()
                .putString(KEY_ACCOUNT_LIST, accountList)
                .putString(KEY_ACCOUNT_PREFIX + account, account)
                .putString(KEY_PASSWORD_PREFIX + account, password)
                .apply();
        }
    }
    
    /**
     * 验证账号密码
     */
    public boolean verifyAccount(String account, String password) {
        String savedPassword = prefs.getString(KEY_PASSWORD_PREFIX + account, "");
        return !savedPassword.isEmpty() && savedPassword.equals(password);
    }
    
    /**
     * 检查账号是否存在
     */
    public boolean isAccountExists(String account) {
        String accountList = prefs.getString(KEY_ACCOUNT_LIST, "");
        return accountList.contains(account);
    }
    
    /**
     * 获取所有账号列表
     */
    public List<String> getAllAccounts() {
        String accountList = prefs.getString(KEY_ACCOUNT_LIST, "");
        List<String> accounts = new ArrayList<>();
        if (!accountList.isEmpty()) {
            String[] accountArray = accountList.split(",");
            for (String account : accountArray) {
                accounts.add(account);
            }
        }
        return accounts;
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
    }
    
    /**
     * 清空所有笔记缓存
     * 用于与服务器同步时重置本地缓存
     */
    public void clearAllNotes() {
        // 清空笔记缓存
        notesCache.clear();
        
        // 保存变更到本地存储
        saveNotesToLocal();
        
        Log.d(TAG, "已清空笔记缓存");
    }
    
    /**
     * 保存笔记
     */
    public void saveNote(Note note) {
        if (note == null) return;
        
        // 确保笔记有ID
        if (note.getId() == null || note.getId().isEmpty()) {
            // 不再自动生成UUID，让同步逻辑决定如何处理ID
            // note.setId(UUID.randomUUID().toString());
            Log.d(TAG, "笔记没有ID，等待服务器分配ID");
        }
        
        // 确保有用户ID
        if (note.getUserId() == null || note.getUserId().isEmpty()) {
            note.setUserId(getCurrentUserId());
        }
        
        // 更新修改时间
        note.setLastModifiedDate(new Date());
        
        // 保存到缓存
        if (note.getId() != null && !note.getId().isEmpty()) {
            notesCache.put(note.getId(), note);
            
            // 保存到本地存储
            saveNotesToLocal();
            Log.d(TAG, "保存笔记到本地成功: " + notesCache.size() + " 个笔记");
        } else {
            Log.w(TAG, "笔记ID为空，无法保存到本地缓存");
        }
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
        // 检查是否确实从缓存中移除了笔记
        boolean removed = notesCache.containsKey(noteId);
        notesCache.remove(noteId);
        
        // 保存变更到本地存储
        saveNotesToLocal();
        
        // 发送本地广播通知应用中的其他组件笔记已被删除
        sendNoteDeletedBroadcast(noteId);
        
        if (removed) {
            Log.d(TAG, "已成功从缓存中删除笔记ID: " + noteId);
        } else {
            Log.w(TAG, "未能从缓存中删除笔记ID: " + noteId + " (可能不存在)");
        }
    }
    
    /**
     * 发送笔记删除广播，通知应用中的其他组件
     */
    private void sendNoteDeletedBroadcast(String noteId) {
        Intent intent = new Intent("com.lianxiangdaimaowang.lumina.NOTE_DELETED");
        intent.putExtra("note_id", noteId);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
        Log.d(TAG, "已发送笔记删除广播，noteId=" + noteId);
    }
    
    /**
     * 从本地加载所有数据
     */
    private void loadDataFromLocal() {
        // 使用后台线程加载数据，避免阻塞主线程
        new Thread(() -> {
            try {
                // 加载笔记数据
                loadNotesFromLocal();
                
                Log.d(TAG, "从本地加载数据成功: " + notesCache.size() + " 个笔记");
            } catch (Exception e) {
                Log.e(TAG, "从本地加载数据失败", e);
            }
        }).start();
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

    public List<Post> getAllPosts() {
        if (posts == null) {
            String postsJson = prefs.getString(KEY_POSTS, "[]");
            Type type = new TypeToken<List<Post>>(){}.getType();
            posts = gson.fromJson(postsJson, type);
            if (posts == null) {
                posts = new ArrayList<>();
            }
        }
        return posts;
    }

    public void savePost(Post post) {
        if (posts == null) {
            getAllPosts();
        }
        boolean found = false;
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId().equals(post.getId())) {
                posts.set(i, post);
                found = true;
                break;
            }
        }
        if (!found) {
            posts.add(post);
        }
        String postsJson = gson.toJson(posts);
        prefs.edit().putString(KEY_POSTS, postsJson).apply();
    }

    /**
     * 保存认证令牌
     * @param token JWT令牌，服务器返回的原始令牌
     */
    public void saveAuthToken(String token) {
        // 保存原始token，不添加任何前缀
        saveValue(KEY_AUTH_TOKEN, token);
    }
    
    /**
     * 获取认证令牌
     * @return JWT令牌，如果不存在则返回null
     */
    public String getAuthToken() {
        // 获取保存的原始token
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }
    
    /**
     * 清除认证令牌
     */
    public void clearAuthToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply();
    }

    /**
     * 保存本地帖子
     */
    public void saveLocalPost(LocalPost post) {
        if (post == null) return;
        
        // 设置Context
        post.setContext(context);
        
        List<LocalPost> posts = getAllLocalPosts();
        
        // 检查是否已存在该帖子
        boolean found = false;
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId() != null && posts.get(i).getId().equals(post.getId())) {
                // 已存在，更新
                posts.set(i, post);
                found = true;
                break;
            }
        }
        
        // 如果不存在，添加
        if (!found) {
            posts.add(post);
        }
        
        // 保存
        save(KEY_LOCAL_POSTS, new Gson().toJson(posts));
        
        Log.d(TAG, "保存本地帖子: " + post.getId() + ", 帖子总数: " + posts.size());
    }

    /**
     * 获取所有本地帖子
     */
    public List<LocalPost> getAllLocalPosts() {
        List<LocalPost> posts = new ArrayList<>();
        
        try {
            // 读取本地帖子列表
            Type type = new TypeToken<List<LocalPost>>(){}.getType();
            String postsJson = load(KEY_LOCAL_POSTS);
            
            if (postsJson != null && !postsJson.isEmpty()) {
                List<LocalPost> loadedPosts = new Gson().fromJson(postsJson, type);
                if (loadedPosts != null) {
                    posts.addAll(loadedPosts);
                    
                    // 设置Context
                    for (LocalPost post : posts) {
                        post.setContext(context);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "加载本地帖子失败", e);
        }
        
        return posts;
    }

    /**
     * 获取单个本地帖子
     */
    public LocalPost getLocalPostById(String postId) {
        if (postId == null || postId.isEmpty()) {
            return null;
        }
        
        List<LocalPost> posts = getAllLocalPosts();
        for (LocalPost post : posts) {
            if (post.getId() != null && post.getId().equals(postId)) {
                return post;
            }
        }
        
        return null;
    }
    
    /**
     * 获取热门帖子（点赞数最高的前N个帖子）
     */
    public List<LocalPost> getHotLocalPosts(int limit) {
        List<LocalPost> allPosts = getAllLocalPosts();
        
        // 创建一个Map来分组相同点赞数的帖子
        Map<Integer, List<LocalPost>> likeCountGroups = new HashMap<>();
        
        // 分组处理
        for (LocalPost post : allPosts) {
            int likeCount = post.getLikeCount();
            if (!likeCountGroups.containsKey(likeCount)) {
                likeCountGroups.put(likeCount, new ArrayList<>());
            }
            likeCountGroups.get(likeCount).add(post);
        }
        
        // 获取所有点赞数，并降序排序
        List<Integer> likeCounts = new ArrayList<>(likeCountGroups.keySet());
        Collections.sort(likeCounts, Collections.reverseOrder());
        
        // 构建最终的热门帖子列表
        List<LocalPost> hotPosts = new ArrayList<>();
        Random random = new Random();
        
        // 从最高点赞数开始，依次处理每个点赞数组
        for (Integer likeCount : likeCounts) {
            // 删除只处理点赞数大于0的条件，允许所有帖子都能被选中
            List<LocalPost> postsWithSameLikes = likeCountGroups.get(likeCount);
            
            // 随机打乱顺序
            Collections.shuffle(postsWithSameLikes, random);
            
            // 每个点赞数组只选一个帖子
            if (!postsWithSameLikes.isEmpty()) {
                hotPosts.add(postsWithSameLikes.get(0));
            }
            
            // 如果已经达到限制，退出循环
            if (hotPosts.size() >= limit) {
                break;
            }
        }
        
        return hotPosts;
    }
    
    /**
     * 获取用户收藏的帖子
     */
    public List<LocalPost> getUserFavoritePosts() {
        List<LocalPost> allPosts = getAllLocalPosts();
        List<LocalPost> favoritePosts = new ArrayList<>();
        
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            return favoritePosts;
        }
        
        Log.d(TAG, "获取用户收藏帖子 - 用户ID: " + userId + ", 总帖子数: " + allPosts.size());
        
        for (LocalPost post : allPosts) {
            boolean isFavorited = post.isFavoritedBy(userId);
            
            // 输出调试信息
            List<String> favorites = post.getFavorites();
            Log.d(TAG, "检查收藏状态 - 帖子ID: " + post.getId() + 
                    ", 标题: " + post.getTitle() + 
                    ", 收藏用户: " + (favorites != null ? favorites.toString() : "[]") + 
                    ", 是否被当前用户收藏: " + isFavorited);
            
            // 额外检查，用多种方式尝试匹配用户ID
            if (favorites != null) {
                for (String favoriteId : favorites) {
                    String favoriteIdStr = String.valueOf(favoriteId);
                    String userIdStr = String.valueOf(userId);
                    
                    if (favoriteIdStr.equals(userIdStr)) {
                        Log.d(TAG, "额外匹配成功 - 帖子ID: " + post.getId() + 
                                ", 收藏用户ID: " + favoriteId + ", 当前用户ID: " + userId);
                        isFavorited = true;
                        post.addFavorite(userId); // 确保加入收藏列表
                        saveLocalPost(post); // 保存更新
                    }
                }
            }
            
            if (isFavorited) {
                Log.d(TAG, "找到用户收藏的帖子 - ID: " + post.getId() + ", 标题: " + post.getTitle());
                favoritePosts.add(post);
            }
        }
        
        Log.d(TAG, "用户收藏帖子总数: " + favoritePosts.size());
        return favoritePosts;
    }

    /**
     * 删除本地帖子
     */
    public void deleteLocalPost(String postId) {
        List<LocalPost> posts = getAllLocalPosts();
        
        // 移除指定ID的帖子
        posts.removeIf(post -> post.getId().equals(postId));
        
        // 保存到SharedPreferences
        String json = gson.toJson(posts);
        prefs.edit().putString(KEY_LOCAL_POSTS, json).apply();
    }

    /**
     * 清除本地帖子数据
     */
    public void clearLocalPosts() {
        Log.d(TAG, "清空本地帖子数据");
        save(KEY_LOCAL_POSTS, "");
    }

    /**
     * 保存字符串数据到SharedPreferences
     */
    private void save(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
    
    /**
     * 从SharedPreferences加载字符串数据
     */
    private String load(String key) {
        return prefs.getString(key, "");
    }

    /**
     * 保存用户ID与服务器ID的映射关系
     * @param clientUserId 客户端用户ID
     * @param serverUserId 服务器用户ID
     */
    public void saveUserIdMapping(String clientUserId, String serverUserId) {
        if (clientUserId != null && serverUserId != null && !clientUserId.equals(serverUserId)) {
            Log.d(TAG, "保存用户ID映射: 客户端ID=" + clientUserId + ", 服务器ID=" + serverUserId);
            saveValue("server_user_id_" + clientUserId, serverUserId);
        }
    }
    
    /**
     * 获取与客户端用户ID对应的服务器用户ID
     * @param clientUserId 客户端用户ID
     * @return 服务器用户ID，如果没有映射则返回null
     */
    public String getServerUserId(String clientUserId) {
        if (clientUserId == null) return null;
        
        String serverUserId = getValue("server_user_id_" + clientUserId, null);
        if (serverUserId != null && !serverUserId.isEmpty()) {
            Log.d(TAG, "获取到用户ID映射: 客户端ID=" + clientUserId + ", 服务器ID=" + serverUserId);
            return serverUserId;
        }
        
        return null;
    }
    
    /**
     * 获取当前用户的服务器ID
     * 如果存在映射关系则返回服务器ID，否则返回客户端ID
     * @return 有效的服务器用户ID
     */
    public String getEffectiveUserId() {
        String clientUserId = getCurrentUserId();
        String serverUserId = getServerUserId(clientUserId);
        
        if (serverUserId != null) {
            Log.d(TAG, "使用服务器映射ID: " + serverUserId + " (客户端ID: " + clientUserId + ")");
            return serverUserId;
        }
        
        Log.d(TAG, "使用客户端ID: " + clientUserId + " (无映射)");
        return clientUserId;
    }
} 