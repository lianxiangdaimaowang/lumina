package com.lianxiangdaimaowang.lumina.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;
import android.content.ContextWrapper;

import java.util.Locale;

/**
 * 语言管理器，用于设置和管理应用的语言
 */
public class LanguageManager {

    private static final String TAG = "LanguageManager";

    // 语言代码常量
    public static final String LANGUAGE_SYSTEM = "system"; // 跟随系统
    public static final String LANGUAGE_CHINESE = "zh"; // 中文
    public static final String LANGUAGE_ENGLISH = "en"; // 英文

    // SharedPreferences常量
    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANGUAGE_CODE = "language_code";

    // 单例实例
    private static LanguageManager instance;

    // 上下文
    private Context context;

    // SharedPreferences，用于保存语言设置
    private SharedPreferences preferences;

    /**
     * 获取LanguageManager的单例实例
     * @param context 上下文
     * @return LanguageManager实例
     */
    public static synchronized LanguageManager getInstance(Context context) {
        try {
            if (instance == null) {
                Log.d(TAG, "创建新的LanguageManager实例");
                instance = new LanguageManager(context);
            }
            return instance;
        } catch (Exception e) {
            Log.e(TAG, "LanguageManager.getInstance失败", e);
            return new LanguageManager(context); // 返回一个新实例而不是null
        }
    }

    /**
     * 构造函数
     * @param context 上下文
     */
    public LanguageManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 初始化语言设置，应在应用启动时调用
     */
    public void initLanguage() {
        try {
            Log.d(TAG, "initLanguage: 开始初始化语言设置");
            String languageCode = getLanguageCode();
            Log.d(TAG, "initLanguage: 当前语言代码 " + languageCode);
            
            // 应用语言设置
            applyLanguage();
            
            Log.d(TAG, "initLanguage: 语言初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "initLanguage: 初始化语言失败", e);
        }
    }
    
    /**
     * 应用语言设置，不需要传入上下文(使用构造函数中的上下文)
     */
    public void applyLanguage() {
        try {
            Log.d(TAG, "applyLanguage: 开始应用语言设置");
            Context newContext = applyLanguage(this.context);
            if (newContext != this.context) {
                Log.d(TAG, "applyLanguage: 上下文已更新");
            }
            Log.d(TAG, "applyLanguage: 语言设置应用完成");
        } catch (Exception e) {
            Log.e(TAG, "applyLanguage: 应用语言设置失败", e);
            // 应用失败不应该影响应用的运行
        }
    }

    /**
     * 应用语言，更新应用的语言设置
     * @param context 上下文
     * @return 使用更新后语言的上下文
     */
    public Context applyLanguage(Context context) {
        try {
            Log.d(TAG, "applyLanguage: 开始应用语言设置到指定上下文");
            String languageCode = getLanguageCode();
            Log.d(TAG, "applyLanguage: 应用语言代码 " + languageCode);
            
            // 创建Locale对象
            Locale locale;
            if (LANGUAGE_SYSTEM.equals(languageCode)) {
                Log.d(TAG, "applyLanguage: 使用系统语言");
                locale = getSystemLocale();
            } else {
                locale = new Locale(languageCode);
            }
            
            Locale.setDefault(locale);
            Log.d(TAG, "applyLanguage: 设置默认Locale为 " + locale.getLanguage());
            
            // 更新应用的语言设置
            Resources resources = context.getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG, "applyLanguage: Android 7.0+的语言设置方式");
                // 对于Android 7.0及以上版本
                LocaleList localeList = new LocaleList(locale);
                LocaleList.setDefault(localeList);
                configuration.setLocales(localeList);
                
                Context newContext = context.createConfigurationContext(configuration);
                Log.d(TAG, "applyLanguage: 创建了新的配置上下文，语言为 " + locale.getLanguage());
                return newContext;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Log.d(TAG, "applyLanguage: Android 4.2-6.0的语言设置方式");
                // 对于Android 4.2-6.0版本
                configuration.setLocale(locale);
                Context newContext = context.createConfigurationContext(configuration);
                Log.d(TAG, "applyLanguage: 创建了新的配置上下文，语言为 " + locale.getLanguage());
                return newContext;
            } else {
                Log.d(TAG, "applyLanguage: 旧版Android的语言设置方式");
                // 对于Android 4.1及以下版本
                configuration.locale = locale;
                resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                Log.d(TAG, "applyLanguage: 更新了Resources配置，语言为 " + locale.getLanguage());
                return context;
            }
        } catch (Exception e) {
            Log.e(TAG, "applyLanguage: 应用语言设置到指定上下文失败", e);
            // 应用失败时返回原始上下文
            return context;
        }
    }
    
    /**
     * 获取系统的Locale
     * @return 系统Locale
     */
    private Locale getSystemLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            return Resources.getSystem().getConfiguration().locale;
        }
    }

    /**
     * 设置语言代码
     * @param languageCode 语言代码
     */
    public void setLanguageCode(String languageCode) {
        Log.d(TAG, "setLanguageCode: 设置语言代码为 " + languageCode);
        // 保存语言代码到SharedPreferences
        preferences.edit()
                .putString(KEY_LANGUAGE_CODE, languageCode)
                .apply();
    }

    /**
     * 获取当前语言代码
     * @return 语言代码
     */
    public String getLanguageCode() {
        String code = preferences.getString(KEY_LANGUAGE_CODE, LANGUAGE_SYSTEM);
        Log.d(TAG, "getLanguageCode: 获取到语言代码 " + code);
        return code;
    }

    /**
     * 获取语言名称
     * @param languageCode 语言代码
     * @return 语言名称的资源ID
     */
    public int getLanguageName(String languageCode) {
        if (LANGUAGE_CHINESE.equals(languageCode)) {
            return com.lianxiangdaimaowang.lumina.R.string.language_chinese;
        } else if (LANGUAGE_ENGLISH.equals(languageCode)) {
            return com.lianxiangdaimaowang.lumina.R.string.language_english;
        } else {
            return com.lianxiangdaimaowang.lumina.R.string.language_system;
        }
    }
    
    /**
     * 包装上下文，使其使用我们设置的语言
     * @param context 原始上下文
     * @return 包装后的上下文
     */
    public Context wrapContext(Context context) {
        String languageCode = getLanguageCode();
        // 如果使用系统语言，不需要包装
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            return context;
        }
        
        // 创建新的配置
        Locale locale = new Locale(languageCode);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        
        return context.createConfigurationContext(config);
    }

    /**
     * 更新基础上下文，在attachBaseContext中使用
     * @param context 原始上下文
     * @return 更新语言后的上下文
     */
    public Context updateBaseContext(Context context) {
        String languageCode = getLanguageCode();
        
        // 如果使用系统语言，不需要更新
        if (LANGUAGE_SYSTEM.equals(languageCode)) {
            return context;
        }
        
        // 创建Locale对象
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        // 创建更新语言的Configuration
        Configuration config = new Configuration(context.getResources().getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(config);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }
} 