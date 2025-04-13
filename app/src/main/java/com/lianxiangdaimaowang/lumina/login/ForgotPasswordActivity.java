package com.lianxiangdaimaowang.lumina.login;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import com.lianxiangdaimaowang.lumina.utils.NetworkUtils;
import com.lianxiangdaimaowang.lumina.utils.ProgressDialogUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 忘记密码页面
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";

    private TextInputEditText etAccountEmail;
    private TextInputEditText etVerificationCode;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmNewPassword;
    private MaterialButton btnGetCode;
    private MaterialButton btnResetPassword;
    private View tvBackToLogin;

    private CountDownTimer countDownTimer;
    private boolean isCountingDown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etAccountEmail = findViewById(R.id.et_account_email);
        etVerificationCode = findViewById(R.id.et_verification_code);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        btnGetCode = findViewById(R.id.btn_get_code);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
    }

    private void setupListeners() {
        // 获取验证码按钮点击事件
        btnGetCode.setOnClickListener(v -> getVerificationCode());
        
        // 重置密码按钮点击事件
        btnResetPassword.setOnClickListener(v -> resetPassword());
        
        // 返回登录页面
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void getVerificationCode() {
        // 获取账号或邮箱
        String accountOrEmail = etAccountEmail.getText().toString().trim();
        
        if (accountOrEmail.isEmpty()) {
            Toast.makeText(this, "请输入账号或邮箱", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查网络连接
        if (!NetworkUtils.isNetworkConnected(this)) {
            Toast.makeText(this, "无网络连接，请检查网络设置", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 显示进度对话框
        ProgressDialogUtils.showProgress(this, "发送验证码中...");
        
        // 模拟验证码发送，因为API还不支持
        new Handler().postDelayed(() -> {
            ProgressDialogUtils.hideProgress();
            Toast.makeText(ForgotPasswordActivity.this, "验证码已发送，请使用123456", Toast.LENGTH_SHORT).show();
            
            // 开始倒计时
            startCountDown();
        }, 1500);
        
        /* 真实代码，暂时注释保留
        // 创建请求数据
        Map<String, String> data = new HashMap<>();
        data.put("account", accountOrEmail);
        
        // 调用API获取验证码
        ApiService apiService = ApiClient.getApiService(this);
        Call<Map<String, Object>> call = apiService.requestVerificationCode(data);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                ProgressDialogUtils.hideProgress();
                if (response.isSuccessful() && response.body() != null) {
                    // 发送成功
                    Toast.makeText(ForgotPasswordActivity.this, "验证码已发送，请注意查收", Toast.LENGTH_SHORT).show();
                    
                    // 开始倒计时
                    startCountDown();
                } else {
                    // 发送失败
                    String errorMsg = ApiClient.getErrorMessage(response);
                    Toast.makeText(ForgotPasswordActivity.this, "发送失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                ProgressDialogUtils.hideProgress();
                Log.e(TAG, "获取验证码请求失败", t);
                Toast.makeText(ForgotPasswordActivity.this, "服务器连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void resetPassword() {
        // 获取输入内容
        String accountOrEmail = etAccountEmail.getText().toString().trim();
        String verificationCode = etVerificationCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();
        
        // 验证输入
        if (accountOrEmail.isEmpty()) {
            Toast.makeText(this, "请输入账号或邮箱", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (verificationCode.isEmpty()) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查密码格式
        if (!isValidPassword(newPassword)) {
            Toast.makeText(this, "密码格式不正确，长度至少6位", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查网络连接
        if (!NetworkUtils.isNetworkConnected(this)) {
            Toast.makeText(this, "无网络连接，请检查网络设置", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 显示进度对话框
        ProgressDialogUtils.showProgress(this, "重置密码中...");
        
        // 模拟验证码校验，固定为123456
        if (!verificationCode.equals("123456")) {
            ProgressDialogUtils.hideProgress();
            Toast.makeText(this, "验证码错误，请重新输入", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 模拟密码重置成功
        new Handler().postDelayed(() -> {
            ProgressDialogUtils.hideProgress();
            Toast.makeText(ForgotPasswordActivity.this, "密码重置成功，请使用新密码登录", Toast.LENGTH_SHORT).show();
            
            // 返回登录页面
            finish();
        }, 1500);
        
        /* 真实代码，暂时注释保留
        // 创建请求数据
        Map<String, String> data = new HashMap<>();
        data.put("account", accountOrEmail);
        data.put("code", verificationCode);
        data.put("new_password", newPassword);
        
        // 调用API重置密码
        ApiService apiService = ApiClient.getApiService(this);
        Call<Map<String, Object>> call = apiService.resetPassword(data);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                ProgressDialogUtils.hideProgress();
                if (response.isSuccessful() && response.body() != null) {
                    // 重置成功
                    Toast.makeText(ForgotPasswordActivity.this, "密码重置成功，请使用新密码登录", Toast.LENGTH_SHORT).show();
                    
                    // 返回登录页面
                    finish();
                } else {
                    // 重置失败
                    String errorMsg = ApiClient.getErrorMessage(response);
                    Toast.makeText(ForgotPasswordActivity.this, "密码重置失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                ProgressDialogUtils.hideProgress();
                Log.e(TAG, "重置密码请求失败", t);
                Toast.makeText(ForgotPasswordActivity.this, "服务器连接失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void startCountDown() {
        if (isCountingDown) {
            return;
        }
        
        isCountingDown = true;
        btnGetCode.setEnabled(false);
        
        // 60秒倒计时
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnGetCode.setText(millisUntilFinished / 1000 + "秒后重试");
            }

            @Override
            public void onFinish() {
                btnGetCode.setText("获取验证码");
                btnGetCode.setEnabled(true);
                isCountingDown = false;
            }
        }.start();
    }

    private boolean isValidPassword(String password) {
        // 密码长度至少6位
        return password.length() >= 6;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消倒计时
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
} 