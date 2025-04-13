package com.lianxiangdaimaowang.lumina.sync;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lianxiangdaimaowang.lumina.api.ApiClient;
import com.lianxiangdaimaowang.lumina.api.ApiService;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;
import com.lianxiangdaimaowang.lumina.database.NoteEntity;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.model.Note;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 笔记同步器
 * 负责处理笔记的同步操作
 */
public class NoteSynchronizer {
    private static final String TAG = "NoteSynchronizer";
    
    private final Context context;
    private final LocalDataManager localDataManager;
    private final ApiService apiService;
    private final NetworkManager networkManager;
    
    // 待同步的笔记列表
    private final Map<String, Note> pendingNotes = new HashMap<>();
    
    // 存储从服务器获取的笔记列表
    private List<Note> serverNotes = new ArrayList<>();
    
    /**
     * 构造函数
     */
    public NoteSynchronizer(Context context) {
        this.context = context.getApplicationContext();
        this.localDataManager = LocalDataManager.getInstance(context);
        this.apiService = ApiClient.getApiService(context);
        this.networkManager = NetworkManager.getInstance(context);
    }
    
    /**
     * 添加待同步的笔记
     */
    public void addPendingNote(Note note) {
        if (note != null && note.getId() != null) {
            pendingNotes.put(note.getId(), note);
            Log.d(TAG, "添加待同步笔记: " + note.getTitle());
        }
    }
    
    /**
     * 获取服务器笔记列表
     */
    public List<Note> getServerNotes() {
        return new ArrayList<>(serverNotes);
    }
    
    /**
     * 获取指定ID的服务器笔记
     */
    public Note getServerNoteById(String noteId) {
        for (Note note : serverNotes) {
            if (note.getId().equals(noteId)) {
                return note;
            }
        }
        return null;
    }
    
    /**
     * 同步所有待同步的笔记到服务器
     */
    public void syncPendingNotes(SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        List<Note> notes = new ArrayList<>(pendingNotes.values());
        for (Note note : notes) {
            syncNoteToServer(note, null);
        }
        
        if (callback != null) {
            callback.onSuccess();
        }
        
        Log.d(TAG, "已尝试同步所有待同步笔记: " + notes.size() + " 个笔记");
    }
    
    /**
     * 保存笔记，直接保存到服务器
     */
    public void saveNote(Note note, SyncCallback callback) {
        // 检查网络连接
        if (!networkManager.isNetworkConnected()) {
            // 无网络时通知用户
            Log.d(TAG, "无网络连接，无法保存笔记到云端: " + note.getTitle());
            if (callback != null) {
                callback.onError("无网络连接，无法保存到云端。请检查网络后重试。");
            }
            return;
        }
        
        // 有网络，直接同步到服务器
        Log.d(TAG, "正在保存笔记到云端: " + note.getTitle());
        syncNoteToServer(note, callback);
    }
    
