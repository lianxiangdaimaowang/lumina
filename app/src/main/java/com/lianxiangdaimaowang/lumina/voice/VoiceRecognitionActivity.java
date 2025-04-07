package com.lianxiangdaimaowang.lumina.voice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别界面，用于将语音转换为文本
 * 通过科大讯飞WebSocket API实现在线语音识别
 */
public class VoiceRecognitionActivity extends AppCompatActivity {
    private static final String TAG = "VoiceRecognitionActivity";
    
    // 请求码
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    // UI组件
    private ProgressBar progressBar;
    private TextView resultText;
    private TextView statusText;
    private Button btnStart;
    private Button btnStop;
    private FloatingActionButton fabDone;
    
    // 识别结果
    private String recognizedText = "";
    private boolean isRecognizing = false;
    
    // 语音识别管理器
    private IflytekVoiceManager voiceManager;
    
    // 录音管理器
    private RecorderManager recorderManager;
    
    // 网络管理器
    private NetworkManager networkManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_voice_recognition);
            
            // 初始化语音识别管理器
            voiceManager = IflytekVoiceManager.getInstance(this);
            
            // 初始化录音管理器
            recorderManager = new RecorderManager(this);
            
            // 配置语音识别参数
            configureVoiceRecognition();
            
            // 初始化网络管理器
            networkManager = NetworkManager.getInstance(this);
            
            setupToolbar();
            setupViews();
            setupRecorderManager();
            
            // 检查录音权限
            checkRecordPermission();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: 初始化失败", e);
            Toast.makeText(this, "初始化语音识别失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 配置语音识别参数
     */
    private void configureVoiceRecognition() {
        if (voiceManager != null) {
            // 语言设置为中文，方言普通话
            // 启用动态修正，启用标点符号
            voiceManager.setParams(
                "zh_cn",      // 语言：中文
                "mandarin",   // 方言：普通话
                true,         // 启用标点符号
                true          // 启用动态修正
            );
        }
    }
    
    /**
     * 设置录音管理器回调
     */
    private void setupRecorderManager() {
        recorderManager.setCallback(new RecorderManager.OnAudioDataCallback() {
            @Override
            public void onAudioDataReceived(byte[] data) {
                // 收到音频数据后，将数据发送给语音识别引擎
                if (isRecognizing && voiceManager != null) {
                    // 日志记录音频数据长度，帮助调试
                    Log.d(TAG, "音频数据: " + (data != null ? data.length : 0) + " bytes");
                    
                    // 发送音频数据
                    voiceManager.sendAudioData(data);
                }
            }
            
            @Override
            public void onRecordingStarted() {
                Log.d(TAG, "录音开始");
            }
            
            @Override
            public void onRecordingStopped() {
                Log.d(TAG, "录音停止");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "录音错误: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(VoiceRecognitionActivity.this, 
                            "录音错误: " + error, Toast.LENGTH_SHORT).show();
                    isRecognizing = false;
                    updateUIForState(false);
                });
            }
        });
    }
    
    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "setupToolbar: 设置工具栏失败", e);
        }
    }
    
    private void setupViews() {
        try {
            progressBar = findViewById(R.id.progress_bar);
            resultText = findViewById(R.id.text_result);
            statusText = findViewById(R.id.text_status);
            btnStart = findViewById(R.id.btn_start);
            btnStop = findViewById(R.id.btn_stop);
            fabDone = findViewById(R.id.fab_done);
            
            if (progressBar == null || resultText == null || statusText == null || 
                btnStart == null || fabDone == null) {
                Log.e(TAG, "setupViews: 某些UI组件为空");
                Toast.makeText(this, "初始化视图失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 使用普通点击监听器，点击一次开始识别，再次点击停止识别
            btnStart.setOnClickListener(v -> {
                if (isRecognizing) {
                    // 如果正在识别，点击则停止
                    stopRecognition();
                    btnStart.setText(R.string.start_recording);
                    statusText.setText(R.string.voice_recognition_stopped);
                } else {
                    // 如果没有在识别，点击则开始
                    resultText.setText("");
                    fabDone.setVisibility(View.GONE);
                    startRecognition();
                    btnStart.setText(R.string.stop_recording);
                    statusText.setText(R.string.voice_recognizing);
                }
            });
            
            // 隐藏停止按钮，因为使用点击模式
            btnStop.setVisibility(View.GONE);
            
            fabDone.setOnClickListener(v -> {
                String result = resultText.getText().toString().trim();
                if (!TextUtils.isEmpty(result)) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("voice_result", result);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });
            
            // 显示初始状态
            if (voiceManager != null && voiceManager.isInitialized()) {
                statusText.setText(R.string.voice_press_to_speak);
                btnStart.setText(R.string.start_recording);
            } else {
                statusText.setText(R.string.voice_init_failed);
                btnStart.setEnabled(false);
            }
            resultText.setText("");
            resultText.setHint(R.string.voice_result_hint);
            fabDone.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "setupViews: 初始化视图失败", e);
        }
    }
    
    private void checkRecordPermission() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "录音权限未授予，请求权限");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                Log.d(TAG, "已有录音权限");
            }
        } catch (Exception e) {
            Log.e(TAG, "checkRecordPermission: 检查权限失败", e);
        }
    }
    
    private void checkNetworkAndStartRecognition() {
        try {
            // 先检查网络连接状态
            if (!networkManager.isNetworkConnected()) {
                Log.d(TAG, "网络未连接，显示提示");
                showNetworkErrorDialog();
                return;
            }
            
            // 网络连接正常，检查录音权限并开始语音识别
            checkPermissionAndStartRecognition();
        } catch (Exception e) {
            Log.e(TAG, "checkNetworkAndStartRecognition: 检查网络状态失败", e);
            Toast.makeText(this, "无法检查网络状态", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showNetworkErrorDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("网络错误")
                .setMessage("网络连接失败，请检查网络设置")
                .setPositiveButton("确定", null)
                .show();
    }
    
    private void showNetworkDiagnosticDialog() {
        // 显示加载对话框
        new MaterialAlertDialogBuilder(this)
                .setTitle("网络诊断中...")
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .show();
        
        // 在后台线程执行网络诊断
        new Thread(() -> {
            final String diagnosticResult = networkManager.diagnoseNetworkConnection();
            
            // 在主线程显示结果
            runOnUiThread(() -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("网络诊断结果")
                        .setMessage(diagnosticResult)
                        .setPositiveButton("确定", null)
                        .show();
            });
        }).start();
    }
    
    private void checkPermissionAndStartRecognition() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startRecognition();
            }
        } catch (Exception e) {
            Log.e(TAG, "checkPermissionAndStartRecognition: 检查权限失败", e);
            Toast.makeText(this, "无法检查录音权限", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 启动语音识别
     */
    private void startRecognition() {
        try {
            if (!checkPermission()) {
                requestPermission();
                return;
            }
            
            isRecognizing = true;
            updateUIForState(true);
            
            voiceManager.setCallback(new IflytekVoiceManager.VoiceRecognitionCallback() {
                @Override
                public void onResult(String result) {
                    runOnUiThread(() -> {
                        if (result != null && !result.isEmpty()) {
                            recognizedText = result;
                            resultText.setText(recognizedText);
                            // 在这里添加日志以便调试
                            Log.d(TAG, "收到识别结果: " + result);
                            
                            // 如果语音识别结果不为空，显示完成按钮
                            if (!recognizedText.isEmpty()) {
                                fabDone.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        handleRecognitionError(error);
                    });
                }
                
                @Override
                public void onReady() {
                    runOnUiThread(() -> {
                        showProgress(true);
                        statusText.setText(R.string.voice_recognizing);
                        
                        // 启动录音管理器，开始收集音频
                        startRecorderManager();
                    });
                }
            });
            
            // 启动语音识别
            voiceManager.startRecognition();
            
        } catch (Exception e) {
            Log.e(TAG, "startRecognition: 启动识别失败", e);
            Toast.makeText(this, "启动语音识别失败", Toast.LENGTH_SHORT).show();
            isRecognizing = false;
            updateUIForState(false);
        }
    }
    
    /**
     * 启动录音管理器
     */
    private void startRecorderManager() {
        if (recorderManager != null && !recorderManager.isRecording()) {
            recorderManager.startRecording();
        }
    }
    
    /**
     * 处理识别错误
     */
    private void handleRecognitionError(String errorMsg) {
        isRecognizing = false;
        updateUIForState(false);
        showProgress(false);
        
        Log.e(TAG, "语音识别错误: " + errorMsg);
        
        if (errorMsg.contains("网络未连接") || errorMsg.contains("连接失败")) {
            showNetworkErrorDialog();
        } else if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
            show401ErrorDialog();
        } else if (errorMsg.contains("认证失败") || errorMsg.contains("auth") || 
                   errorMsg.contains("invalid handle") || errorMsg.contains("10165")) {
            // 处理科大讯飞的invalid handle错误，这通常是认证问题
            showAuthErrorDialog(errorMsg);
        } else {
            Toast.makeText(this, "语音识别错误: " + errorMsg, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示认证错误对话框
     */
    private void showAuthErrorDialog(String errorMsg) {
        String message = "连接科大讯飞服务器认证失败，请检查APP_ID、API_KEY和API_SECRET是否正确";
        if (errorMsg.contains("[10165]") || errorMsg.contains("invalid handle")) {
            message = "认证失败: invalid handle [10165]\n\n此错误通常是因为：\n" +
                    "1. API密钥不正确或已过期\n" +
                    "2. 应用未授权使用语音听写功能\n" +
                    "3. 应用配额已用完\n\n" +
                    "请前往科大讯飞开放平台检查应用配置";
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.voice_auth_error_title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .setNeutralButton("重试", (dialog, which) -> {
                    retryRecognition();
                })
                .show();
    }
    
    /**
     * 重试语音识别
     */
    private void retryRecognition() {
        // 停止之前的所有识别活动
        if (voiceManager != null) {
            voiceManager.stopRecognition();
        }
        
        // 停止录音
        if (recorderManager != null) {
            recorderManager.stopRecording();
        }
        
        // 等待1秒后重试
        new Handler().postDelayed(() -> {
            // 重新初始化
            voiceManager = IflytekVoiceManager.getInstance(this);
            configureVoiceRecognition();
            
            // 开始新的识别
            resultText.setText("");
            startRecognition();
        }, 1000);
    }
    
    /**
     * 检查录音权限
     */
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * 请求录音权限
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                REQUEST_RECORD_AUDIO_PERMISSION);
    }
    
    /**
     * 根据识别状态更新UI
     */
    private void updateUIForState(boolean isRecognizing) {
        if (isRecognizing) {
            btnStart.setText(R.string.stop_recording);
            statusText.setText(R.string.voice_recognizing);
            showProgress(true);
            fabDone.setVisibility(View.GONE);
        } else {
            btnStart.setText(R.string.start_recording);
            statusText.setText(R.string.voice_recognition_stopped);
            showProgress(false);
            
            // 在停止识别且有结果时，显示完成按钮
            if (resultText != null && resultText.getText() != null && 
                !resultText.getText().toString().trim().isEmpty()) {
                fabDone.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * 显示或隐藏进度条
     */
    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                progressBar.setProgress(0);
            }
        }
    }
    
    private void finishWithResult() {
        try {
            // 获取输入的文字内容
            recognizedText = resultText.getText().toString();
            
            if (recognizedText.isEmpty()) {
                Toast.makeText(this, R.string.voice_no_speech_recognized, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("voice_result", recognizedText);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "finishWithResult: 返回结果失败", e);
            Toast.makeText(this, "无法返回识别结果", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "录音权限已授予");
                    // 权限被授予，可以启动录音
                    if (btnStart != null) {
                        btnStart.setEnabled(true);
                    }
                } else {
                    Log.e(TAG, "录音权限被拒绝");
                    Toast.makeText(this, R.string.microphone_permission_denied, Toast.LENGTH_SHORT).show();
                    // 权限被拒绝，禁用录音按钮
                    if (btnStart != null) {
                        btnStart.setEnabled(false);
                    }
                    if (statusText != null) {
                        statusText.setText(R.string.microphone_permission_denied);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onRequestPermissionsResult: 处理权限结果失败", e);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == android.R.id.home) {
                onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, "onOptionsItemSelected: 处理菜单选择失败", e);
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (isRecognizing) {
            stopRecognition();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.release();
        }
        
        if (recorderManager != null) {
            recorderManager.release();
        }
    }
    
    /**
     * 停止语音识别
     */
    private void stopRecognition() {
        if (voiceManager != null) {
            voiceManager.stopRecognition();
        }
        
        if (recorderManager != null) {
            recorderManager.stopRecording();
        }
        
        isRecognizing = false;
        updateUIForState(false);
    }
    
    /**
     * 显示401未授权错误对话框
     */
    private void show401ErrorDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("连接错误")
                .setMessage("连接科大讯飞服务器失败，错误代码: 401 Unauthorized\n\n请检查以下内容:\n1. APP_ID是否正确\n2. API_KEY是否正确\n3. API_SECRET是否正确\n4. 授权URL是否正确生成")
                .setPositiveButton("确定", null)
                .show();
    }
} 