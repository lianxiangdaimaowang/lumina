package com.lianxiangdaimaowang.lumina.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.lianxiangdaimaowang.lumina.R;

/**
 * 主题管理器，用于设置和管理应用的主题
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";

    // 主题模式常量
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO; // 浅色主题
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES; // 深色主题
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // 跟随系统

    // SharedPreferences常量
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // 单例实例
    private static ThemeManager instance;

    // 上下文
    private Context context;

    // SharedPreferences，用于保存主题设置
    private SharedPreferences preferences;

    /**
     * 获取ThemeManager的单例实例
     * @param context 上下文
     * @return ThemeManager实例
     */
    public static synchronized ThemeManager getInstance(Context context) {
        try {
            if (instance == null) {
                Log.d(TAG, "创建新的ThemeManager实例");
                instance = new ThemeManager(context);
            }
            return instance;
        } catch (Exception e) {
            Log.e(TAG, "ThemeManager.getInstance失败", e);
            return new ThemeManager(context); // 返回一个新实例而不是null
        }
    }

    /**
     * 构造函数
     * @param context 上下文
     */
    public ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 初始化主题，应在应用启动时调用
     */
    public void initTheme() {
        int themeMode = getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * 应用当前主题设置
     */
    public void applyTheme() {
        try {
            Log.d(TAG, "applyTheme: 开始应用主题");
            int themeMode = getThemeMode();
            Log.d(TAG, "applyTheme: 应用主题模式 " + themeMode);
            AppCompatDelegate.setDefaultNightMode(themeMode);
            Log.d(TAG, "applyTheme: 主题应用完成");
        } catch (Exception e) {
            Log.e(TAG, "applyTheme: 应用主题失败", e);
            // 应用默认主题
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * 设置主题模式
     * @param themeMode 主题模式
     */
    public void setThemeMode(int themeMode) {
        // 保存主题模式到SharedPreferences
        preferences.edit()
                .putInt(KEY_THEME_MODE, themeMode)
                .apply();

        // 应用主题模式
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    /**
     * 获取当前主题模式
     * @return 主题模式
     */
    public int getThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, getDefaultThemeMode());
    }

    /**
     * 获取默认主题模式
     * @return 默认主题模式
     */
    private int getDefaultThemeMode() {
        // 对于Android 10及以上版本，默认跟随系统
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MODE_SYSTEM;
        }
        // 对于低版本，根据当前配置判断
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES ? MODE_DARK : MODE_LIGHT;
    }

    /**
     * 判断当前是否为深色主题
     * @return 是否为深色主题
     */
    public boolean isDarkTheme() {
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * 获取主题模式名称
     * @param themeMode 主题模式
     * @return 主题模式名称的资源ID
     */
    public int getThemeModeName(int themeMode) {
        if (themeMode == MODE_LIGHT) {
            return R.string.theme_light;
        } else if (themeMode == MODE_DARK) {
            return R.string.theme_dark;
        } else {
            return R.string.theme_system;
        }
    }
} 