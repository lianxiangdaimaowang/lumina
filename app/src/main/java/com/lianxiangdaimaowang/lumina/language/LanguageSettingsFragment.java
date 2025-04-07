package com.lianxiangdaimaowang.lumina.language;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.lianxiangdaimaowang.lumina.MainActivity;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.base.BaseFragment;

/**
 * 语言设置Fragment，用于设置应用的语言
 */
public class LanguageSettingsFragment extends BaseFragment {
    private static final String TAG = "LanguageSettingsFragment";

    private LanguageManager languageManager;
    private com.lianxiangdaimaowang.lumina.util.LanguageManager utilLanguageManager;
    private RadioGroup radioGroupLanguage;
    private RadioButton radioLanguageSystem;
    private RadioButton radioLanguageChinese;
    private RadioButton radioLanguageEnglish;
    private MaterialButton buttonRestartApp;
    private MaterialButton buttonApplyNow;
    
    // 记录是否更改了语言
    private boolean languageChanged = false;

    public LanguageSettingsFragment() {
        // 必需的空构造函数
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            languageManager = LanguageManager.getInstance(requireContext());
            utilLanguageManager = com.lianxiangdaimaowang.lumina.util.LanguageManager.getInstance(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "onCreate: 初始化LanguageManager失败", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_language_settings, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: 加载布局失败", e);
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            // 设置工具栏
            setupToolbar(view);
            
            // 初始化视图
            setupViews(view);
            
            // 设置当前选中的语言
            setCheckedLanguage();
            
            // 设置监听器
            setupListeners();
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: 初始化界面失败", e);
        }
    }
    
