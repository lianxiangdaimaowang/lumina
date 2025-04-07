package com.lianxiangdaimaowang.lumina.voice;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.lianxiangdaimaowang.lumina.voice.util.IflytekWebIAT;

/**
 * 讯飞语音识别管理器
 * 基于讯飞WebAPI实现在线语音识别
 */
public class IflytekVoiceManager {
    private static final String TAG = "IflytekVoiceManager";
    
    // 请替换为您的实际应用ID和密钥
    // 科大讯飞开放平台(https://www.xfyun.cn/)申请的应用ID和密钥
    // 原API密钥可能是无效的，或者应用未授权，或者已过期
    // 请更新为有效的API密钥
    private static final String APP_ID = "1cdb6339";
    private static final String API_KEY = "a76f02305fccad06f1d8e2df394822ce";
    private static final String API_SECRET = "NjIwOTJkM2JjMDUyNTE2ZjYzY2UzNjRl";
    
    private static volatile IflytekVoiceManager instance;
    private Context context;
    private boolean isInitialized = false;
    private boolean isRecognizing = false;
    
    private IflytekWebIAT webIAT;
    private VoiceRecognitionCallback callback;
    private RecognitionListener recognitionListener;
    
    /**
     * 语音识别回调接口
     */
    public interface VoiceRecognitionCallback {
        void onResult(String result);
        void onError(String error);
        void onReady();
    }
    
    /**
     * 语音识别监听器接口
     */
    public interface RecognitionListener {
        void onBeginOfSpeech();
        void onEndOfSpeech();
        void onResult(String result, boolean isLast);
        void onError(String error);
        void onVolumeChanged(int volume);
    }
    
