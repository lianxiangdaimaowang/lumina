package com.lianxiangdaimaowang.lumina.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;

/**
 * 网络状态变化广播接收器
 * 监听网络状态变化，在网络连接时尝试同步数据
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    private static boolean wasOffline = false; // 用于跟踪之前的网络状态
    private static final long DEBOUNCE_TIME_MS = 5000; // 5秒防抖动
    private static long lastSyncTime = 0; // 上次同步时间
    private static boolean isSyncing = false; // 是否正在同步
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d(TAG, "接收到网络状态变化广播: " + intent.getAction());
            
            if (intent.getAction() == null || 
                    !intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                return;
            }
            
            // 检查是否正在同步中
            if (isSyncing) {
                Log.d(TAG, "已有同步操作正在进行中，忽略此次网络变化");
                return;
            }
            
            // 防抖动检查
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSyncTime < DEBOUNCE_TIME_MS) {
                Log.d(TAG, "网络变化触发太频繁，忽略此次同步");
                return;
            }
            
            NetworkManager networkManager = NetworkManager.getInstance(context);
            SyncManager syncManager = SyncManager.getInstance(context);
            LocalDataManager localDataManager = LocalDataManager.getInstance(context);
            
            // 检查SyncManager是否有挂起的操作
            if (syncManager.hasPendingOperations()) {
                Log.d(TAG, "同步管理器有 " + syncManager.getPendingOperationCount() + " 个操作正在进行中，等待完成");
                return;
            }
            
            // 检查网络是否已连接
            boolean isConnected = networkManager.isNetworkConnected();
            
            if (isConnected) {
                if (wasOffline) {
                    // 从离线状态恢复到在线状态
                    Log.d(TAG, "网络已恢复连接，开始同步数据");
                    lastSyncTime = currentTime;
                    
                    // 检查用户是否已登录
                    if (localDataManager.isSignedIn()) {
                        // 标记同步开始
                        isSyncing = true;
                        
                        // 同步所有笔记
                        syncManager.fetchNotesFromServer(SyncCallbackAdapter.toSyncManagerCallback(new SyncCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "网络恢复后同步完成");
                                
                                // 移除同步成功提示
                                
                                // 发送广播通知应用数据已更新
                                sendDataSyncedBroadcast(context);
                                
                                // 标记同步结束
                                isSyncing = false;
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                Log.e(TAG, "网络恢复后同步失败: " + errorMessage);
                                
                                // 尝试同步未同步的待办项
                                syncPendingItems(context, syncManager);
                            }
                        }));
                    } else {
                        Log.d(TAG, "用户未登录，不进行自动同步");
                    }
                } else {
                    Log.d(TAG, "网络状态良好");
                }
            } else {
                Log.d(TAG, "网络已断开");
                wasOffline = true;
            }
            
            // 更新网络状态
            wasOffline = !isConnected;
        } catch (Exception e) {
            Log.e(TAG, "处理网络状态变化时出错", e);
            isSyncing = false;
        }
    }
    
    /**
     * 只同步待同步的数据项
     */
    private void syncPendingItems(Context context, SyncManager syncManager) {
        syncManager.syncAllPendingItems(SyncCallbackAdapter.toSyncManagerCallback(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "待同步数据同步成功");
                
                // 只在主线程显示Toast
                // 移除同步成功提示
                
                // 发送广播通知应用数据已更新
                sendDataSyncedBroadcast(context);
                
                // 标记同步结束
                isSyncing = false;
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "待同步数据同步失败: " + errorMessage);
                
                // 标记同步结束
                isSyncing = false;
            }
        }));
    }
    
    /**
     * 在主线程显示Toast
     */
    private void showToastOnMainThread(final Context context, final String message) {
        try {
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "显示Toast失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送数据已同步的广播
     */
    private void sendDataSyncedBroadcast(Context context) {
        try {
            Intent syncSuccessIntent = new Intent("com.lianxiangdaimaowang.lumina.DATA_SYNCED");
            context.sendBroadcast(syncSuccessIntent);
        } catch (Exception e) {
            Log.e(TAG, "发送数据同步广播失败: " + e.getMessage());
        }
    }
} 