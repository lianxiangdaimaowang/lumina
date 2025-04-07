package com.lianxiangdaimaowang.lumina.voice;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录音管理器
 * 用于管理音频录制并将数据传递给语音识别引擎
 */
public class RecorderManager {
    private static final String TAG = "RecorderManager";
    
    // 音频参数
    private static final int SAMPLE_RATE = 16000; // 采样率16kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // 16位PCM格式
    
    // 计算缓冲区大小
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
            
    // 音频录制相关
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private ExecutorService executor;
    
    // 回调
    private OnAudioDataCallback callback;
    
    // 处理器（用于在主线程回调）
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 回调接口
    public interface OnAudioDataCallback {
        void onAudioDataReceived(byte[] data);
        void onRecordingStarted();
        void onRecordingStopped();
        void onError(String error);
    }
    
    /**
     * 构造函数
     */
    public RecorderManager(Context context) {
        try {
            // 初始化线程池
            executor = Executors.newSingleThreadExecutor();
        } catch (Exception e) {
            Log.e(TAG, "初始化录音管理器失败", e);
        }
    }
    
    /**
     * 设置音频数据回调
     */
    public void setCallback(OnAudioDataCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 开始录音
     */
    public void startRecording() {
        if (isRecording) {
            Log.d(TAG, "已经在录音中");
            return;
        }
        
        try {
            // 初始化AudioRecord
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE);
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("初始化录音器失败"));
                }
                Log.e(TAG, "AudioRecord初始化失败");
                return;
            }
            
            // 开始录音
            audioRecord.startRecording();
            isRecording = true;
            
            // 通知录音开始
            if (callback != null) {
                mainHandler.post(() -> callback.onRecordingStarted());
            }
            
            // 在后台线程中读取音频数据
            executor.execute(this::readAudioData);
            
            Log.d(TAG, "开始录音");
        } catch (Exception e) {
            Log.e(TAG, "启动录音失败", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("启动录音失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 读取音频数据的后台任务
     */
    private void readAudioData() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (isRecording) {
                // 读取音频数据
                int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
                
                if (readSize > 0) {
                    // 复制数据
                    final byte[] audioData = new byte[readSize];
                    System.arraycopy(buffer, 0, audioData, 0, readSize);
                    
                    // 通过回调返回数据
                    if (callback != null) {
                        mainHandler.post(() -> callback.onAudioDataReceived(audioData));
                    }
                } else if (readSize == AudioRecord.ERROR_INVALID_OPERATION || 
                           readSize == AudioRecord.ERROR_BAD_VALUE ||
                           readSize == AudioRecord.ERROR) {
                    String error = "录制音频错误: " + readSize;
                    Log.e(TAG, error);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(error));
                    }
                    break;
                }
                
                // 降低CPU占用
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Log.e(TAG, "读取音频线程被中断", e);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "读取音频数据失败", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("读取音频数据失败: " + e.getMessage()));
            }
        } finally {
            stopRecording();
        }
    }
    
    /**
     * 停止录音
     */
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        try {
            isRecording = false;
            
            if (audioRecord != null) {
                // 检查状态，避免在未初始化时调用
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }
            
            // 通知录音停止
            if (callback != null) {
                mainHandler.post(() -> callback.onRecordingStopped());
            }
            
            Log.d(TAG, "停止录音");
        } catch (Exception e) {
            Log.e(TAG, "停止录音失败", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("停止录音失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopRecording();
        
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
    
    /**
     * 检查是否正在录音
     */
    public boolean isRecording() {
        return isRecording;
    }
} 