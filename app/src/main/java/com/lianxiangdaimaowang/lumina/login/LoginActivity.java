package com.lianxiangdaimaowang.lumina.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lianxiangdaimaowang.lumina.MainActivity;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.model.User;

/**
 * 登录界面Activity - 简化版，不实现社交登录
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private LocalDataManager localDataManager;
    private Button btnSkip;
    private Button btnGuestLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化LocalDataManager
        localDataManager = LocalDataManager.getInstance(this);

        // 如果用户已经登录过，直接进入主界面
        if (localDataManager.isSignedIn()) {
            startMainActivity();
            finish();
            return;
        }

        // 初始化视图
        initViews();
    }

    private void initViews() {
        btnSkip = findViewById(R.id.btn_skip);
        btnGuestLogin = findViewById(R.id.btn_guest_login);

        // 设置跳过登录按钮点击事件
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipLogin();
            }
        });
        
        // 设置访客登录按钮点击事件
        btnGuestLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipLogin(); // 复用跳过登录的逻辑
            }
        });
    }

    /**
     * 跳过登录
     */
    private void skipLogin() {
        // 使用访客身份
        mockLoginSuccess("guest_" + System.currentTimeMillis(), "访客用户", null, "guest");
    }

    /**
     * 模拟登录成功，保存用户信息并跳转到主界面
     */
    private void mockLoginSuccess(String userId, String username, String avatarUrl, String provider) {
        try {
            // 创建用户对象
            User user = new User(userId, username, avatarUrl);
            
            // 保存用户信息到本地
            localDataManager.saveValue(LocalDataManager.KEY_USER_ID, userId);
            localDataManager.setUsername(username);
            localDataManager.setLoginProvider(provider);
            if (avatarUrl != null) {
                localDataManager.setAvatarUrl(avatarUrl);
            } else {
                localDataManager.setAvatarUrl("https://example.com/default_avatar.png");
            }
            localDataManager.signIn();
            
            // 跳转到主界面
            startMainActivity();
            
        } catch (Exception e) {
            Log.e(TAG, "模拟登录失败", e);
        }
    }
    
    /**
     * 跳转到主界面
     */
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
} 