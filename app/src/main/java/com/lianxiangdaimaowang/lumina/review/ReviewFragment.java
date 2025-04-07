package com.lianxiangdaimaowang.lumina.review;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.model.ReviewPlan;
import com.lianxiangdaimaowang.lumina.note.NoteDetailActivity;
import com.lianxiangdaimaowang.lumina.review.ReviewDetailActivity;
import com.lianxiangdaimaowang.lumina.review.AddReviewPlanActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;

public class ReviewFragment extends Fragment implements ReviewPlanAdapter.OnReviewPlanClickListener {
    private static final String TAG = "ReviewFragment";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView todayRecycler;
    private RecyclerView upcomingRecycler;
    private RecyclerView completedRecycler;
    private TextView todayEmptyText;
    private TextView upcomingEmptyText;
    private TextView completedEmptyText;
    private TextView emptyText;
    private TextView todayCountText;
    private TextView upcomingCountText;
    private TextView completedCountText;
    private View layoutEmpty; // 全局空视图布局
    private View cardTodayReview; // 今日复习卡片
    private View cardUpcomingReview; // 即将复习卡片
    private View cardCompletedReview; // 已完成复习卡片
    private FloatingActionButton fabAddReview; // 添加复习计划按钮

    private LocalDataManager localDataManager;
    private List<ReviewPlan> todayPlans;
    private List<ReviewPlan> upcomingPlans;
    private List<ReviewPlan> completedPlans;
    private ReviewPlanAdapter todayAdapter;
    private ReviewPlanAdapter upcomingAdapter;
    private ReviewPlanAdapter completedAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            localDataManager = LocalDataManager.getInstance(requireContext());
            todayPlans = new ArrayList<>();
            upcomingPlans = new ArrayList<>();
            completedPlans = new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: 初始化失败", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review, container, false);
        
        // 初始化SwipeRefreshLayout
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
        