    private void setupToolbar(View view) {
        try {
            MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "setupToolbar: 初始化工具栏失败", e);
        }
    }
    
    private void setupViews(View view) {
        try {
            radioGroupLanguage = view.findViewById(R.id.radio_group_language);
            radioLanguageSystem = view.findViewById(R.id.radio_language_system);
            radioLanguageChinese = view.findViewById(R.id.radio_language_chinese);
            radioLanguageEnglish = view.findViewById(R.id.radio_language_english);
            buttonRestartApp = view.findViewById(R.id.button_restart_app);
            
            // 添加立即应用按钮
            buttonApplyNow = view.findViewById(R.id.button_apply_now);
            if (buttonApplyNow == null) {
                Log.d(TAG, "立即应用按钮不存在，可能需要更新布局");
            }
            
            if (radioGroupLanguage == null || radioLanguageSystem == null || 
                radioLanguageChinese == null || radioLanguageEnglish == null || 
                buttonRestartApp == null) {
                Log.e(TAG, "setupViews: 某些UI组件为空");
                Toast.makeText(requireContext(), "加载语言设置界面失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "setupViews: 初始化视图失败", e);
        }
    }
    
    private void setupListeners() {
        try {
            if (radioGroupLanguage == null || buttonRestartApp == null || languageManager == null) {
                Log.e(TAG, "setupListeners: 必要组件为空");
                return;
            }
            
            // 设置语言切换监听器
            radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
                try {
                    String languageCode;
                    if (checkedId == R.id.radio_language_chinese) {
                        languageCode = LanguageManager.LANGUAGE_CHINESE;
                    } else if (checkedId == R.id.radio_language_english) {
                        languageCode = LanguageManager.LANGUAGE_ENGLISH;
                    } else {
                        languageCode = LanguageManager.LANGUAGE_SYSTEM;
                    }
                    
                    // 应用新语言
                    if (!languageCode.equals(languageManager.getLanguageCode())) {
                        // 同时更新两个LanguageManager的语言设置
                        languageManager.setLanguageCode(languageCode);
                        utilLanguageManager.setLanguage(languageCode);  // 添加这一行来同步设置
                        
                        languageChanged = true;
                        Toast.makeText(requireContext(), R.string.language_changed, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "语言切换失败", e);
                    Toast.makeText(requireContext(), "切换语言失败", Toast.LENGTH_SHORT).show();
                }
            });

            // 设置重启应用按钮点击事件
            buttonRestartApp.setOnClickListener(v -> {
                try {
                    restartApp();
                } catch (Exception e) {
                    Log.e(TAG, "重启应用失败", e);
                    Toast.makeText(requireContext(), "重启应用失败", Toast.LENGTH_SHORT).show();
                }
            });
            
            // 如果立即应用按钮存在，设置其点击事件
            if (buttonApplyNow != null) {
                buttonApplyNow.setOnClickListener(v -> {
                    try {
                        applyLanguageNow();
                    } catch (Exception e) {
                        Log.e(TAG, "立即应用语言失败", e);
                        Toast.makeText(requireContext(), "应用语言失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "setupListeners: 设置监听器失败", e);
        }
    }

    /**
     * 设置当前选中的语言
     */
    private void setCheckedLanguage() {
        try {
            if (languageManager == null || radioLanguageSystem == null || 
                radioLanguageChinese == null || radioLanguageEnglish == null) {
                Log.e(TAG, "setCheckedLanguage: 必要组件为空");
                return;
            }
            
            String currentLanguageCode = languageManager.getLanguageCode();
            Log.d(TAG, "当前语言代码: " + currentLanguageCode);
            
            if (LanguageManager.LANGUAGE_CHINESE.equals(currentLanguageCode)) {
                radioLanguageChinese.setChecked(true);
            } else if (LanguageManager.LANGUAGE_ENGLISH.equals(currentLanguageCode)) {
                radioLanguageEnglish.setChecked(true);
            } else {
                radioLanguageSystem.setChecked(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "setCheckedLanguage: 设置选中语言失败", e);
        }
    }
    
    /**
     * 立即应用语言设置
     */
    private void applyLanguageNow() {
        try {
            if (languageManager == null) {
                Log.e(TAG, "applyLanguageNow: LanguageManager为空");
                Toast.makeText(requireContext(), "无法应用语言设置", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查语言是否变化
            if (!languageChanged) {
                Toast.makeText(requireContext(), "语言设置未更改", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 保存新的语言设置
            String languageCode = languageManager.getLanguageCode();
            Log.d(TAG, "applyLanguageNow: 正在应用语言: " + languageCode);
            
            // 确保两个语言管理器设置一致
            languageManager.setLanguageCode(languageCode);
            utilLanguageManager.setLanguage(languageCode);
            
            // 应用语言设置
            Context context = requireContext();
            Context wrappedContext = languageManager.wrapContext(context);
            
            // 显示提示
            Toast.makeText(wrappedContext, R.string.language_changed, Toast.LENGTH_SHORT).show();
            
            // 刷新当前界面
            if (getActivity() != null) {
                Log.d(TAG, "applyLanguageNow: 刷新Activity");
                getActivity().recreate();
            }
            
            languageChanged = false;
        } catch (Exception e) {
            Log.e(TAG, "applyLanguageNow: 应用语言设置失败", e);
            Toast.makeText(requireContext(), "应用语言设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 重启应用
     */
    private void restartApp() {
        try {
            Activity activity = getActivity();
            if (activity != null) {
                // 如果语言没有变化，询问用户
                if (!languageChanged) {
                    Toast.makeText(requireContext(), "语言设置未更改，无需重启", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 确保在重启前两个语言管理器设置一致
                String languageCode = languageManager.getLanguageCode();
                utilLanguageManager.setLanguage(languageCode);
                
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("refresh_language", true); // 添加标记，表示这是语言刷新
                startActivity(intent);
                activity.finish();
            } else {
                Log.e(TAG, "restartApp: Activity为空");
                Toast.makeText(requireContext(), "无法重启应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "restartApp: 重启应用失败", e);
            Toast.makeText(requireContext(), "重启应用失败", Toast.LENGTH_SHORT).show();
        }
    }
}