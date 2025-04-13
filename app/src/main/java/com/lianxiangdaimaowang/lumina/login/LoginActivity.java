package com.lianxiangdaimaowang.lumina.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lianxiangdaimaowang.lumina.MainActivity;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.api.ApiClient;
import com.lianxiangdaimaowang.lumina.api.ApiService;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;
import com.lianxiangdaimaowang.lumina.utils.NetworkUtils;
import com.lianxiangdaimaowang.lumina.utils.ProgressDialogUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 登录界面Activity
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_REGISTER = 100;

    private LocalDataManager localDataManager;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private View tvGoRegister;
    private View tvForgotPassword;

    // 注册页面返回结果处理
    private final ActivityResultLauncher<Intent> registerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // 从注册页面获取用户名
                    String username = result.getData().getStringExtra("username");
                    if (username != null && !username.isEmpty()) {
                        // 自动填充用户名
                        etUsername.setText(username);
                        etPassword.requestFocus();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化LocalDataManager
        localDataManager = LocalDataManager.getInstance(this);

        // 初始化视图
        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
    }

    private void setupListeners() {
        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> login());
        
        // 跳转到注册页面
        tvGoRegister.setOnClickListener(v -> navigateToRegister());
        
        // 跳转到忘记密码页面
        tvForgotPassword.setOnClickListener(v -> navigateToForgotPassword());
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        registerLauncher.launch(intent);
    }

    private void navigateToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查网络连接
        if (!NetworkUtils.isNetworkConnected(this)) {
            Toast.makeText(this, "无网络连接，请检查网络设置", Toast.LENGTH_LONG).show();
            return;
        }

        // 显示进度对话框
        ProgressDialogUtils.showProgress(this, "登录中...");

        // 创建登录请求数据
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);

        // 调用API登录
        ApiService apiService = ApiClient.getApiService(this);
        Call<Map<String, Object>> call = apiService.login(loginData);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                ProgressDialogUtils.hideProgress();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 登录成功，保存令牌
                        Map<String, Object> responseData = response.body();
                        Log.d(TAG, "登录响应: " + responseData);
                        
                        // 尝试多种可能的token键名
                        String token = null;
                        if (responseData.containsKey("token")) {
                            token = responseData.get("token").toString();
                        } else if (responseData.containsKey("access_token")) {
                            token = responseData.get("access_token").toString();
                        } else if (responseData.containsKey("id_token")) {
                            token = responseData.get("id_token").toString();
                        } else if (responseData.containsKey("auth_token")) {
                            token = responseData.get("auth_token").toString();
                        }
                        
                        if (token != null && !token.isEmpty()) {
                            Log.d(TAG, "获取到认证令牌: " + token);
                            
                            // 提取服务器用户ID
                            if (responseData.containsKey("user_id") || responseData.containsKey("userId")) {
                                String serverUserId = null;
                                if (responseData.containsKey("user_id")) {
                                    serverUserId = responseData.get("user_id").toString();
                                } else if (responseData.containsKey("userId")) {
                                    serverUserId = responseData.get("userId").toString();
                                }
                                
                                if (serverUserId != null && !serverUserId.isEmpty()) {
                                    Log.d(TAG, "服务器返回的用户ID: " + serverUserId);
                                    
                                    // 从令牌中提取客户端用户ID
                                    String clientUserId = extractUserIdFromToken(token);
                                    if (clientUserId != null && !clientUserId.isEmpty() && !clientUserId.equals(serverUserId)) {
                                        // 保存映射关系
                                        Log.d(TAG, "发现客户端ID和服务器ID不同，保存映射: " + clientUserId + " -> " + serverUserId);
                                        localDataManager.saveUserIdMapping(clientUserId, serverUserId);
                                    }
                                }
                            }
                            
                            // 处理登录成功
                            handleLoginSuccess(token, username);
                        } else {
                            // 没有获取到令牌，可能是响应格式不正确
                            Log.e(TAG, "登录响应中没有找到令牌: " + responseData);
                            Toast.makeText(LoginActivity.this, "登录成功但未获取到令牌", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理登录响应时出错", e);
                        Toast.makeText(LoginActivity.this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 登录失败
                    String errorMsg = ApiClient.getErrorMessage(response);
                    Log.e(TAG, "登录失败，HTTP状态码: " + response.code() + ", 错误信息: " + errorMsg);
                    Toast.makeText(LoginActivity.this, "登录失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                ProgressDialogUtils.hideProgress();
                Log.e(TAG, "登录请求失败", t);
                Toast.makeText(LoginActivity.this, "服务器连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
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
                    
                    // 直接返回服务器用户ID，不再重新分配ID
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
     * 登录成功
     */
    private void handleLoginSuccess(String token, String username) {
        try {
            // 保存认证令牌
            localDataManager.saveAuthToken(token);
            
            // 从令牌中提取用户ID，确保从100001开始
            String userId = extractUserIdFromToken(token);
            
            // 确保总是有用户ID
            if (userId == null || userId.isEmpty()) {
                userId = "100001";
                Log.d(TAG, "没有获取到有效用户ID，使用默认ID: " + userId);
            }
            
            // 保存用户信息（包含用户ID）
            localDataManager.saveCurrentUserWithId(username, userId);
            Log.d(TAG, "登录成功，保存用户信息: 用户名=" + username + ", 用户ID=" + userId);
            
            // 设置登录状态
            localDataManager.signIn();
            
            // 检查是否有服务器用户ID映射
            try {
                // 尝试从响应中提取真实的服务器用户ID
                if (username.equals(userId)) {
                    // 如果用户名和ID相同，可能需要在后续请求中发现真实映射
                    Log.d(TAG, "用户名与ID相同，将在后续请求中检查是否需要ID映射");
                } else {
                    Log.d(TAG, "用户名与ID不同，可能已有明确的用户ID");
                }
            } catch (Exception e) {
                Log.e(TAG, "处理用户ID映射时出错: " + e.getMessage());
            }
            
            // 显示登录成功提示
            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
            
            // 直接跳转到主界面，让MainActivity在后台处理数据同步
            goToMainActivity();
            
        } catch (Exception e) {
            Log.e(TAG, "处理登录响应时出错", e);
            // 显示错误信息
            Toast.makeText(this, "处理登录响应时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMainActivity() {
        // 启动主活动，传递同步标志
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("sync_data", true);  // 告诉MainActivity需要同步数据
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // 关闭当前活动
        finish();
    }
} 