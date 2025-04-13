package com.lianxiangdaimaowang.lumina.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 网络工具类，提供网络相关的实用方法
 */
public class NetworkUtils {
    
    /**
     * 检查设备是否连接到网络
     * @param context 上下文
     * @return 如果设备已连接到网络（WiFi或移动数据），则返回true
     */
    public static boolean isNetworkConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    
    /**
     * 检查设备是否连接到WiFi网络
     * @param context 上下文
     * @return 如果设备已连接到WiFi网络，则返回true
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetwork != null && wifiNetwork.isConnected();
    }
    
    /**
     * 检查设备是否连接到移动数据网络
     * @param context 上下文
     * @return 如果设备已连接到移动数据网络，则返回true
     */
    public static boolean isMobileConnected(Context context) {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobileNetwork != null && mobileNetwork.isConnected();
    }
} 