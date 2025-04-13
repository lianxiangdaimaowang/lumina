package com.lianxiangdaimaowang.lumina.theme;

import android.os.Bundle;
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
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.base.BaseFragment;

/**
 * 主题设置Fragment，用于设置应用的主题
 */
public class ThemeSettingsFragment extends BaseFragment {

    private ThemeManager themeManager;
    private RadioGroup radioGroupTheme;
    private RadioButton radioThemeSystem;
    private RadioButton radioThemeLight;
    private RadioButton radioThemeDark;
    private MaterialButton btnApplyTheme;
    private int selectedThemeMode;

    public ThemeSettingsFragment() {
        // 必需的空构造函数
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeManager = ThemeManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 根据当前主题设置背景
        View view = inflater.inflate(R.layout.fragment_theme_settings, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置工具栏
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // 初始化视图
        radioGroupTheme = view.findViewById(R.id.radio_group_theme);
        radioThemeSystem = view.findViewById(R.id.radio_theme_system);
        radioThemeLight = view.findViewById(R.id.radio_theme_light);
        radioThemeDark = view.findViewById(R.id.radio_theme_dark);
        btnApplyTheme = view.findViewById(R.id.btn_apply_theme);

        // 设置当前选中的主题
        selectedThemeMode = themeManager.getThemeMode();
        setCheckedTheme();

        // 设置主题切换监听器
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_theme_light) {
                selectedThemeMode = ThemeManager.MODE_LIGHT;
            } else if (checkedId == R.id.radio_theme_dark) {
                selectedThemeMode = ThemeManager.MODE_DARK;
            } else {
                selectedThemeMode = ThemeManager.MODE_SYSTEM;
            }
        });

        // 设置应用按钮的点击事件
        btnApplyTheme.setOnClickListener(v -> {
            if (selectedThemeMode != themeManager.getThemeMode()) {
                themeManager.setThemeMode(selectedThemeMode);
            } else {
                Toast.makeText(requireContext(), R.string.theme_already_applied, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 设置当前选中的主题
     */
    private void setCheckedTheme() {
        if (selectedThemeMode == ThemeManager.MODE_LIGHT) {
            radioThemeLight.setChecked(true);
        } else if (selectedThemeMode == ThemeManager.MODE_DARK) {
            radioThemeDark.setChecked(true);
        } else {
            radioThemeSystem.setChecked(true);
        }
    }
}