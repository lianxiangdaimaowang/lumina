package com.lianxiangdaimaowang.lumina.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

/**
 * 内存管理工具类，帮助应用优化内存使用和检测内存问题
 */
public class MemoryUtils {
    private static final String TAG = "MemoryUtils";
    
    /**
     * 获取当前应用可用内存情况
     * @param context 上下文
     * @return 可用内存信息字符串
     */
    public static String getMemoryInfo(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            
            long totalMem = memoryInfo.totalMem / (1024 * 1024);
            long availMem = memoryInfo.availMem / (1024 * 1024);
            
            Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(debugMemoryInfo);
            
            int privateDirty = debugMemoryInfo.getTotalPrivateDirty() / 1024;
            int pss = debugMemoryInfo.getTotalPss() / 1024;
            
            StringBuilder sb = new StringBuilder();
            sb.append("总内存: ").append(totalMem).append("MB\n");
            sb.append("可用内存: ").append(availMem).append("MB\n");
            sb.append("当前应用内存使用(PSS): ").append(pss).append("MB\n");
            sb.append("当前应用私有内存: ").append(privateDirty).append("MB\n");
            
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "获取内存信息失败", e);
            return "无法获取内存信息";
        }
    }
    
    /**
     * 检查应用内存是否紧张
     * @param context 上下文
     * @return 如果内存紧张返回true
     */
    public static boolean isMemoryLow(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.lowMemory;
        } catch (Exception e) {
            Log.e(TAG, "检查内存状态失败", e);
            return false;
        }
    }
    
    /**
     * 尝试释放内存，包括清理缓存等
     */
    public static void tryFreeMemory() {
        try {
            // 建议系统进行GC
            System.gc();
            System.runFinalization();
            
            Log.d(TAG, "已请求系统进行内存回收");
        } catch (Exception e) {
            Log.e(TAG, "释放内存失败", e);
        }
    }
    
    /**
     * 打印当前内存使用情况到日志
     * @param context 上下文
     * @param tag 标签，用于标识日志来源
     */
    public static void logMemoryUsage(Context context, String tag) {
        try {
            String memInfo = getMemoryInfo(context);
            Log.d(TAG, tag + " - 内存使用情况:\n" + memInfo);
        } catch (Exception e) {
            Log.e(TAG, "打印内存使用情况失败", e);
        }
    }
} 