package com.lianxiangdaimaowang.lumina.sync;

import java.lang.ref.WeakReference;

/**
 * SyncCallback适配器，用于在不同类型的回调接口之间进行转换
 */
public class SyncCallbackAdapter {
    
    /**
     * 将SyncManager.SyncCallback转换为SyncCallback
     * 使用弱引用避免内存泄漏
     */
    public static SyncCallback toSyncCallback(final SyncManager.SyncCallback callback) {
        if (callback == null) return null;
        
        // 使用弱引用避免内存泄漏
        final WeakReference<SyncManager.SyncCallback> weakCallback = new WeakReference<>(callback);
        
        return new SyncCallback() {
            @Override
            public void onSuccess() {
                SyncManager.SyncCallback cb = weakCallback.get();
                if (cb != null) {
                    cb.onSuccess();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                SyncManager.SyncCallback cb = weakCallback.get();
                if (cb != null) {
                    cb.onError(errorMessage);
                }
            }
        };
    }
    
    /**
     * 将SyncCallback转换为SyncManager.SyncCallback
     * 使用弱引用避免内存泄漏
     */
    public static SyncManager.SyncCallback toSyncManagerCallback(final SyncCallback callback) {
        if (callback == null) return null;
        
        // 使用弱引用避免内存泄漏
        final WeakReference<SyncCallback> weakCallback = new WeakReference<>(callback);
        
        return new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                SyncCallback cb = weakCallback.get();
                if (cb != null) {
                    cb.onSuccess();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                SyncCallback cb = weakCallback.get();
                if (cb != null) {
                    cb.onError(errorMessage);
                }
            }
        };
    }
} 