package com.lianxiangdaimaowang.lumina.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.api.ApiClient;
import com.lianxiangdaimaowang.lumina.api.ApiService;
import com.lianxiangdaimaowang.lumina.model.User;
import com.lianxiangdaimaowang.lumina.utils.NetworkUtils;
import com.lianxiangdaimaowang.lumina.utils.ProgressDialogUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 注册页面
 */
public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private View tvGoLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_register_username);
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register_submit);
        tvGoLogin = findViewById(R.id.tv_go_login);
    }

    private void setupListeners() {
        // 注册按钮点击事件
        btnRegister.setOnClickListener(v -> register());
        
        // 返回登录页面
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        // 获取输入内容
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 验证输入
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查账号格式
        if (!isValidUsername(username)) {
            Toast.makeText(this, "用户名格式不正确，只能包含字母、数字和下划线", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查密码格式
        if (!isValidPassword(password)) {
            Toast.makeText(this, "密码格式不正确，长度至少6位", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查邮箱格式（如果提供）
        if (!email.isEmpty() && !isValidEmail(email)) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查网络连接
        if (!NetworkUtils.isNetworkConnected(this)) {
            Toast.makeText(this, "无网络连接，请检查网络设置", Toast.LENGTH_LONG).show();
            return;
        }

        // 显示进度对话框
        ProgressDialogUtils.showProgress(this, "注册中...");
        
        // 创建注册数据
        Map<String, String> registerData = new HashMap<>();
        registerData.put("username", username);
        registerData.put("password", password);
        registerData.put("email", email.isEmpty() ? username + "@example.com" : email);
        registerData.put("status", "1");
        
        // 调用API注册
        ApiService apiService = ApiClient.getApiService(this);
        Call<Map<String, Object>> call = apiService.register(registerData);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                ProgressDialogUtils.hideProgress();
                if (response.isSuccessful() && response.body() != null) {
                    // 注册成功
                    Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                    
                    // 返回登录页面并传递用户名
                    Intent intent = new Intent();
                    intent.putExtra("username", username);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    // 注册失败
                    String errorMsg = ApiClient.getErrorMessage(response);
                    Toast.makeText(RegisterActivity.this, "注册失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                ProgressDialogUtils.hideProgress();
                Log.e(TAG, "注册请求失败", t);
                Toast.makeText(RegisterActivity.this, "服务器连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidUsername(String username) {
        // 用户名只能包含字母、数字和下划线
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean isValidPassword(String password) {
        // 密码长度至少6位
        return password.length() >= 6;
    }

    private boolean isValidEmail(String email) {
        // 简单的邮箱格式验证
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
} 