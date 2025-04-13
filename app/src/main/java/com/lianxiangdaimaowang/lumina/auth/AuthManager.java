package com.lianxiangdaimaowang.lumina.auth;

import android.util.Log;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;

/**
 * 认证管理器，处理用户认证和授权
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    private LocalDataManager localDataManager;
    
    public AuthManager(LocalDataManager localDataManager) {
        this.localDataManager = localDataManager;
    }

    /**
     * 从JWT令牌中提取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    private String extractUserIdFromToken(String token) {
        try {
            // JWT令牌格式: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                // 解码payload部分(Base64)
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT));
                Log.d(TAG, "JWT令牌负载: " + payload);
                
                // 首先尝试从sub字段提取用户ID
                if (payload.contains("\"sub\":")) {
                    int start = payload.indexOf("\"sub\":") + 6;
                    int end = payload.indexOf(",", start);
                    if (end == -1) end = payload.indexOf("}", start);
                    String serverUserId = payload.substring(start, end).replace("\"", "").trim();
                    
                    Log.d(TAG, "从JWT令牌中提取的服务器用户ID: " + serverUserId);
                    
                    // 直接返回服务器用户ID，确保使用服务器分配的ID进行同步
                    return serverUserId;
                }
                
                // 如果没有sub字段，尝试从id字段提取
                if (payload.contains("\"id\":")) {
                    int start = payload.indexOf("\"id\":") + 5;
                    int end = payload.indexOf(",", start);
                    if (end == -1) end = payload.indexOf("}", start);
                    String serverUserId = payload.substring(start, end).replace("\"", "").trim();
                    
                    Log.d(TAG, "从JWT令牌的id字段提取的服务器用户ID: " + serverUserId);
                    
                    return serverUserId;
                }
                
                // 尝试从userId字段提取
                if (payload.contains("\"userId\":")) {
                    int start = payload.indexOf("\"userId\":") + 9;
                    int end = payload.indexOf(",", start);
                    if (end == -1) end = payload.indexOf("}", start);
                    String serverUserId = payload.substring(start, end).replace("\"", "").trim();
                    
                    Log.d(TAG, "从JWT令牌的userId字段提取的服务器用户ID: " + serverUserId);
                    
                    return serverUserId;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析JWT令牌失败: " + e.getMessage());
        }
        
        // 如果无法从令牌中提取，则返回null
        Log.d(TAG, "无法从令牌提取ID，返回null");
        return null;
    }

    /**
     * 登录成功后处理JWT令牌
     * @param token JWT令牌
     * @param username 用户名
     */
    private void handleSuccessfulLogin(String token, String username) {
        try {
            // 保存令牌
            localDataManager.saveAuthToken(token);
            
            // 从令牌中提取用户ID
            String userId = extractUserIdFromToken(token);
            
            // 如果提取的ID为null，使用空字符串
            if (userId == null) {
                userId = "";
                Log.d(TAG, "没有获取到有效用户ID，使用空ID");
            }
            
            // 保存用户信息（包含用户ID）
            localDataManager.saveCurrentUserWithId(username, userId);
            Log.d(TAG, "登录成功，保存用户信息: 用户名=" + username + ", 用户ID=" + userId);
            Log.d(TAG, "注意：用户名仅作为显示用途，所有同步操作都将使用服务器分配的用户ID");
            
            // 设置登录状态
            localDataManager.signIn();
        } catch (Exception e) {
            Log.e(TAG, "处理登录响应时出错", e);
        }
    }
} 