    /**
     * 获取单例实例
     */
    public static IflytekVoiceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (IflytekVoiceManager.class) {
                if (instance == null) {
                    instance = new IflytekVoiceManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 构造函数
     */
    private IflytekVoiceManager(Context context) {
        this.context = context.getApplicationContext();
        
        // 检查API参数是否有效
        if (APP_ID == null || APP_ID.isEmpty() || 
            API_KEY == null || API_KEY.isEmpty() || 
            API_SECRET == null || API_SECRET.isEmpty()) {
            Log.e(TAG, "API参数无效，请检查APP_ID、API_KEY和API_SECRET");
            isInitialized = false;
        } else {
            initWebIAT();
            isInitialized = true;
        }
    }
    
    /**
     * 初始化WebIAT组件
     */
    private void initWebIAT() {
        webIAT = new IflytekWebIAT(APP_ID, API_KEY, API_SECRET, new IflytekWebIAT.OnResultListener() {
            @Override
            public void onResult(String result, boolean isLast) {
                // 处理识别结果
                if (callback != null) {
                    callback.onResult(result);
                }
                
                if (recognitionListener != null) {
                    recognitionListener.onResult(result, isLast);
                }
                
                if (isLast) {
                    isRecognizing = false;
                }
            }
            
            @Override
            public void onError(int errorCode, String errorMessage) {
                // 处理错误
                String errorMsg = "识别错误: [" + errorCode + "] " + errorMessage;
                Log.e(TAG, errorMsg);
                
                if (callback != null) {
                    callback.onError(errorMsg);
                }
                
                if (recognitionListener != null) {
                    recognitionListener.onError(errorMsg);
                }
                
                isRecognizing = false;
            }
            
            @Override
            public void onConnected() {
                // WebSocket连接成功
                Log.d(TAG, "语音识别服务连接成功");
                
                if (callback != null) {
                    callback.onReady();
                }
                
                if (recognitionListener != null) {
                    recognitionListener.onBeginOfSpeech();
                }
            }
            
            @Override
            public void onDisconnected() {
                // WebSocket连接断开
                Log.d(TAG, "语音识别服务连接断开");
                
                if (recognitionListener != null) {
                    recognitionListener.onEndOfSpeech();
                }
                
                isRecognizing = false;
            }
        });
    }
    
    /**
     * 设置回调
     */
    public void setCallback(VoiceRecognitionCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 设置识别监听器
     */
    public void setRecognitionListener(RecognitionListener listener) {
        this.recognitionListener = listener;
    }
    
    /**
     * 检查网络连接
     */
    private void checkNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.e(TAG, "无法获取ConnectivityManager");
            return;
        }
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        
        if (!isConnected) {
            Log.e(TAG, "网络未连接，请检查网络设置");
            if (callback != null) {
                callback.onError("网络未连接，请检查网络设置");
            }
        } else {
            String networkType = activeNetwork.getTypeName();
            Log.d(TAG, "网络已连接，类型: " + networkType);
        }
    }
    
    /**
     * 开始语音识别
     */
    public void startRecognition() {
        if (!isInitialized) {
            String errorMsg = "语音识别未初始化，请检查API参数配置";
            Log.e(TAG, errorMsg);
            
            if (callback != null) {
                callback.onError(errorMsg);
            }
            
            if (recognitionListener != null) {
                recognitionListener.onError(errorMsg);
            }
            return;
        }
        
        if (isRecognizing) {
            if (callback != null) {
                callback.onError("语音识别正在进行中");
            }
            return;
        }
        
        // 检查网络连接
        checkNetworkConnection();
        
        // 连接WebSocket
        try {
            isRecognizing = true;
            webIAT.connect();
        } catch (Exception e) {
            isRecognizing = false;
            String errorMsg = "启动语音识别失败: " + e.getMessage();
            Log.e(TAG, errorMsg);
            
            if (callback != null) {
                callback.onError(errorMsg);
            }
            
            if (recognitionListener != null) {
                recognitionListener.onError(errorMsg);
            }
        }
    }
    
    /**
     * 发送第一帧音频数据
     * @param audioData 第一帧音频数据
     */
    public void sendFirstFrameAudio(byte[] audioData) {
        if (!isInitialized || !isRecognizing || webIAT == null) {
            Log.e(TAG, "无法发送第一帧数据，识别未初始化或未开始");
            return;
        }
        
        try {
            Log.d(TAG, "发送第一帧音频数据，长度: " + (audioData != null ? audioData.length : 0));
            // 确保使用StatusFirstFrame作为第一帧标记
            webIAT.sendAudioData(audioData, IflytekWebIAT.StatusFirstFrame);
            
            // 模拟音量变化通知
            if (recognitionListener != null) {
                int volumeLevel = calculateVolumeLevel(audioData);
                recognitionListener.onVolumeChanged(volumeLevel);
            }
        } catch (Exception e) {
            Log.e(TAG, "发送第一帧音频数据失败: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("发送第一帧数据失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 发送音频数据
     * @param audioData 音频数据，必须是16k采样率，16bit位深的PCM数据
     */
    public void sendAudioData(byte[] audioData) {
        if (!isInitialized || !isRecognizing || webIAT == null) {
            return;
        }
        
        try {
            webIAT.sendAudioData(audioData, IflytekWebIAT.StatusContinueFrame);
            
            // 模拟音量变化通知
            if (recognitionListener != null) {
                int volumeLevel = calculateVolumeLevel(audioData);
                recognitionListener.onVolumeChanged(volumeLevel);
            }
        } catch (Exception e) {
            Log.e(TAG, "发送音频数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算音量等级
     */
    private int calculateVolumeLevel(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return 0;
        }
        
        // 简单实现，计算音频数据的平均振幅作为音量指标
        long sum = 0;
        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                // 将两个字节组合成一个16位有符号整数
                short amplitude = (short) ((audioData[i] & 0xFF) | (audioData[i + 1] << 8));
                sum += Math.abs(amplitude);
            }
        }
        
        int average = (int) (sum / (audioData.length / 2));
        // 将平均振幅映射到0-100的范围
        return Math.min(100, average * 100 / 32768);
    }
    
    /**
     * 停止语音识别
     */
    public void stopRecognition() {
        if (!isRecognizing || webIAT == null) {
            return;
        }
        
        try {
            // 发送结束帧
            webIAT.sendAudioData(null, IflytekWebIAT.StatusLastFrame);
            isRecognizing = false;
        } catch (Exception e) {
            Log.e(TAG, "停止语音识别失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置语音识别参数
     */
    public void setParams(String language, String accent, boolean enablePunctuation, 
                           boolean enableDynamicCorrection) {
        if (webIAT != null) {
            webIAT.setParams(language, accent, enablePunctuation, enableDynamicCorrection);
        }
    }
    
    /**
     * 开始监听
     */
    public void startListening(RecognitionListener listener) {
        this.recognitionListener = listener;
        startRecognition();
    }
    
    /**
     * 停止监听
     */
    public void stopListening() {
        stopRecognition();
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (isRecognizing && webIAT != null) {
            try {
                webIAT.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "释放资源失败: " + e.getMessage());
            }
        }
        
        isRecognizing = false;
        callback = null;
        recognitionListener = null;
    }
    
    /**
     * 检查是否正在处理
     */
    public boolean isProcessing() {
        return isRecognizing;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }
} 