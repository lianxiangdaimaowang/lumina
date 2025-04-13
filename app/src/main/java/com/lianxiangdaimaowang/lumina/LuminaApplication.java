package com.lianxiangdaimaowang.lumina;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.multidex.MultiDex;

import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.theme.ThemeManager;
import com.lianxiangdaimaowang.lumina.database.NoteDatabase;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.ocr.BaiduOcrManager;
import com.lianxiangdaimaowang.lumina.voice.IflytekVoiceManager;
import com.lianxiangdaimaowang.lumina.utils.MemoryUtils;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 应用全局Application类，用于初始化各种SDK
 */
public class LuminaApplication extends Application {
    
    private static final String TAG = "LuminaApplication";
    private static LuminaApplication instance;
    private ThemeManager themeManager;
    private Handler mainHandler;
    private LocalDataManager localDataManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化崩溃处理
        initCrashHandler();
        
        // 初始化主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化本地数据管理器
        localDataManager = LocalDataManager.getInstance(this);
        
        // 检查内存情况
        try {
            MemoryUtils.logMemoryUsage(this, "应用启动");
            if (MemoryUtils.isMemoryLow(this)) {
                // 如果内存低，尝试释放一些内存
                MemoryUtils.tryFreeMemory();
                Log.w(TAG, "应用启动时检测到内存不足，已尝试释放内存");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查内存状态失败", e);
        }
    }
    
    /**
     * 初始化崩溃处理器
     */
    private void initCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "应用发生未捕获异常，即将崩溃", throwable);
                
                // 记录崩溃日志到文件
                String stackTrace = Log.getStackTraceString(throwable);
                File crashDir = new File(getFilesDir(), "crash_logs");
                if (!crashDir.exists()) {
                    crashDir.mkdirs();
                }
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                String fileName = "crash_" + dateFormat.format(new Date()) + ".txt";
                File crashFile = new File(crashDir, fileName);
                
                try (FileWriter writer = new FileWriter(crashFile)) {
                    writer.write("崩溃时间: " + new Date().toString() + "\n");
                    writer.write("崩溃线程: " + thread.getName() + "\n");
                    writer.write("崩溃堆栈: \n" + stackTrace);
                }
                
                // 执行默认的崩溃处理
                if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof Thread.UncaughtExceptionHandler)) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, throwable);
                }
            } catch (Exception e) {
                Log.e(TAG, "处理崩溃失败", e);
            }
        });
    }
    
    /**
     * 预初始化数据库
     */
    private void initDatabase() {
        try {
            Log.d(TAG, "开始预初始化数据库");
            // 使用新线程初始化数据库，避免阻塞主线程
            new Thread(() -> {
                int retryCount = 0;
                boolean success = false;
                
                while (retryCount < 3 && !success) {
                    try {
                        // 预初始化数据库
                        NoteDatabase database = NoteDatabase.getInstance(getApplicationContext());
                        NoteRepository repository = NoteRepository.getInstance(getApplicationContext());
                        
                        // 验证数据库是否可用
                        if (database != null && repository != null) {
                            // 尝试执行一个简单的数据库操作
                            database.noteDao();
                            success = true;
                            Log.d(TAG, "数据库预初始化成功");
                        } else {
                            throw new Exception("数据库或仓库实例为null");
                        }
                    } catch (Exception e) {
                        retryCount++;
                        Log.e(TAG, "数据库预初始化失败 (尝试 " + retryCount + "/3): " + e.getMessage());
                        try {
                            // 等待一段时间后重试
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                if (!success) {
                    Log.e(TAG, "数据库预初始化最终失败，已重试3次");
                    // 在主线程显示错误提示
                    mainHandler.post(() -> {
                        Toast.makeText(getApplicationContext(), 
                            "数据库初始化失败，部分功能可能无法使用", 
                            Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "启动数据库初始化线程失败", e);
        }
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        // 实现组件初始化代码
    }
    
    /**
     * 初始化工作线程
     */
    private void initWorkers() {
        // 实现工作线程初始化代码
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    
    public static LuminaApplication getInstance() {
        return instance;
    }

    /**
     * 获取本地数据管理器实例
     */
    public LocalDataManager getLocalDataManager() {
        return localDataManager;
    }

    /**
     * 获取主线程Handler
     */
    public Handler getMainHandler() {
        return mainHandler;
    }
}