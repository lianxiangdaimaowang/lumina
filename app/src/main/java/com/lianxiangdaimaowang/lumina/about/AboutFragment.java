package com.lianxiangdaimaowang.lumina.about;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.base.BaseFragment;

/**
 * 关于应用的Fragment，展示应用信息和开发团队介绍
 */
public class AboutFragment extends BaseFragment {

    public AboutFragment() {
        // 必需的空构造函数
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置工具栏
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // 返回上一个Fragment
            getParentFragmentManager().popBackStack();
        });

        // 设置版本号
        TextView versionText = view.findViewById(R.id.text_version);
        // 由于无法直接获取BuildConfig.VERSION_NAME，使用硬编码版本号，或从资源文件中获取
        String versionInfo = getString(R.string.settings_version) + ": 1.0.0";
        versionText.setText(versionInfo);
    }
} 