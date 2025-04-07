package com.lianxiangdaimaowang.lumina.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.util.LanguageManager;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 加载设置Fragment
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsFragment())
            .commit();
    }
    
    /**
     * 设置Fragment的实现
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            
            // 移除同步设置相关的项
            findPreference("enable_sync").setVisible(false);
            findPreference("auto_sync").setVisible(false);
            findPreference("sync_wifi_only").setVisible(false);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            Log.d(TAG, "onPreferenceTreeClick: 点击了偏好设置 - " + key);
            
            if ("language_settings".equals(key)) {
                Log.d(TAG, "onPreferenceTreeClick: 打开语言设置对话框");
                showLanguageDialog();
                return true;
            }
            
            return super.onPreferenceTreeClick(preference);
        }

        private void showLanguageDialog() {
            Log.d(TAG, "showLanguageDialog: 显示语言选择对话框");
            
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.language_settings);
            
            final LanguageManager languageManager = LanguageManager.getInstance(requireContext());
            final String currentLanguage = languageManager.getLanguage();
            Log.d(TAG, "showLanguageDialog: 当前语言设置为 " + currentLanguage);
            
            // 语言选项：系统默认、中文、英文
            final String[] languages = {
                LanguageManager.LANGUAGE_SYSTEM,
                LanguageManager.LANGUAGE_CHINESE,
                LanguageManager.LANGUAGE_ENGLISH
            };
            
            final String[] languageNames = {
                getString(R.string.language_system),
                getString(R.string.language_chinese),
                getString(R.string.language_english)
            };
            
            // 查找当前选择的语言位置
            int checkedItem = 0; // 默认使用系统设置
            for (int i = 0; i < languages.length; i++) {
                if (languages[i].equals(currentLanguage)) {
                    checkedItem = i;
                    break;
                }
            }
            
            builder.setSingleChoiceItems(languageNames, checkedItem, (dialog, which) -> {
                Log.d(TAG, "showLanguageDialog: 选择了语言 " + languages[which]);
                
                // 如果选择的语言与当前语言不同，则应用新语言设置
                if (!languages[which].equals(currentLanguage)) {
                    Log.d(TAG, "showLanguageDialog: 应用新语言设置 " + languages[which]);
                    languageManager.setLanguage(languages[which]);
                    
                    // 显示应用重启提示对话框
                    dialog.dismiss();
                    showLanguageChangeConfirmDialog();
                } else {
                    dialog.dismiss();
                }
            });
            
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        
        private void showLanguageChangeConfirmDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.language_change_title);
            builder.setMessage(R.string.language_change_message);
            builder.setPositiveButton(R.string.restart_now, (dialog, which) -> {
                dialog.dismiss();
                restartApp();
            });
            builder.setNegativeButton(R.string.restart_later, (dialog, which) -> dialog.dismiss());
            
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        
        private void restartApp() {
            requireActivity().recreate();
        }
    }
} 