    /**
     * 同步笔记到服务器
     */
    public void syncNoteToServer(final Note note, final SyncCallback callback) {
        if (note == null) {
            Log.e(TAG, "同步笔记失败: 笔记对象为空");
            if (callback != null) {
                callback.onError("笔记对象为空");
            }
            return;
        }
        
        try {
            Log.d(TAG, "开始同步笔记到服务器: ID=" + note.getId() + ", 标题=" + note.getTitle() + ", 科目=" + note.getSubject());
            
            // 创建用于同步的服务器笔记对象
            Note serverNote = new Note();
            serverNote.setId(note.getId());
            serverNote.setTitle(note.getTitle());
            serverNote.setContent(note.getContent());
            serverNote.setSubject(note.getSubject());
            serverNote.setTags(note.getTags());
            serverNote.setUserId(note.getUserId());
            serverNote.setCreatedDate(note.getCreatedDate());
            serverNote.setLastModifiedDate(note.getLastModifiedDate());
            serverNote.setShared(note.isShared());
            serverNote.setAttachmentPaths(note.getAttachmentPaths());
            
            // 使用GSON手动构建包含categoryId字段的JSON
            try {
                Gson gson = new Gson();
                String noteJson = gson.toJson(serverNote);
                
                // 将JSON转换为JSONObject
                JSONObject jsonObject = new JSONObject(noteJson);
                
                // 添加categoryId字段
                Integer categoryId = note.getCategoryIdFromSubject();
                jsonObject.put("categoryId", categoryId);
                
                // 记录日志
                Log.d(TAG, "设置笔记categoryId为: " + categoryId + ", 对应科目: " + note.getSubject());
                
                // 转回字符串用于调试
                String updatedJson = jsonObject.toString();
                Log.d(TAG, "发送到服务器的笔记JSON: " + updatedJson);
            } catch (Exception e) {
                Log.e(TAG, "构建笔记JSON时出错: " + e.getMessage(), e);
            }
            
            // 打印完整笔记内容用于调试
            ApiClient.logRequestAndResponse(TAG, serverNote, null);
            
            // 检查网络连接
            if (!networkManager.isNetworkConnected()) {
                Log.e(TAG, "无网络连接，无法同步笔记");
                if (callback != null) {
                    callback.onError("无网络连接，请检查网络设置");
                }
                return;
            }
            
            // 处理用户ID
            processUserId(serverNote);
            
            // 判断是创建新笔记还是更新现有笔记
            if (serverNote.getId() != null && !serverNote.getId().isEmpty()) {
                updateExistingNote(serverNote, note, callback);
            } else {
                createNewNote(serverNote, note, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "同步笔记时发生异常: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("同步过程中发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理笔记的用户ID
     */
    private void processUserId(Note serverNote) {
        try {
            // 尝试从JWT token中提取用户ID
            String token = localDataManager.getAuthToken();
            if (token != null && !token.isEmpty()) {
                Log.d(TAG, "JWT令牌: " + token);
                // 解析JWT获取userId
                String serverUserId = SyncUtils.extractUserIdFromToken(token);
                
                if (serverUserId != null && !serverUserId.isEmpty()) {
                    // 将用户ID转换为Long类型
                    try {
                        // 处理可能包含小数点的用户ID
                        if (serverUserId.contains(".")) {
                            double doubleValue = Double.parseDouble(serverUserId);
                            long longValue = (long) doubleValue;
                            serverUserId = String.valueOf(longValue);
                            Log.d(TAG, "处理包含小数点的用户ID: " + serverUserId);
                        }
                        
                        long userIdLong = Long.parseLong(serverUserId);
                        // 使用服务器返回的用户ID，不使用本地保存的ID
                        serverNote.setUserId(String.valueOf(userIdLong));
                        Log.d(TAG, "使用JWT令牌中的用户ID: " + serverUserId);
                        
                        // 更新本地存储的用户ID，使其与服务器一致
                        localDataManager.saveValue(LocalDataManager.KEY_USER_ID, serverUserId);
                        Log.d(TAG, "已更新本地存储的用户ID为: " + serverUserId);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "JWT中的用户ID不是有效的数字: " + serverUserId);
                        useDefaultUserId(serverNote);
                    }
                } else {
                    useDefaultUserId(serverNote);
                }
            } else {
                useDefaultUserId(serverNote);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理用户ID时出错: " + e.getMessage());
            useDefaultUserId(serverNote);
        }
        
        Log.d(TAG, "设置笔记用户ID为: " + serverNote.getUserId());
    }
    
    private void useDefaultUserId(Note serverNote) {
        // 从本地获取用户ID，如果本地也没有则使用默认值
        String localUserId = localDataManager.getCurrentUserId();
        serverNote.setUserId(localUserId);
        Log.d(TAG, "无法从令牌获取用户ID，使用本地用户ID: " + localUserId);
    }
    
    /**
     * 更新已存在的笔记
     */
    private void updateExistingNote(Note serverNote, Note originalNote, SyncCallback callback) {
        Log.d(TAG, "正在同步笔记到服务器: ID=" + serverNote.getId() + ", 标题=" + serverNote.getTitle() + ", 用户ID=" + serverNote.getUserId());
        
        // 预处理笔记ID，确保不包含小数点
        String noteId = serverNote.getId();
        noteId = SyncUtils.normalizeId(noteId);
        serverNote.setId(noteId);
        
        // 获取科目对应的categoryId并记录日志
        Integer categoryId = originalNote.getCategoryIdFromSubject();
        Log.d(TAG, "更新笔记科目: " + originalNote.getSubject() + ", 对应categoryId: " + categoryId);
        
        // 将Note对象转换为Map并添加categoryId
        Gson gson = new Gson();
        String noteJson = gson.toJson(serverNote);
        Map<String, Object> noteMap = gson.fromJson(noteJson, new TypeToken<Map<String, Object>>(){}.getType());
        noteMap = SyncUtils.addCategoryIdToRequest(noteMap, originalNote);
        
        // 检查Map中是否正确添加了categoryId
        Log.d(TAG, "发送到服务器的请求Map: " + noteMap.toString());
        
        // 尝试更新笔记
        Call<Note> call = apiService.updateNoteWithMap(noteId, noteMap);
        final String finalNoteId = noteId;
        call.enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 创建用于解析的笔记对象
                    Note responseNote = new Note();
                    
                    try {
                        // 将响应体转为JSON字符串
                        String responseBodyString = gson.toJson(response.body());
                        
                        // 尝试处理外层JSON响应
                        JSONObject jsonResponse = new JSONObject(responseBodyString);
                        Log.d(TAG, "服务器响应: " + responseBodyString);
                        
                        // 检查是否包含note字段（服务器嵌套响应）
                        if (jsonResponse.has("note")) {
                            // 获取嵌套的note对象
                            JSONObject noteObject = jsonResponse.getJSONObject("note");
                            
                            // 提取各字段并设置到responseNote
                            if (noteObject.has("id")) {
                                String idStr = String.valueOf(noteObject.get("id"));
                                responseNote.setId(SyncUtils.normalizeId(idStr));
                                Log.d(TAG, "服务器返回的笔记ID: " + responseNote.getId());
                            }
                            
                            if (noteObject.has("title")) {
                                responseNote.setTitle(noteObject.getString("title"));
                            }
                            
                            if (noteObject.has("content")) {
                                responseNote.setContent(noteObject.getString("content"));
                            }
                            
                            if (noteObject.has("categoryId")) {
                                Object categoryId = noteObject.get("categoryId");
                                // 直接设置categoryId字段
                                if (categoryId instanceof Number) {
                                    responseNote.setCategoryId(((Number) categoryId).intValue());
                                } else if (categoryId instanceof String) {
                                    try {
                                        responseNote.setCategoryId(Integer.parseInt((String) categoryId));
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "无法将categoryId转换为整数: " + categoryId);
                                    }
                                } else {
                                    Log.d(TAG, "使用categoryId设置科目: " + categoryId);
                                    responseNote.setSubjectFromCategoryId(categoryId);
                                }
                            }
                            
                            if (noteObject.has("userId")) {
                                String userIdStr = String.valueOf(noteObject.get("userId"));
                                responseNote.setUserId(SyncUtils.normalizeId(userIdStr));
                            }
                            
                            Log.d(TAG, "成功解析服务器返回的笔记数据: ID=" + responseNote.getId() + 
                                  ", 标题=" + responseNote.getTitle() + 
                                  ", 用户ID=" + responseNote.getUserId() + 
                                  ", 科目=" + responseNote.getSubject());
                        } else {
                            // 尝试直接解析响应体本身，可能是直接返回的笔记对象
                            try {
                                // 如果响应本身不包含note字段，尝试检查响应是否有完整的笔记结构
                                if (jsonResponse.has("id")) {
                                    String idStr = String.valueOf(jsonResponse.get("id"));
                                    responseNote.setId(SyncUtils.normalizeId(idStr));
                                    Log.d(TAG, "直接从响应解析的笔记ID: " + responseNote.getId());
                                }
                                
                                if (jsonResponse.has("title")) {
                                    responseNote.setTitle(jsonResponse.getString("title"));
                                }
                                
                                if (jsonResponse.has("content")) {
                                    responseNote.setContent(jsonResponse.getString("content"));
                                }
                                
                                if (jsonResponse.has("categoryId")) {
                                    Object categoryId = jsonResponse.get("categoryId");
                                    // 直接设置categoryId字段
                                    if (categoryId instanceof Number) {
                                        responseNote.setCategoryId(((Number) categoryId).intValue());
                                    } else if (categoryId instanceof String) {
                                        try {
                                            responseNote.setCategoryId(Integer.parseInt((String) categoryId));
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "无法将categoryId转换为整数: " + categoryId);
                                        }
                                    } else {
                                        Log.d(TAG, "使用categoryId设置科目: " + categoryId);
                                        responseNote.setSubjectFromCategoryId(categoryId);
                                    }
                                }
                                
                                if (jsonResponse.has("userId")) {
                                    String userIdStr = String.valueOf(jsonResponse.get("userId"));
                                    responseNote.setUserId(SyncUtils.normalizeId(userIdStr));
                                }
                                
                                if (responseNote.getId() != null) {
                                    Log.d(TAG, "成功从直接响应中解析笔记数据");
                                } else {
                                    Log.e(TAG, "服务器响应中没有note字段且不是完整笔记对象: " + responseBodyString);
                                    
                                    // 尝试使用ApiClient获取原始响应内容
                                    String rawContent = ApiClient.getRawResponseBodyAsString(response);
                                    if (rawContent != null && !rawContent.isEmpty()) {
                                        Log.d(TAG, "尝试解析原始响应内容: " + rawContent);
                                        
                                        try {
                                            JSONObject rawJson = new JSONObject(rawContent);
                                            if (rawJson.has("note")) {
                                                JSONObject noteObject = rawJson.getJSONObject("note");
                                                
                                                if (noteObject.has("id")) {
                                                    String idStr = String.valueOf(noteObject.get("id"));
                                                    responseNote.setId(SyncUtils.normalizeId(idStr));
                                                    Log.d(TAG, "从原始响应解析的笔记ID: " + responseNote.getId());
                                                }
                                                
                                                if (noteObject.has("title")) {
                                                    responseNote.setTitle(noteObject.getString("title"));
                                                }
                                                
                                                if (noteObject.has("content")) {
                                                    responseNote.setContent(noteObject.getString("content"));
                                                }
                                                
                                                if (noteObject.has("categoryId")) {
                                                    // 使用categoryId设置科目
                                                    Object categoryId = noteObject.get("categoryId");
                                                    // 直接设置categoryId字段
                                                    if (categoryId instanceof Number) {
                                                        responseNote.setCategoryId(((Number) categoryId).intValue());
                                                    } else if (categoryId instanceof String) {
                                                        try {
                                                            responseNote.setCategoryId(Integer.parseInt((String) categoryId));
                                                        } catch (NumberFormatException e) {
                                                            Log.e(TAG, "无法将categoryId转换为整数: " + categoryId);
                                                            responseNote.setSubjectFromCategoryId(categoryId);
                                                        }
                                                    } else {
                                                        responseNote.setSubjectFromCategoryId(categoryId);
                                                        Log.d(TAG, "从原始响应解析的categoryId(其他类型): " + categoryId + " -> 科目: " + responseNote.getSubject());
                                                    }
                                                }
                                                
                                                if (noteObject.has("userId")) {
                                                    String userIdStr = String.valueOf(noteObject.get("userId"));
                                                    responseNote.setUserId(SyncUtils.normalizeId(userIdStr));
                                                }
                                                
                                                Log.d(TAG, "成功从原始响应解析笔记数据");
                                            }
                                        } catch (Exception e3) {
                                            Log.e(TAG, "解析原始响应内容失败: " + e3.getMessage(), e3);
                                        }
                                    }
                                }
                            } catch (Exception e2) {
                                Log.e(TAG, "尝试直接解析响应体失败: " + e2.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析服务器响应出错: " + e.getMessage(), e);
                        
                        // 尝试使用另一种方式解析原始响应
                        try {
                            String rawResponse = response.raw().toString();
                            Log.d(TAG, "尝试解析原始响应: " + rawResponse);
                            
                            // 尝试直接从响应中获取note字段
                            JSONObject responseJson = null;
                            try {
                                // 从response.body()获取JSON，而不是response.raw().body()
                                responseJson = new JSONObject(gson.toJson(response.body()));
                                Log.d(TAG, "从response.body()获取的JSON: " + responseJson);
                                
                                if (responseJson.has("note")) {
                                    JSONObject noteObject = responseJson.getJSONObject("note");
                                    if (noteObject.has("id")) {
                                        String idStr = String.valueOf(noteObject.get("id"));
                                        responseNote.setId(SyncUtils.normalizeId(idStr));
                                        Log.d(TAG, "从response.body()解析的笔记ID: " + responseNote.getId());
                                    }
                                }
                            } catch (Exception e2) {
                                Log.e(TAG, "解析response.body()失败: " + e2.getMessage());
                            }
                            
                            // 尝试处理Retrofit响应
                            if (response.body() instanceof Map) {
                                try {
                                    Map<String, Object> responseMap = (Map<String, Object>) response.body();
                                    if (responseMap.containsKey("note")) {
                                        Object noteObj = responseMap.get("note");
                                        if (noteObj instanceof Map) {
                                            Map<String, Object> noteMap = (Map<String, Object>) noteObj;
                                            if (noteMap.containsKey("id")) {
                                                String idStr = String.valueOf(noteMap.get("id"));
                                                responseNote.setId(SyncUtils.normalizeId(idStr));
                                                Log.d(TAG, "从Map解析的笔记ID: " + responseNote.getId());
                                            }
                                        }
                                    }
                                } catch (Exception e2) {
                                    Log.e(TAG, "处理Map响应失败: " + e2.getMessage());
                                }
                            }
                        } catch (Exception e1) {
                            Log.e(TAG, "解析原始响应出错: " + e1.getMessage());
                        }
                    }
                    
                    // 从待同步列表中移除
                    pendingNotes.remove(finalNoteId);
                    
                    // 如果解析失败或字段缺失，使用原始笔记的值
                    if (responseNote.getId() == null || responseNote.getId().isEmpty()) {
                        responseNote.setId(originalNote.getId());
                        Log.d(TAG, "服务器响应没有ID，使用原始ID: " + originalNote.getId());
                    }
                    
                    if (responseNote.getTitle() == null || responseNote.getTitle().isEmpty()) {
                        responseNote.setTitle(originalNote.getTitle());
                        Log.d(TAG, "服务器响应没有标题，使用原始标题: " + originalNote.getTitle());
                    }
                    
                    if (responseNote.getContent() == null || responseNote.getContent().isEmpty()) {
                        responseNote.setContent(originalNote.getContent());
                        Log.d(TAG, "服务器响应没有内容，使用原始内容");
                    }
                    
                    if (responseNote.getSubject() == null || responseNote.getSubject().isEmpty()) {
                        responseNote.setSubject(originalNote.getSubject());
                        Log.d(TAG, "服务器响应没有科目，使用原始科目: " + originalNote.getSubject());
                    } else if (!responseNote.getSubject().equals(originalNote.getSubject()) && 
                               originalNote.getSubject() != null && 
                               !originalNote.getSubject().isEmpty()) {
                        // 如果服务器返回的科目与原始科目不同，记录日志并优先使用原始科目
                        Log.d(TAG, "服务器返回的科目(" + responseNote.getSubject() + ")与原始科目(" + 
                              originalNote.getSubject() + ")不同，使用原始科目");
                        responseNote.setSubject(originalNote.getSubject());
                    }
                    
                    if (responseNote.getUserId() == null || responseNote.getUserId().isEmpty()) {
                        responseNote.setUserId(originalNote.getUserId());
                        Log.d(TAG, "服务器响应没有用户ID，使用原始用户ID: " + originalNote.getUserId());
                    }
                    
                    // 更新服务器笔记列表
                    updateServerNotesList(responseNote);
                    
                    // 同步成功回调
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    // 处理404错误，创建新笔记
                    if (response.code() == 404) {
                        Log.d(TAG, "笔记不存在，尝试创建新笔记");
                        // 新笔记，没有ID，让服务器生成ID
                        serverNote.setId(null);
                        createNewNote(serverNote, originalNote, callback);
                    } else {
                        String errorMessage = "更新笔记失败: " + ApiClient.getErrorMessage(response) + 
                                              ", 状态码: " + response.code();
                        Log.e(TAG, errorMessage);
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                String errorMessage = "同步笔记失败: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * 创建新笔记
     */
    private void createNewNote(Note serverNote, Note originalNote, SyncCallback callback) {
        // 新笔记，没有ID，让服务器生成ID
        serverNote.setId(null);
        Log.d(TAG, "新笔记，使用创建笔记的方式同步到服务器");
        
        // 获取科目对应的categoryId并记录日志
        Integer categoryId = originalNote.getCategoryIdFromSubject();
        Log.d(TAG, "新笔记科目: " + originalNote.getSubject() + ", 对应categoryId: " + categoryId);
        
        // 将Note对象转换为Map并添加categoryId
        Gson gson = new Gson();
        String noteJson = gson.toJson(serverNote);
        Map<String, Object> noteMap = gson.fromJson(noteJson, new TypeToken<Map<String, Object>>(){}.getType());
        noteMap = SyncUtils.addCategoryIdToRequest(noteMap, originalNote);
        
        // 检查Map中是否正确添加了categoryId
        Log.d(TAG, "发送到服务器的请求Map: " + noteMap.toString());
        
        Call<Note> createCall = apiService.createNoteWithMap(noteMap);
        createCall.enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 创建用于解析的笔记对象
                    Note responseNote = new Note();
                    
                    try {
                        // 将响应体转为JSON字符串
                        String responseBodyString = gson.toJson(response.body());
                        
                        // 尝试处理外层JSON响应
                        JSONObject jsonResponse = new JSONObject(responseBodyString);
                        Log.d(TAG, "服务器响应: " + responseBodyString);
                        
                        // 检查是否包含note字段（服务器嵌套响应）
                        if (jsonResponse.has("note")) {
                            // 获取嵌套的note对象
                            JSONObject noteObject = jsonResponse.getJSONObject("note");
                            
                            // 提取各字段并设置到responseNote
                            if (noteObject.has("id")) {
                                String idStr = String.valueOf(noteObject.get("id"));
                                responseNote.setId(SyncUtils.normalizeId(idStr));
                                Log.d(TAG, "服务器返回的笔记ID: " + responseNote.getId());
                            }
                            
                            if (noteObject.has("title")) {
                                responseNote.setTitle(noteObject.getString("title"));
                            }
                            
                            if (noteObject.has("content")) {
                                responseNote.setContent(noteObject.getString("content"));
                            }
                            
                            if (noteObject.has("categoryId")) {
                                Object categoryId = noteObject.get("categoryId");
                                // 直接设置categoryId字段
                                if (categoryId instanceof Number) {
                                    responseNote.setCategoryId(((Number) categoryId).intValue());
                                } else if (categoryId instanceof String) {
                                    try {
                                        responseNote.setCategoryId(Integer.parseInt((String) categoryId));
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "无法将categoryId转换为整数: " + categoryId);
                                    }
                                } else {
                                    Log.d(TAG, "使用categoryId设置科目: " + categoryId);
                                    responseNote.setSubjectFromCategoryId(categoryId);
                                }
                            }
                            
                            if (noteObject.has("userId")) {
                                String userIdStr = String.valueOf(noteObject.get("userId"));
                                responseNote.setUserId(SyncUtils.normalizeId(userIdStr));
                            }
                            
                            Log.d(TAG, "成功解析服务器返回的笔记数据: ID=" + responseNote.getId() + 
                                  ", 标题=" + responseNote.getTitle() + 
                                  ", 用户ID=" + responseNote.getUserId() + 
                                  ", 科目=" + responseNote.getSubject());
                        } else {
                            // 尝试直接解析响应体本身，可能是直接返回的笔记对象
                            try {
                                // 如果响应本身不包含note字段，尝试检查响应是否有完整的笔记结构
                                if (jsonResponse.has("id")) {
                                    String idStr = String.valueOf(jsonResponse.get("id"));
                                    responseNote.setId(SyncUtils.normalizeId(idStr));
                                    Log.d(TAG, "直接从响应解析的笔记ID: " + responseNote.getId());
                                }
                                
                                if (jsonResponse.has("title")) {
                                    responseNote.setTitle(jsonResponse.getString("title"));
                                }
                                
                                if (jsonResponse.has("content")) {
                                    responseNote.setContent(jsonResponse.getString("content"));
                                }
                                
                                if (jsonResponse.has("categoryId")) {
                                    Object categoryId = jsonResponse.get("categoryId");
                                    // 直接设置categoryId字段
                                    if (categoryId instanceof Number) {
                                        responseNote.setCategoryId(((Number) categoryId).intValue());
                                    } else if (categoryId instanceof String) {
                                        try {
                                            responseNote.setCategoryId(Integer.parseInt((String) categoryId));
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "无法将categoryId转换为整数: " + categoryId);
                                        }
                                    } else {
                                        Log.d(TAG, "使用categoryId设置科目: " + categoryId);
                                        responseNote.setSubjectFromCategoryId(categoryId);
                                    }
                                }
                                
                                if (jsonResponse.has("userId")) {
                                    String userIdStr = String.valueOf(jsonResponse.get("userId"));
                                    responseNote.setUserId(SyncUtils.normalizeId(userIdStr));
                                }
                                
                                if (responseNote.getId() != null) {
                                    Log.d(TAG, "成功从直接响应中解析笔记数据");
                                } else {
                                    Log.e(TAG, "服务器响应中没有note字段且不是完整笔记对象: " + responseBodyString);
                                    
                                    // 尝试使用ApiClient获取原始响应内容
                                    String rawContent = ApiClient.getRawResponseBodyAsString(response);
                                    if (rawContent != null && !rawContent.isEmpty()) {
                                        Log.d(TAG, "尝试解析原始响应内容: " + rawContent);
                                        
                                        try {
                                            JSONObject rawJson = new JSONObject(rawContent);
                                            if (rawJson.has("note")) {
                                                JSONObject noteObject = rawJson.getJSONObject("note");
                                                
                                                if (noteObject.has("id")) {
                                                    String idStr = String.valueOf(noteObject.get("id"));
                                                    responseNote.setId(SyncUtils.normalizeId(idStr));
                                                    Log.d(TAG, "从原始响应解析的笔记ID: " + responseNote.getId());
                                                }
                                                
                                                if (noteObject.has("title")) {
                                                    responseNote.setTitle(noteObject.getString("title"));
                                                }
                                                
                                                if (noteObject.has("content")) {
                                                    responseNote.setContent(noteObject.getString("content"));
                                                }
                                                
                                                if (noteObject.has("categoryId")) {
                                                    // 使用categoryId设置科目
                                                    Object categoryId = noteObject.get("categoryId");
                                                    // 直接设置categoryId字段
                                                    if (categoryId instanceof Number) {
                                                        responseNote.setCategoryId(((Number) categoryId).intValue());
                                                    } else if (categoryId instanceof String) {
                                                        try {
                                                            responseNote.setCategoryId(Integer.parseInt((String) categoryId));
                                                        } catch (NumberFormatException e) {
                                                            Log.e(TAG, "无法将categoryId转换为整数: " + categoryId);
                                                        }
                                                    } else {
                                                        responseNote.setSubjectFromCategoryId(categoryId);
                                                        Log.d(TAG, "从原始响应解析的categoryId(其他类型): " + categoryId + " -> 科目: " + responseNote.getSubject());
                                                    }
                                                }
                                                
                                                if (noteObject.has("userId")) {
                                                    String userIdStr = String.valueOf(noteObject.get("userId"));
                                                    responseNote.setUserId(SyncUtils.normalizeId(userIdStr));
                                                }
                                                
                                                Log.d(TAG, "成功从原始响应解析笔记数据");
                                            }
                                        } catch (Exception e3) {
                                            Log.e(TAG, "解析原始响应内容失败: " + e3.getMessage(), e3);
                                        }
                                    }
                                }
                            } catch (Exception e2) {
                                Log.e(TAG, "尝试直接解析响应体失败: " + e2.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析服务器响应出错: " + e.getMessage(), e);
                        
                        // 尝试使用另一种方式解析原始响应
                        try {
                            String rawResponse = response.raw().toString();
                            Log.d(TAG, "尝试解析原始响应: " + rawResponse);
                            
                            // 尝试直接从响应中获取note字段
                            JSONObject responseJson = null;
                            try {
                                // 从response.body()获取JSON，而不是response.raw().body()
                                responseJson = new JSONObject(gson.toJson(response.body()));
                                Log.d(TAG, "从response.body()获取的JSON: " + responseJson);
                                
                                if (responseJson.has("note")) {
                                    JSONObject noteObject = responseJson.getJSONObject("note");
                                    if (noteObject.has("id")) {
                                        String idStr = String.valueOf(noteObject.get("id"));
                                        responseNote.setId(SyncUtils.normalizeId(idStr));
                                        Log.d(TAG, "从response.body()解析的笔记ID: " + responseNote.getId());
                                    }
                                }
                            } catch (Exception e2) {
                                Log.e(TAG, "解析response.body()失败: " + e2.getMessage());
                            }
                            
                            // 尝试处理Retrofit响应
                            if (response.body() instanceof Map) {
                                try {
                                    Map<String, Object> responseMap = (Map<String, Object>) response.body();
                                    if (responseMap.containsKey("note")) {
                                        Object noteObj = responseMap.get("note");
                                        if (noteObj instanceof Map) {
                                            Map<String, Object> noteMap = (Map<String, Object>) noteObj;
                                            if (noteMap.containsKey("id")) {
                                                String idStr = String.valueOf(noteMap.get("id"));
                                                responseNote.setId(SyncUtils.normalizeId(idStr));
                                                Log.d(TAG, "从Map解析的笔记ID: " + responseNote.getId());
                                            }
                                        }
                                    }
                                } catch (Exception e2) {
                                    Log.e(TAG, "处理Map响应失败: " + e2.getMessage());
                                }
                            }
                        } catch (Exception e1) {
                            Log.e(TAG, "解析原始响应出错: " + e1.getMessage());
                        }
                    }
                    
                    // 如果解析失败或字段缺失，使用原始笔记的值
                    if (responseNote.getId() == null || responseNote.getId().isEmpty()) {
                        Log.d(TAG, "无法从服务器响应提取ID，使用临时ID");
                        // 不设置ID，等待后续同步获取正确ID
                    }
                    
                    if (responseNote.getTitle() == null || responseNote.getTitle().isEmpty()) {
                        responseNote.setTitle(originalNote.getTitle());
                        Log.d(TAG, "服务器响应没有标题，使用原始标题: " + originalNote.getTitle());
                    }
                    
                    if (responseNote.getContent() == null || responseNote.getContent().isEmpty()) {
                        responseNote.setContent(originalNote.getContent());
                        Log.d(TAG, "服务器响应没有内容，使用原始内容");
                    }
                    
                    if (responseNote.getSubject() == null || responseNote.getSubject().isEmpty()) {
                        responseNote.setSubject(originalNote.getSubject());
                        Log.d(TAG, "服务器响应没有科目，使用原始科目: " + originalNote.getSubject());
                    } else if (!responseNote.getSubject().equals(originalNote.getSubject()) && 
                               originalNote.getSubject() != null && 
                               !originalNote.getSubject().isEmpty()) {
                        // 如果服务器返回的科目与原始科目不同，记录日志并优先使用原始科目
                        Log.d(TAG, "服务器返回的科目(" + responseNote.getSubject() + ")与原始科目(" + 
                              originalNote.getSubject() + ")不同，使用原始科目");
                        responseNote.setSubject(originalNote.getSubject());
                    }
                    
                    if (responseNote.getUserId() == null || responseNote.getUserId().isEmpty()) {
                        responseNote.setUserId(originalNote.getUserId());
                        Log.d(TAG, "服务器响应没有用户ID，使用原始用户ID: " + originalNote.getUserId());
                    }
                    
                    // 更新本地笔记ID
                    if (responseNote.getId() != null && !responseNote.getId().isEmpty()) {
                        originalNote.setId(responseNote.getId());
                        
                        // 保存更新后的笔记到本地
                        localDataManager.saveNote(originalNote);
                        Log.d(TAG, "已更新本地笔记ID: " + originalNote.getId());
                    }
                    
                    // 更新服务器笔记列表
                    updateServerNotesList(responseNote);
                    
                    // 同步成功回调
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    String errorMessage = "创建笔记失败: " + ApiClient.getErrorMessage(response) + 
                                          ", 状态码: " + response.code();
                    Log.e(TAG, errorMessage);
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                String errorMessage = "创建笔记失败: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * 处理服务器响应，修正可能缺失的字段
     */
    private void processServerResponse(Note responseNote, Note originalNote) {
        // 检查响应中是否有"note"字段（可能包含在嵌套对象中）
        if (responseNote.getId() == null && responseNote.getTitle() == null) {
            try {
                // 尝试从嵌套JSON获取笔记信息
                String responseJson = new Gson().toJson(responseNote);
                Log.d(TAG, "尝试解析嵌套响应: " + responseJson);
                
                org.json.JSONObject jsonObject = new org.json.JSONObject(responseJson);
                if (jsonObject.has("note")) {
                    org.json.JSONObject noteJson = jsonObject.getJSONObject("note");
                    
                    if (noteJson.has("id")) {
                        // 确保ID不含小数点
                        String idValue = String.valueOf(noteJson.get("id"));
                        if (idValue.contains(".")) {
                            try {
                                double doubleValue = Double.parseDouble(idValue);
                                responseNote.setId(String.valueOf((long)doubleValue));
                            } catch (Exception e) {
                                responseNote.setId(idValue.substring(0, idValue.indexOf('.')));
                            }
                        } else {
                            responseNote.setId(idValue);
                        }
                    }
                    if (noteJson.has("title")) responseNote.setTitle(noteJson.getString("title"));
                    if (noteJson.has("content")) responseNote.setContent(noteJson.getString("content"));
                    
                    // 优先处理categoryId，然后再处理subject
                    if (noteJson.has("categoryId") && !noteJson.isNull("categoryId")) {
                        // 如果有categoryId，根据categoryId设置科目
                        Object categoryIdObj = noteJson.get("categoryId");
                        responseNote.setSubjectFromCategoryId(categoryIdObj);
                        Log.d(TAG, "根据categoryId设置科目: " + categoryIdObj + " -> " + responseNote.getSubject());
                        
                        // 如果设置后仍是其他，但原始科目不是其他且原始科目不为空，则保留原始科目
                        if ((responseNote.getSubject() == null || responseNote.getSubject().equals("其他")) && 
                            originalNote.getSubject() != null && !originalNote.getSubject().equals("其他")) {
                            responseNote.setSubject(originalNote.getSubject());
                            Log.d(TAG, "服务器返回'其他'科目，保留原始科目: " + originalNote.getSubject());
                        }
                    } else if (noteJson.has("subject")) {
                        // 如果有subject字段但没有categoryId
                        responseNote.setSubject(noteJson.getString("subject"));
                        
                        // 如果服务器返回的科目为空或"其他"，但原始科目不为空且不是"其他"，则保留原始科目
                        String serverSubject = noteJson.getString("subject");
                        if ((serverSubject == null || serverSubject.isEmpty() || serverSubject.equals("其他")) && 
                            originalNote.getSubject() != null && !originalNote.getSubject().isEmpty() && 
                            !originalNote.getSubject().equals("其他")) {
                            responseNote.setSubject(originalNote.getSubject());
                            Log.d(TAG, "服务器返回空或'其他'科目，保留原始科目: " + originalNote.getSubject());
                        }
                    } else {
                        // 如果服务器响应没有subject和categoryId字段，保留原始笔记的科目
                        responseNote.setSubject(originalNote.getSubject());
                        Log.d(TAG, "服务器响应没有科目相关字段，保留原始科目: " + originalNote.getSubject());
                    }
                    
                    if (noteJson.has("userId")) {
                        // 确保用户ID不含小数点
                        String userIdValue = String.valueOf(noteJson.get("userId"));
                        if (userIdValue.contains(".")) {
                            try {
                                double doubleValue = Double.parseDouble(userIdValue);
                                responseNote.setUserId(String.valueOf((long)doubleValue));
                            } catch (Exception e) {
                                responseNote.setUserId(userIdValue.substring(0, userIdValue.indexOf('.')));
                            }
                        } else {
                            responseNote.setUserId(userIdValue);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "解析嵌套响应失败: " + e.getMessage(), e);
            }
        }
        
        // 确保保留关键数据，如果服务器响应缺失
        if (responseNote.getId() == null || responseNote.getId().isEmpty()) {
            responseNote.setId(originalNote.getId());
            Log.d(TAG, "服务器响应没有ID，保留原始ID: " + originalNote.getId());
        }
        if (responseNote.getTitle() == null || responseNote.getTitle().isEmpty()) {
            responseNote.setTitle(originalNote.getTitle());
            Log.d(TAG, "服务器响应没有标题，保留原始标题: " + originalNote.getTitle());
        }
        if (responseNote.getContent() == null || responseNote.getContent().isEmpty()) {
            responseNote.setContent(originalNote.getContent());
            Log.d(TAG, "服务器响应没有内容，保留原始内容");
        }
        
        // 强化科目处理逻辑
        if (responseNote.getSubject() == null || responseNote.getSubject().isEmpty()) {
            responseNote.setSubject(originalNote.getSubject());
            Log.d(TAG, "服务器响应没有科目，保留原始科目: " + originalNote.getSubject());
        } else if (responseNote.getSubject().equals("其他") && 
                  originalNote.getSubject() != null && 
                  !originalNote.getSubject().isEmpty() && 
                  !originalNote.getSubject().equals("其他")) {
            // 如果服务器返回"其他"，但原始笔记有指定科目，则保留原始科目
            responseNote.setSubject(originalNote.getSubject());
            Log.d(TAG, "服务器响应科目为'其他'，保留原始科目: " + originalNote.getSubject());
        }
        
        if (responseNote.getUserId() == null || responseNote.getUserId().isEmpty()) {
            responseNote.setUserId(originalNote.getUserId());
            Log.d(TAG, "服务器响应没有用户ID，保留原始用户ID: " + originalNote.getUserId());
        }
    }
    
    /**
     * 更新serverNotes列表中的笔记
     */
    public void updateServerNotesList(Note note) {
        if (note == null) {
            Log.e(TAG, "无法更新serverNotes列表：笔记为空");
            return;
        }
        
        // 处理ID为空的情况
        if (note.getId() == null || note.getId().isEmpty()) {
            Log.e(TAG, "笔记ID为空，尝试生成临时ID或使用标题作为标识");
            
            // 如果ID为空但有标题，使用标题作为临时标识
            if (note.getTitle() != null && !note.getTitle().isEmpty()) {
                // 临时ID格式: temp_title_timestamp
                String tempId = "temp_" + note.getTitle() + "_" + System.currentTimeMillis();
                note.setId(tempId);
                Log.d(TAG, "为笔记生成临时ID: " + tempId);
            } else {
                // 如果连标题都没有，使用时间戳作为临时ID
                String tempId = "temp_" + System.currentTimeMillis();
                note.setId(tempId);
                Log.d(TAG, "为无标题笔记生成临时ID: " + tempId);
            }
        }
        
        // 处理数字ID的情况(服务器可能返回数字ID)
        try {
            if (note.getId().matches("\\d+")) {
                Log.d(TAG, "笔记ID是数字: " + note.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "处理笔记ID出错: " + e.getMessage());
        }
        
        boolean found = false;
        
        // 查找并更新现有笔记
        for (int i = 0; i < serverNotes.size(); i++) {
            Note existingNote = serverNotes.get(i);
            if (existingNote.getId() != null && existingNote.getId().equals(note.getId())) {
                // 保留已存在笔记中有价值的信息，如果新笔记中缺失
                if (note.getTitle() == null || note.getTitle().isEmpty()) {
                    note.setTitle(existingNote.getTitle());
                }
                if (note.getContent() == null || note.getContent().isEmpty()) {
                    note.setContent(existingNote.getContent());
                }
                if (note.getSubject() == null || note.getSubject().isEmpty()) {
                    note.setSubject(existingNote.getSubject());
                    Log.d(TAG, "保留原有科目信息: " + existingNote.getSubject() + ", 笔记ID: " + note.getId());
                } else {
                    Log.d(TAG, "更新科目信息: " + note.getSubject() + ", 笔记ID: " + note.getId());
                }
                if (note.getUserId() == null || note.getUserId().isEmpty()) {
                    note.setUserId(existingNote.getUserId());
                }
                
                serverNotes.set(i, note);
                found = true;
                Log.d(TAG, "更新serverNotes列表中ID为 " + note.getId() + " 的笔记");
                break;
            }
        }
        
        // 如果未找到现有笔记，添加到列表
        if (!found) {
            // 确保新笔记有科目信息
            if (note.getSubject() == null || note.getSubject().isEmpty()) {
                note.setSubject("其他");
                Log.d(TAG, "新笔记没有科目，默认设置为'其他', 笔记ID: " + note.getId());
            }
            
            serverNotes.add(note);
            Log.d(TAG, "将新笔记添加到serverNotes列表，ID: " + note.getId() + ", 标题: " + note.getTitle() + ", 科目: " + note.getSubject());
        }
    }
    
    /**
     * 删除笔记
     */
    public void deleteNote(String noteId, SyncCallback callback) {
        if (noteId == null || noteId.isEmpty()) {
            if (callback != null) {
                callback.onError("笔记ID为空");
            }
            return;
        }
        
        try {
            // 处理可能是浮点数形式的ID (如: "10.0")
            final String finalNoteId = SyncUtils.normalizeId(noteId);
            
            // 首先从本地服务器缓存中删除所有匹配ID的笔记
            // 注意：需要处理浮点数形式的ID进行比较
            boolean removedFromCache = false;
            for (Iterator<Note> iterator = serverNotes.iterator(); iterator.hasNext();) {
                Note note = iterator.next();
                String currentId = note.getId();
                // 处理整数ID和浮点数ID的情况
                boolean matches = finalNoteId.equals(currentId) || 
                    (currentId != null && currentId.contains(".") && 
                     finalNoteId.equals(currentId.substring(0, currentId.indexOf('.'))));
                
                if (matches) {
                    iterator.remove();
                    removedFromCache = true;
                    Log.d(TAG, "从缓存中移除笔记: " + currentId);
                }
            }
            
            if (!removedFromCache) {
                Log.w(TAG, "在服务器缓存中未找到要删除的笔记ID: " + finalNoteId);
            }
            
            // 调用API删除
            Log.d(TAG, "正在从服务器删除笔记: " + finalNoteId);
            apiService.deleteNote(finalNoteId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "笔记删除成功: " + finalNoteId);
                        
                        // 从待同步列表中移除
                        pendingNotes.remove(finalNoteId);
                        
                        // 再次检查并从本地服务器缓存中移除所有匹配的笔记
                        for (Iterator<Note> iterator = serverNotes.iterator(); iterator.hasNext();) {
                            Note note = iterator.next();
                            String noteId = note.getId();
                            // 处理可能是浮点数形式的ID (如: "38.0")
                            if (noteId != null && (noteId.equals(finalNoteId) || 
                                (noteId.contains(".") && noteId.substring(0, noteId.indexOf('.')).equals(finalNoteId)))) {
                                iterator.remove();
                                Log.d(TAG, "从服务器笔记缓存中移除笔记: " + noteId);
                            }
                        }
                        
                        // 从本地数据库中删除
                        localDataManager.deleteNote(finalNoteId);
                        
                        // 回调成功
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        String errorMsg = "删除笔记失败，状态码: " + response.code();
                        Log.e(TAG, errorMsg);
                        
                        // 检查是否是404（笔记可能已经被删除）
                        if (response.code() == 404) {
                            Log.d(TAG, "笔记不存在或已被删除: " + finalNoteId);
                            // 从本地数据库中删除
                            localDataManager.deleteNote(finalNoteId);
                            // 仍然视为成功，因为用户的目标是删除笔记
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            // 其他错误
                            if (callback != null) {
                                callback.onError(errorMsg);
                            }
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    String errorMsg = "删除笔记请求失败: " + t.getMessage();
                    Log.e(TAG, errorMsg);
                    
                    // 如果是网络错误，自动从本地删除以保持一致性
                    if (t instanceof java.io.IOException) {
                        Log.d(TAG, "网络错误，从本地删除笔记: " + finalNoteId);
                        localDataManager.deleteNote(finalNoteId);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            });
        } catch (Exception e) {
            String errorMsg = "删除笔记时出错: " + e.getMessage();
            Log.e(TAG, errorMsg);
            if (callback != null) {
                callback.onError(errorMsg);
            }
        }
    }
    
    /**
     * 从服务器获取所有笔记列表
     */
    public void getAllServerNotes(final SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接，请检查网络设置");
            }
            return;
        }
        
        Log.d(TAG, "正在从服务器获取所有笔记");
        
        apiService.getAllNotes().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> responseData = response.body();
                        
                        if (responseData.containsKey("notes")) {
                            // 清空已有的服务器笔记列表
                            serverNotes.clear();
                            
                            // 获取笔记列表
                            Object notesObj = responseData.get("notes");
                            
                            // 使用Gson将Object转换为List<Note>
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Note>>(){}.getType();
                            String notesJson = gson.toJson(notesObj);
                            List<Note> notes = gson.fromJson(notesJson, listType);
                            
                            // 处理笔记列表
                            if (notes != null) {
                                Log.d(TAG, "从服务器获取到 " + notes.size() + " 条笔记");
                                
                                // 遍历笔记列表，处理每一条笔记
                                for (Note note : notes) {
                                    // 处理ID
                                    SyncUtils.normalizeNoteIds(note);
                                    
                                    // 处理科目信息
                                    SyncUtils.processServerNoteFields(note, gson);
                                    
                                    // 添加到服务器笔记列表
                                    serverNotes.add(note);
                                    
                                    Log.d(TAG, "添加服务器笔记: ID=" + note.getId() + 
                                          ", 标题=" + note.getTitle() + 
                                          ", 用户ID=" + note.getUserId() + 
                                          ", 科目=" + note.getSubject());
                                }
                                
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            } else {
                                Log.d(TAG, "从服务器获取到的笔记列表为空");
                                if (callback != null) {
                                    callback.onSuccess(); // 虽然列表为空，但API调用成功
                                }
                            }
                        } else {
                            String errorMessage = "服务器响应中没有笔记列表";
                            Log.e(TAG, errorMessage);
                            if (callback != null) {
                                callback.onError(errorMessage);
                            }
                        }
                    } catch (Exception e) {
                        String errorMessage = "处理服务器笔记列表出错: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                } else {
                    String errorMessage = "获取笔记列表失败: " + ApiClient.getErrorMessage(response) + 
                                          ", 状态码: " + response.code();
                    Log.e(TAG, errorMessage);
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                String errorMessage = "获取笔记列表出错: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * 同步本地和服务器之间的笔记差异
     * 确保本地和服务器笔记数据一致
     */
    public void syncNotesWithServer(final com.lianxiangdaimaowang.lumina.sync.SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            Log.d(TAG, "无网络连接，无法同步服务器笔记");
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }

        getAllServerNotes(new com.lianxiangdaimaowang.lumina.sync.SyncCallback() {
            @Override
            public void onSuccess() {
                // 获取到服务器笔记后，更新本地数据库
                syncLocalDatabaseWithServer(callback);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "从服务器获取笔记失败: " + errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }

    /**
     * 同步本地数据库与服务器笔记
     * 此方法会确保本地数据库中只有服务器上存在的笔记
     */
    public void syncLocalDatabaseWithServer(final com.lianxiangdaimaowang.lumina.sync.SyncCallback callback) {
        try {
            // 获取本地所有笔记
            List<Note> localNotes = localDataManager.getAllNotes();
            
            // 将服务器笔记转换为Map，以便快速查找
            Map<String, Note> serverNoteMap = new HashMap<>();
            for (Note serverNote : serverNotes) {
                serverNoteMap.put(serverNote.getId(), serverNote);
            }
            
            // 检查本地笔记是否存在于服务器上
            for (Note localNote : localNotes) {
                String normalizedId = SyncUtils.normalizeId(localNote.getId());
                
                // 如果本地笔记不在服务器上，则从本地删除
                if (!serverNoteMap.containsKey(normalizedId)) {
                    Log.d(TAG, "本地笔记 " + normalizedId + " 不在服务器上，从本地删除");
                    localDataManager.deleteNote(localNote.getId());
                }
            }
            
            // 保存服务器笔记到本地，确保所有服务器笔记都在本地
            for (Note serverNote : serverNotes) {
                localDataManager.saveNote(serverNote);
            }
            
            Log.d(TAG, "完成本地数据库与服务器笔记同步");
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "同步本地数据库与服务器笔记时出错: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("同步本地数据库失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 将Note对象转换为NoteEntity对象
     */
    public NoteEntity convertNoteToEntity(Note note) {
        NoteEntity entity = new NoteEntity();
        
        // 如果ID是数字字符串，转换为long类型
        if (note.getId() != null && !note.getId().isEmpty()) {
            try {
                // 处理可能是浮点数形式的ID (如: "10.0")
                if (note.getId().contains(".")) {
                    double doubleValue = Double.parseDouble(note.getId());
                    entity.setId((long) doubleValue);
                } else {
                    entity.setId(Long.parseLong(note.getId()));
                }
            } catch (NumberFormatException e) {
                // 如果转换失败，使用默认的自动生成ID
                Log.e(TAG, "无法将笔记ID转换为数字: " + note.getId(), e);
            }
        }
        
        entity.setTitle(note.getTitle());
        entity.setContent(note.getContent());
        entity.setSubject(note.getSubject());
        entity.setCreationDate(note.getCreatedDate());
        entity.setLastModifiedDate(note.getLastModifiedDate());
        entity.setUserId(note.getUserId());
        
        // 设置附件列表
        if (note.getAttachmentPaths() != null) {
            entity.setAttachments(new ArrayList<>(note.getAttachmentPaths()));
        }
        
        return entity;
    }

    /**
     * 从服务器获取笔记列表
     * 向后兼容的旧方法，内部调用getAllServerNotes
     */
    public void fetchNotesFromServer(final SyncCallback callback) {
        getAllServerNotes(callback);
    }
} 