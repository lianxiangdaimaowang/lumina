package com.lianxiangdaimaowang.lumina;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;
import com.lianxiangdaimaowang.lumina.note.NoteEditActivity;
import com.lianxiangdaimaowang.lumina.note.NotesFragment;
import com.lianxiangdaimaowang.lumina.profile.ProfileFragment;
import com.lianxiangdaimaowang.lumina.review.ReviewFragment;

public class MainActivity extends BaseActivity {
    
    private static final String TAG = "MainActivity";
    
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;
    
    private LocalDataManager localDataManager;
    private NetworkManager networkManager;
    
    // 当前显示的Fragment类型
    private enum FragmentType {
        NOTES, REVIEW, PROFILE
    }
    
    private FragmentType currentFragmentType = FragmentType.NOTES;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        // 初始化各种管理器
        localDataManager = LocalDataManager.getInstance(this);
        networkManager = NetworkManager.getInstance(this);
        
        // 初始化视图
        initViews();
        
        // 设置默认Fragment
        if (savedInstanceState == null) {
            loadFragment(new NotesFragment());
            bottomNav.setSelectedItemId(R.id.action_notes);
        }
    }
    
    private void initViews() {
        // 设置底部导航
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_notes) {
                currentFragmentType = FragmentType.NOTES;
                loadFragment(new NotesFragment());
                fabAdd.setOnClickListener(v -> {
                    // 创建新笔记
                    Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                    startActivity(intent);
                });
                fabAdd.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.action_review) {
                currentFragmentType = FragmentType.REVIEW;
                loadFragment(new ReviewFragment());
                fabAdd.setOnClickListener(v -> {
                    // 创建新复习计划
                    Intent intent = new Intent(MainActivity.this, com.lianxiangdaimaowang.lumina.review.AddReviewPlanActivity.class);
                    startActivity(intent);
                });
                fabAdd.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.action_profile) {
                currentFragmentType = FragmentType.PROFILE;
                loadFragment(new ProfileFragment());
                fabAdd.setVisibility(View.GONE);
                return true;
            }
            return false;
        });
        
        // 初始化浮动按钮
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 默认创建新笔记
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                startActivity(intent);
            }
        });
    }
    
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}