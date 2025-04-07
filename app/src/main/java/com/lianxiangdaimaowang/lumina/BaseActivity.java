package com.lianxiangdaimaowang.lumina;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lianxiangdaimaowang.lumina.util.LanguageManager;
import com.lianxiangdaimaowang.lumina.theme.ThemeManager;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected LanguageManager languageManager;
    protected ThemeManager themeManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        languageManager = LanguageManager.getInstance(newBase);
        // 使用wrapContext方法包装上下文以应用正确的语言设置
        Context context = languageManager.wrapContext(newBase);
        Log.d(TAG, "attachBaseContext: 应用语言设置 - " + 
              context.getResources().getConfiguration().locale.getLanguage());
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: 初始化活动");
        // 应用主题设置
        themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();
        
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 活动创建完成");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: 配置更改，当前语言 - " + 
              newConfig.locale.getLanguage());
        
        // 确保语言设置在配置更改时保持不变
        languageManager = LanguageManager.getInstance(this);
        languageManager.applyLanguage();
    }
} 