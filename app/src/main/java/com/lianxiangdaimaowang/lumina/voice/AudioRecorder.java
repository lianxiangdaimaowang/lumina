package com.lianxiangdaimaowang.lumina.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * 音频录制器
 * 用于实时录制音频并通过回调返回PCM数据
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2;
    
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private AudioDataCallback callback;
    
    public interface AudioDataCallback {
        void onAudioData(byte[] data, int length);
        void onError(String error);
    }
    
    public AudioRecorder() {
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );
        } catch (Exception e) {
            Log.e(TAG, "AudioRecord initialization failed: " + e.getMessage());
        }
    }
    
    public void setCallback(AudioDataCallback callback) {
        this.callback = callback;
    }
    
    public void startRecording() {
        if (isRecording) {
            return;
        }
        
        if (audioRecord == null) {
            if (callback != null) {
                callback.onError("AudioRecord未初始化");
            }
            return;
        }
        
        try {
            audioRecord.startRecording();
            isRecording = true;
            
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (isRecording) {
                    int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    if (readSize > 0 && callback != null) {
                        byte[] data = new byte[readSize];
                        System.arraycopy(buffer, 0, data, 0, readSize);
                        callback.onAudioData(data, readSize);
                    }
                }
            });
            recordingThread.start();
            
        } catch (Exception e) {
            isRecording = false;
            if (callback != null) {
                callback.onError("开始录音失败: " + e.getMessage());
            }
        }
    }
    
    public void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recordingThread = null;
        }
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, "停止录音失败: " + e.getMessage());
            }
        }
    }
    
    public void release() {
        stopRecording();
        if (audioRecord != null) {
            try {
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "释放录音资源失败: " + e.getMessage());
            }
            audioRecord = null;
        }
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public int getBufferSize() {
        return BUFFER_SIZE;
    }
} 