package com.lianxiangdaimaowang.lumina.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.imageview.ShapeableImageView;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.base.BaseFragment;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.language.LanguageSettingsFragment;
import com.lianxiangdaimaowang.lumina.theme.ThemeSettingsFragment;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.about.AboutFragment;

/**
 * 个人中心Fragment，展示用户信息和设置项
 */
public class ProfileFragment extends BaseFragment {
    
    private static final String PREF_STUDENT_IDENTITY = "student_identity";
    private LocalDataManager localDataManager;
    private ShapeableImageView avatarImage;
    private TextView usernameText;
    private TextView notesCountText;
    private TextView reviewsCountText;
    private View pointsContainer; // 积分容器视图（需要隐藏）
    
    public ProfileFragment() {
        // 必需的空构造函数
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localDataManager = LocalDataManager.getInstance(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化各个视图
        avatarImage = view.findViewById(R.id.image_avatar);
        usernameText = view.findViewById(R.id.text_username);
        notesCountText = view.findViewById(R.id.text_notes_count);
        reviewsCountText = view.findViewById(R.id.text_reviews_count);
        
        // 隐藏积分容器
        pointsContainer = view.findViewById(R.id.layout_points);
        pointsContainer.setVisibility(View.GONE);
        
        // 隐藏邮箱文本
        TextView emailText = view.findViewById(R.id.text_email);
        emailText.setVisibility(View.GONE);
        
        // 设置点击事件
        view.findViewById(R.id.button_edit_profile).setOnClickListener(v -> navigateToEditProfile());
        view.findViewById(R.id.layout_theme_settings).setOnClickListener(v -> navigateToThemeSettings());
        view.findViewById(R.id.layout_language_settings).setOnClickListener(v -> navigateToLanguageSettings());
        view.findViewById(R.id.layout_about).setOnClickListener(v -> navigateToAbout());
        view.findViewById(R.id.button_logout).setOnClickListener(v -> logout());
        
        // 加载用户信息
        loadUserInfo();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次重新显示时刷新用户信息
        loadUserInfo();
    }
    
    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        // 设置用户名及学生身份
        String username = localDataManager.getCurrentUsername();
        String studentIdentity = getStudentIdentityName();
        if (studentIdentity != null && !studentIdentity.isEmpty()) {
            usernameText.setText(String.format("%s (%s)", username, studentIdentity));
        } else {
            usernameText.setText(username);
        }
        
        try {
            // 显示本地数据管理器的复习计划数量
            int reviewsCount = localDataManager.getAllReviewPlans().size();
            reviewsCountText.setText(String.valueOf(reviewsCount));
            
            // 获取数据库中的笔记数量
            NoteRepository noteRepository = NoteRepository.getInstance(requireContext());
            noteRepository.getNotesCount(count -> {
                // 在UI线程更新笔记数量
                requireActivity().runOnUiThread(() -> {
                    notesCountText.setText(String.valueOf(count));
                });
            });
        } catch (Exception e) {
            // 出现异常时使用默认值
            notesCountText.setText("0");
            reviewsCountText.setText("0");
        }
    }
    
    /**
     * 获取学生身份名称
     */
    private String getStudentIdentityName() {
        SharedPreferences prefs = requireContext().getSharedPreferences("profile_prefs", requireContext().MODE_PRIVATE);
        int identityId = prefs.getInt(PREF_STUDENT_IDENTITY, 0); // 默认为0
        
        // 根据保存的值确定身份
        switch (identityId) {
            case 1: // 小学生
                return getString(R.string.primary_student);
            case 2: // 初中生
                return getString(R.string.middle_student);
            case 3: // 高中生
                return getString(R.string.high_student);
            case 4: // 大学生
                return getString(R.string.college_student);
            case 5: // 硕士
                return getString(R.string.master_student);
            case 6: // 博士
                return getString(R.string.phd_student);
            default:
                return getString(R.string.middle_student);
        }
    }
    
    /**
     * 导航到编辑个人资料页面
     */
    private void navigateToEditProfile() {
        Intent intent = new Intent(requireContext(), EditProfileActivity.class);
        startActivity(intent);
    }
    
    /**
     * 导航到主题设置页面
     */
    private void navigateToThemeSettings() {
        // 创建主题设置Fragment
        ThemeSettingsFragment themeSettingsFragment = new ThemeSettingsFragment();
        
        // 执行Fragment事务
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, themeSettingsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    /**
     * 导航到语言设置页面
     */
    private void navigateToLanguageSettings() {
        // 创建语言设置Fragment
        LanguageSettingsFragment languageSettingsFragment = new LanguageSettingsFragment();
        
        // 执行Fragment事务
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, languageSettingsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    /**
     * 导航到关于页面
     */
    private void navigateToAbout() {
        // 创建关于应用Fragment
        AboutFragment aboutFragment = new AboutFragment();
        
        // 执行Fragment事务
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, aboutFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    /**
     * 退出登录
     */
    private void logout() {
        // 执行退出登录操作
        localDataManager.signOut();
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();
        
        // 跳转到登录页面
        Intent intent = new Intent(requireContext(), com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}