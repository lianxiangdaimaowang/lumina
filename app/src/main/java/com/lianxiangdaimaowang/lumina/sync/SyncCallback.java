package com.lianxiangdaimaowang.lumina.sync;

/**
 * 同步回调接口
 * 用于处理同步操作的成功和失败回调
 */
public interface SyncCallback {
    /**
     * 同步操作成功时调用
     */
    void onSuccess();
    
    /**
     * 同步操作失败时调用
     * @param errorMessage 错误信息
     */
    void onError(String errorMessage);
} 