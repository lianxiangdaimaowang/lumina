package com.lianxiangdaimaowang.lumina.review;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.lianxiangdaimaowang.lumina.BaseActivity;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.model.ReviewPlan;

import java.util.UUID;

/**
 * 添加复习计划的页面
 */
public class AddReviewPlanActivity extends BaseActivity {
    private static final String TAG = "AddReviewPlanActivity";
    
    private LocalDataManager localDataManager;
    private EditText titleEdit;
    private EditText contentEdit;
    private Button saveButton;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review_plan);
        
        // 初始化数据
        localDataManager = LocalDataManager.getInstance(this);
        
        // 初始化控件
        setupViews();
    }
    
    private void setupViews() {
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.review_plan));
        
        // 初始化视图
        titleEdit = findViewById(R.id.edit_title);
        contentEdit = findViewById(R.id.edit_content);
        saveButton = findViewById(R.id.button_save);
        
        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> saveReviewPlan());
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void saveReviewPlan() {
        // 获取输入内容
        String title = titleEdit.getText().toString().trim();
        String content = contentEdit.getText().toString().trim();
        
        // 验证输入
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, R.string.error_title_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, R.string.error_empty_title, Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // 创建复习计划（使用随机ID作为笔记ID，实际应用中可能需要关联到真实笔记）
            String noteId = UUID.randomUUID().toString();
            ReviewPlan reviewPlan = new ReviewPlan(noteId, title, content);
            reviewPlan.setUserId(localDataManager.getCurrentUserId());
            
            // 保存到本地
            localDataManager.saveReviewPlan(reviewPlan);
            
            // 提示成功并返回
            Toast.makeText(this, R.string.toast_success, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, R.string.add_review_plan_fail, Toast.LENGTH_SHORT).show();
        }
    }
} 