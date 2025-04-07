package com.lianxiangdaimaowang.lumina.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.lianxiangdaimaowang.lumina.MainActivity;
import com.lianxiangdaimaowang.lumina.R;

import java.util.Locale;

public class LanguageManager {
    private static final String TAG = "LanguageManager";
    private static final String PREF_LANGUAGE = "pref_language";
    public static final String LANGUAGE_SYSTEM = "system";
    public static final String LANGUAGE_CHINESE = "zh";
    public static final String LANGUAGE_ENGLISH = "en";

    private static LanguageManager instance;
    private final Context context;
    private SharedPreferences prefs;

    private LanguageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context);
        }
        return instance;
    }

    public void setLanguage(String languageCode) {
        Log.d(TAG, "setLanguage: 设置语言为 " + languageCode);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_LANGUAGE, languageCode);
        editor.apply();
    }

    public String getLanguage() {
        return prefs.getString(PREF_LANGUAGE, LANGUAGE_CHINESE);
    }

    public Context wrapContext(Context context) {
        String language = getLanguage();
        Locale locale;
        
        if (LANGUAGE_SYSTEM.equals(language)) {
            locale = Resources.getSystem().getConfiguration().locale;
            Log.d(TAG, "wrapContext: 使用系统语言 " + locale.getLanguage());
        } else {
            locale = new Locale(language);
            Log.d(TAG, "wrapContext: 使用指定语言 " + language);
        }
        
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        
        return context.createConfigurationContext(config);
    }

    public void applyLanguage() {
        String language = getLanguage();
        Log.d(TAG, "applyLanguage: 应用语言设置为 " + language);
        Locale locale;

        if (LANGUAGE_SYSTEM.equals(language)) {
            locale = Resources.getSystem().getConfiguration().locale;
            Log.d(TAG, "applyLanguage: 使用系统语言 " + locale.getLanguage());
        } else {
            locale = new Locale(language);
            Log.d(TAG, "applyLanguage: 使用指定语言 " + language);
        }

        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config);
        } else {
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }

    public void updateLanguage(Context activityContext, boolean shouldRecreate) {
        Log.d(TAG, "updateLanguage: 更新语言设置");
        applyLanguage();

        if (shouldRecreate) {
            Log.d(TAG, "updateLanguage: 需要重新创建活动");
            Intent intent = new Intent(activityContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("refresh_language", true);
            activityContext.startActivity(intent);
        }
    }

    public boolean isSystemLanguage() {
        return LANGUAGE_SYSTEM.equals(getLanguage());
    }

    public boolean isChineseLanguage() {
        String language = getLanguage();
        return LANGUAGE_CHINESE.equals(language) || 
               (LANGUAGE_SYSTEM.equals(language) && Resources.getSystem().getConfiguration().locale.getLanguage().startsWith("zh"));
    }

    public boolean isEnglishLanguage() {
        String language = getLanguage();
        return LANGUAGE_ENGLISH.equals(language) || 
               (LANGUAGE_SYSTEM.equals(language) && Resources.getSystem().getConfiguration().locale.getLanguage().startsWith("en"));
    }
} 