package com.lianxiangdaimaowang.lumina.review;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.lianxiangdaimaowang.lumina.BaseActivity;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.model.ReviewPlan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

/**
 * 复习计划详情页面
 */
public class ReviewDetailActivity extends BaseActivity {
    private static final String TAG = "ReviewDetailActivity";
    
    private LocalDataManager localDataManager;
    private ReviewPlan reviewPlan;
    
    private TextView titleText;
    private TextView contentText;
    private TextView stageText;
    private TextView nextReviewText;
    private TextView reviewHistoryText;
    private Button completeButton;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_detail);
        
        // 初始化数据
        localDataManager = LocalDataManager.getInstance(this);
        
        // 获取传递的计划ID
        String planId = getIntent().getStringExtra("planId");
        if (planId == null || planId.isEmpty()) {
            Toast.makeText(this, R.string.toast_error_loading, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 加载复习计划
        reviewPlan = localDataManager.getReviewPlan(planId);
        if (reviewPlan == null) {
            Toast.makeText(this, R.string.toast_error_loading, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化控件
        setupViews();
        
        // 加载数据
        loadReviewPlanData();
    }
    
    private void setupViews() {
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.review_plan));
        
        // 初始化视图
        titleText = findViewById(R.id.text_title);
        contentText = findViewById(R.id.text_content);
        stageText = findViewById(R.id.text_stage);
        nextReviewText = findViewById(R.id.text_next_review);
        reviewHistoryText = findViewById(R.id.text_review_history);
        completeButton = findViewById(R.id.button_complete);
        
        // 设置完成按钮点击事件
        completeButton.setOnClickListener(v -> completeReview());
    }
    
    private void loadReviewPlanData() {
        // 设置标题和内容
        titleText.setText(reviewPlan.getNoteTitle());
        contentText.setText(reviewPlan.getNoteContent());
        
        // 设置阶段信息
        String stageInfo = String.format(getString(R.string.review_stage), reviewPlan.getCurrentStage() + 1);
        stageText.setText(stageInfo);
        
        // 设置下次复习时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (reviewPlan.getNextReviewDate() != null) {
            String nextReview = String.format(getString(R.string.review_next_date), dateFormat.format(reviewPlan.getNextReviewDate()));
            nextReviewText.setText(nextReview);
        } else {
            nextReviewText.setText(R.string.review_completed);
        }
        
        // 设置复习历史
        StringBuilder historyBuilder = new StringBuilder(getString(R.string.title_review) + "：\n");
        List<Date> reviewDates = reviewPlan.getReviewDates();
        if (reviewDates != null && !reviewDates.isEmpty()) {
            for (int i = 0; i < reviewDates.size(); i++) {
                historyBuilder.append(String.format(getString(R.string.review_stage) + ": %s\n", i + 1, dateFormat.format(reviewDates.get(i))));
            }
        } else {
            historyBuilder.append(getString(R.string.today_no_review));
        }
        reviewHistoryText.setText(historyBuilder.toString());
        
        // 如果已完成所有复习，隐藏完成按钮
        if (reviewPlan.isCompleted()) {
            completeButton.setVisibility(View.GONE);
        } else {
            completeButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void completeReview() {
        if (reviewPlan != null) {
            // 完成当前复习并计算下一次复习日期
            reviewPlan.completeCurrentReview();
            
            // 保存更新后的复习计划
            localDataManager.saveReviewPlan(reviewPlan);
            
            // 刷新页面数据
            loadReviewPlanData();
            
            // 提示完成
            Toast.makeText(this, R.string.review_completed_message, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}