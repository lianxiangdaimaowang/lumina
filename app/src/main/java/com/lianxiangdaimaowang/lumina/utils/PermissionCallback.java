package com.lianxiangdaimaowang.lumina.utils;

/**
 * 权限回调接口，用于统一处理权限请求结果
 */
public interface PermissionCallback {
    /**
     * 当权限被授予时调用
     * @param permission 权限名称
     */
    void onPermissionGranted(String permission);
    
    /**
     * 当权限被拒绝时调用
     * @param permission 权限名称
     */
    void onPermissionDenied(String permission);
} 