        try {
            setupViews(view);
            setupRecyclers();
            setupSwipeRefresh(view);
            
            // 加载本地复习计划
            loadReviewPlans();
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: 初始化视图失败", e);
            Toast.makeText(requireContext(), "加载复习功能失败", Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // 设置添加按钮点击事件
            if (fabAddReview != null) {
                fabAddReview.setOnClickListener(v -> {
                    // 跳转到添加复习计划页面
                    Intent intent = new Intent(requireContext(), AddReviewPlanActivity.class);
                    startActivity(intent);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: 设置添加按钮失败", e);
        }
    }

    private void setupViews(View view) {
        try {
            todayRecycler = view.findViewById(R.id.recycler_today);
            upcomingRecycler = view.findViewById(R.id.recycler_upcoming);
            completedRecycler = view.findViewById(R.id.recycler_completed);
            todayEmptyText = view.findViewById(R.id.text_today_empty);
            upcomingEmptyText = view.findViewById(R.id.text_upcoming_empty);
            completedEmptyText = view.findViewById(R.id.text_completed_empty);
            emptyText = view.findViewById(R.id.text_empty);
            todayCountText = view.findViewById(R.id.text_today_count);
            upcomingCountText = view.findViewById(R.id.text_upcoming_count);
            completedCountText = view.findViewById(R.id.text_completed_count);
            
            // 初始化新增视图
            layoutEmpty = view.findViewById(R.id.layout_empty);
            cardTodayReview = view.findViewById(R.id.card_today_review);
            cardUpcomingReview = view.findViewById(R.id.card_upcoming_review);
            cardCompletedReview = view.findViewById(R.id.card_completed_review);
            fabAddReview = view.findViewById(R.id.fab_add_review);
            
            // 隐藏fragment中的按钮，因为MainActivity中已经有一个全局FAB
            if (fabAddReview != null) {
                fabAddReview.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "setupViews: 初始化视图失败", e);
        }
    }
    
    private void setupRecyclers() {
        try {
            // 今日复习
            todayAdapter = new ReviewPlanAdapter(todayPlans, this);
            todayRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            todayRecycler.setAdapter(todayAdapter);
            
            // 即将复习
            upcomingAdapter = new ReviewPlanAdapter(upcomingPlans, this);
            upcomingRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            upcomingRecycler.setAdapter(upcomingAdapter);
            
            // 已完成复习
            completedAdapter = new ReviewPlanAdapter(completedPlans, this);
            completedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            completedRecycler.setAdapter(completedAdapter);
        } catch (Exception e) {
            Log.e(TAG, "setupRecyclers: 初始化列表失败", e);
        }
    }
    
    private void setupSwipeRefresh(View view) {
        try {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeResources(
                        R.color.colorPrimary,
                        R.color.colorAccent,
                        R.color.colorPrimaryDark
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "setupSwipeRefresh: 初始化下拉刷新失败", e);
        }
    }
    
    private void loadReviewPlans() {
        try {
            // 清空现有列表
            todayPlans.clear();
            upcomingPlans.clear();
            completedPlans.clear();
            
            // 从LocalDataManager加载复习计划
            List<ReviewPlan> allPlans = localDataManager.getAllReviewPlans();
            
            // 如果没有复习计划，生成示例数据（仅用于开发测试）
            if (allPlans.isEmpty()) {
                // 删除createSampleReviewPlans()调用，不再生成模拟数据
            } else {
                // 根据复习日期和完成状态分类
                Date now = new Date();
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                
                Calendar tomorrow = (Calendar) today.clone();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                
                for (ReviewPlan plan : allPlans) {
                    if (plan.isCompleted()) {
                        completedPlans.add(plan);
                    } else if (plan.getNextReviewDate() != null) {
                        if (plan.getNextReviewDate().before(tomorrow.getTime()) &&
                            plan.getNextReviewDate().after(today.getTime())) {
                            todayPlans.add(plan);
                        } else {
                            upcomingPlans.add(plan);
                        }
                    }
                }
            }
            
            // 更新UI
            updateUI();
            
            // 停止刷新动画
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadReviewPlans: 加载复习计划失败", e);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }
    
    private void updateUI() {
        try {
            // 显示今日复习计划数量
            String todayCountFormatted = getString(R.string.today_review_count, todayPlans.size());
            todayCountText.setText(todayCountFormatted);
            
            // 显示即将复习计划数量
            String upcomingCountFormatted = getString(R.string.upcoming_review_count, upcomingPlans.size());
            upcomingCountText.setText(upcomingCountFormatted);
            
            // 显示已完成复习计划数量
            String completedCountFormatted = getString(R.string.completed_review_count, completedPlans.size());
            completedCountText.setText(completedCountFormatted);
            
            // 更新适配器
            todayAdapter.notifyDataSetChanged();
            upcomingAdapter.notifyDataSetChanged();
            completedAdapter.notifyDataSetChanged();
            
            // 处理空视图逻辑
            boolean hasAnyPlans = !todayPlans.isEmpty() || !upcomingPlans.isEmpty() || !completedPlans.isEmpty();
            
            // 设置全局空视图的可见性
            layoutEmpty.setVisibility(hasAnyPlans ? View.GONE : View.VISIBLE);
            
            // 今日复习卡片空视图
            todayEmptyText.setVisibility(todayPlans.isEmpty() ? View.VISIBLE : View.GONE);
            cardTodayReview.setVisibility(hasAnyPlans ? View.VISIBLE : View.GONE);
            
            // 即将复习卡片空视图
            upcomingEmptyText.setVisibility(upcomingPlans.isEmpty() ? View.VISIBLE : View.GONE);
            cardUpcomingReview.setVisibility(hasAnyPlans ? View.VISIBLE : View.GONE);
            
            // 已完成复习卡片空视图
            completedEmptyText.setVisibility(completedPlans.isEmpty() ? View.VISIBLE : View.GONE);
            cardCompletedReview.setVisibility(hasAnyPlans ? View.VISIBLE : View.GONE);
            
            // 停止刷新动画
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateUI: 更新界面失败", e);
        }
    }
    
    @Override
    public void onReviewClick(ReviewPlan reviewPlan) {
        try {
            if (reviewPlan == null) return;
            
            // 打开复习计划详情页面
            Intent intent = new Intent(requireContext(), ReviewDetailActivity.class);
            intent.putExtra("planId", reviewPlan.getId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "onReviewClick: 打开复习详情失败", e);
            Toast.makeText(requireContext(), "无法打开复习详情", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onCompleteClick(ReviewPlan reviewPlan) {
        try {
            if (reviewPlan == null || localDataManager == null) {
                Toast.makeText(requireContext(), "无法完成复习", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 标记当前复习阶段为已完成
            reviewPlan.completeCurrentReview();
            
            // 重新分类复习计划
            todayPlans.remove(reviewPlan);
            upcomingPlans.remove(reviewPlan);
            completedPlans.remove(reviewPlan);
            
            // 根据完成状态和下一次复习日期重新分类
            if (reviewPlan.isCompleted()) {
                completedPlans.add(reviewPlan);
            } else {
                Date now = new Date();
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                
                Calendar tomorrow = (Calendar) today.clone();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                
                if (reviewPlan.getNextReviewDate().before(tomorrow.getTime()) && 
                    reviewPlan.getNextReviewDate().after(today.getTime())) {
                    todayPlans.add(reviewPlan);
                } else {
                    upcomingPlans.add(reviewPlan);
                }
            }
            
            // 保存复习计划到本地
            localDataManager.saveReviewPlan(reviewPlan);
            
            // 更新UI
            updateUI();
            Toast.makeText(requireContext(), "复习已完成", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "onCompleteClick: 完成复习失败", e);
            Toast.makeText(requireContext(), "无法完成复习", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(ReviewPlan reviewPlan) {
        try {
            if (reviewPlan == null || localDataManager == null) {
                Toast.makeText(requireContext(), "无法删除复习计划", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 弹出确认对话框
            new AlertDialog.Builder(requireContext())
                    .setTitle("删除复习计划")
                    .setMessage("确定要删除这个复习计划吗？此操作不可恢复。")
                    .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 从列表中删除
                            todayPlans.remove(reviewPlan);
                            upcomingPlans.remove(reviewPlan);
                            completedPlans.remove(reviewPlan);
                            
                            // 从本地存储中删除
                            localDataManager.deleteReviewPlan(reviewPlan.getId());
                            
                            // 更新UI
                            updateUI();
                            Toast.makeText(requireContext(), "复习计划已删除", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            
        } catch (Exception e) {
            Log.e(TAG, "onDeleteClick: 删除复习计划失败", e);
            Toast.makeText(requireContext(), "无法删除复习计划", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        try {
            loadReviewPlans();
        } catch (Exception e) {
            Log.e(TAG, "refreshData: 刷新数据失败", e);
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复时刷新数据
        refreshData();
    }
} 