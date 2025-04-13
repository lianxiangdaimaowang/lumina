package com.lianxiangdaimaowang.lumina.community;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ProgressBar;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 社区页面Fragment
 */
public class CommunityFragment extends Fragment {
    private static final String TAG = "CommunityFragment";
    private static final int HOT_POSTS_LIMIT = 3; // 热门帖子显示数量
    
    private LocalDataManager localDataManager;
    private NetworkManager networkManager;
    private ListView lvPosts;
    private FloatingActionButton fabSend;
    private PostAdapter postAdapter;
    private List<LocalPost> posts;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    private TextView tvEmptyMessage;
    private SyncManager syncManager;
    private ProgressBar progressBar;
    
    // TabLayout相关
    private TabLayout tabLayout;
    private static final int TAB_LATEST = 0;
    private static final int TAB_HOT = 1;
    private static final int TAB_FAVORITE = 2;
    private int currentTab = TAB_LATEST;
    
    // 广播接收器，用于接收帖子删除通知
    private BroadcastReceiver postDeletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.lianxiangdaimaowang.lumina.POST_DELETED".equals(intent.getAction())) {
                String deletedPostId = intent.getStringExtra("post_id");
                Log.d(TAG, "社区页面收到帖子删除广播: " + deletedPostId);
                
                // 刷新UI以反映帖子删除
                loadPosts();
            }
        }
    };

    // 添加缓存变量
    private List<LocalPost> cachedLatestPosts = new ArrayList<>();
    private List<LocalPost> cachedHotPosts = new ArrayList<>();
    private long lastSyncTimeMs = 0;
    private static final long SYNC_INTERVAL_MS = 30000; // 30秒同步间隔

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        
        try {
            // 初始化LocalDataManager
            localDataManager = LocalDataManager.getInstance(requireContext());
            
            // 初始化NetworkManager
            networkManager = NetworkManager.getInstance(requireContext());
            
            // 初始化SyncManager
            syncManager = SyncManager.getInstance(requireContext());
            
            // 初始化视图
            initViews(view);
            
            // 加载帖子数据
            loadPosts();
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: 初始化社区页面失败", e);
            Toast.makeText(requireContext(), "加载社区失败", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次页面恢复时刷新数据
        if (isAdded() && getContext() != null) {
            loadPosts();
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("com.lianxiangdaimaowang.lumina.POST_DELETED");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(postDeletedReceiver, filter);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        // 注销广播接收器
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(postDeletedReceiver);
    }
    
    private void initViews(View view) {
        try {
            // 初始化视图
            lvPosts = view.findViewById(R.id.lv_posts);
            fabSend = view.findViewById(R.id.fab_post);
            emptyView = view.findViewById(R.id.layout_empty);
            tvEmptyMessage = view.findViewById(R.id.text_empty);
            progressBar = view.findViewById(R.id.progress_bar);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
            tabLayout = view.findViewById(R.id.tab_layout);
            
            // 初始化帖子列表
            posts = new ArrayList<>();
            
            // 确保ListView有固定高度
            ViewGroup.LayoutParams params = lvPosts.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            lvPosts.setLayoutParams(params);
            
            // 设置下拉刷新布局
            swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.colorPrimary,
                    R.color.colorAccent,
                    R.color.colorPrimaryDark);
            
            // 设置发送按钮点击事件
            if (fabSend != null) {
                fabSend.setOnClickListener(v -> {
                    showCreatePostDialog();
                });
            }
            
            // 设置TabLayout事件
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    int position = tab.getPosition();
                    currentTab = position;
                    Log.d(TAG, "切换到标签: " + (position == TAB_LATEST ? "最新" : position == TAB_HOT ? "热门" : "收藏"));
                    
                    // 清空当前帖子列表
                    if (posts != null) {
                        posts.clear();
                    } else {
                        posts = new ArrayList<>();
                    }
                    
                    // 如果有适配器，通知它数据已经变化
                    if (postAdapter != null) {
                        postAdapter.notifyDataSetChanged();
                    }
                    
                    // 加载新标签的帖子
                    loadPostsByTab(position);
                }
                
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // 不需要处理
                }
                
                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // 刷新当前选中的列表
                    loadPostsByTab(tab.getPosition());
                }
            });
            
            // 设置长按删除
            lvPosts.setOnItemLongClickListener((parent, postView, position, id) -> {
                if (localDataManager.isSignedIn()) {
                    LocalPost post = posts.get(position);
                    // 检查是否是当前用户发布的帖子
                    if (post.getUserId() != null && post.getUserId().equals(localDataManager.getCurrentUserId())) {
                        confirmDeletePost(post);
                        return true;
                    } else {
                        Toast.makeText(requireContext(), "只能删除自己发布的帖子", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
                }
                return false;
            });
        } catch (Exception e) {
            Log.e(TAG, "initViews: 初始化视图失败", e);
        }
    }
    
    /**
     * 根据当前选中的Tab加载不同的帖子列表
     */
    private void loadPostsByTab(int tabPosition) {
        showLoading(true);
        
        switch (tabPosition) {
            case TAB_LATEST:
                // 加载最新帖子
                loadLatestPosts();
                break;
            case TAB_HOT:
                // 加载热门帖子
                loadHotPosts();
                break;
            case TAB_FAVORITE:
                // 加载收藏帖子
                loadFavoritePosts();
                break;
        }
    }
    
    /**
     * 加载最新帖子
     */
    private void loadLatestPosts() {
        // 显示加载进度
        showLoading(true);
        
        // 如果有缓存且最后同步时间在30秒内，先显示缓存
        long currentTimeMs = System.currentTimeMillis();
        if (!cachedLatestPosts.isEmpty() && (currentTimeMs - lastSyncTimeMs < SYNC_INTERVAL_MS)) {
            Log.d(TAG, "使用最新帖子缓存，共 " + cachedLatestPosts.size() + " 条");
            posts = new ArrayList<>(cachedLatestPosts);
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            updatePostsUI();
            return;
        }

        // 没有缓存或间隔时间已到，显示刷新指示器
        swipeRefreshLayout.setRefreshing(true);
        
        // 检查网络状态
        if (!networkManager.isNetworkConnected()) {
            loadLocalPosts();
            return;
        }
        
        // 从服务器加载帖子
        loadPostsFromServer();
    }

    private void loadPostsFromServer() {
        // 从服务器获取帖子列表
        syncManager.getAllPostsFromServer(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (isAdded() && getContext() != null) {
                    lastSyncTimeMs = System.currentTimeMillis();
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        
                        // 从本地数据库加载同步后的帖子
                        posts = localDataManager.getAllLocalPosts();
                        
                        // 设置Context
                        for (LocalPost post : posts) {
                            post.setContext(requireContext());
                        }
                        
                        // 按时间排序（新的在前面）
                        Collections.sort(posts, (p1, p2) -> {
                            if (p1.getCreateTime() == null) return 1;
                            if (p2.getCreateTime() == null) return -1;
                            return p2.getCreateTime().compareTo(p1.getCreateTime());
                        });
                        
                        // 更新缓存
                        cachedLatestPosts = new ArrayList<>(posts);
                        
                        updatePostsUI();
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        
                        // 显示错误信息
                        Toast.makeText(requireContext(), "加载帖子失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "帖子同步失败: " + errorMessage);
                        
                        // 在服务器加载失败时，尝试加载本地缓存数据
                        loadLocalPosts();
                    });
                }
            }
        });
    }
    
    /**
     * 加载热门帖子
     */
    private void loadHotPosts() {
        // 显示加载进度
        showLoading(true);
        
        // 如果有缓存且最后同步时间在30秒内，先显示缓存
        long currentTimeMs = System.currentTimeMillis();
        if (!cachedHotPosts.isEmpty() && (currentTimeMs - lastSyncTimeMs < SYNC_INTERVAL_MS)) {
            Log.d(TAG, "使用热门帖子缓存，共 " + cachedHotPosts.size() + " 条");
            posts = new ArrayList<>(cachedHotPosts);
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            updatePostsUI();
            return;
        }

        // 没有缓存或间隔时间已到，显示刷新指示器
        swipeRefreshLayout.setRefreshing(true);
        
        // 首先尝试从本地加载热门帖子，减少白屏时间
        posts = localDataManager.getHotLocalPosts(HOT_POSTS_LIMIT);
        if (!posts.isEmpty()) {
            // 设置Context
            for (LocalPost post : posts) {
                post.setContext(requireContext());
            }
            showLoading(false);
            updatePostsUI();
        }
        
        // 检查网络状态
        if (!networkManager.isNetworkConnected()) {
            swipeRefreshLayout.setRefreshing(false);
            if (posts.isEmpty()) {
                Toast.makeText(requireContext(), "无网络连接，无法加载热门帖子", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        // 从服务器获取热门帖子
        syncManager.getHotPosts(HOT_POSTS_LIMIT, new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (isAdded() && getContext() != null) {
                    lastSyncTimeMs = System.currentTimeMillis();
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        
                        // 从本地数据库获取热门帖子
                        posts = localDataManager.getHotLocalPosts(HOT_POSTS_LIMIT);
                        
                        // 设置Context
                        for (LocalPost post : posts) {
                            post.setContext(requireContext());
                        }
                        
                        // 更新缓存
                        cachedHotPosts = new ArrayList<>(posts);
                        
                        updatePostsUI();
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        
                        // 只在没有本地数据时显示错误
                        if (posts.isEmpty()) {
                            Toast.makeText(requireContext(), "加载热门帖子失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                            
                            // 本地获取排序后的帖子
                            posts = localDataManager.getHotLocalPosts(HOT_POSTS_LIMIT);
                            
                            // 设置Context
                            for (LocalPost post : posts) {
                                post.setContext(requireContext());
                            }
                            
                            updatePostsUI();
                        }
                    });
                }
            }
        });
    }
    
    /**
     * 加载收藏帖子
     */
    private void loadFavoritePosts() {
        // 检查用户是否登录
        if (!localDataManager.isSignedIn()) {
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            posts.clear();
            updatePostsUI();
            return;
        }
        
        // 显示加载中状态
        showLoading(true);
        swipeRefreshLayout.setRefreshing(true);
        
        // 获取当前用户ID用于调试
        String currentUserId = localDataManager.getCurrentUserId();
        Log.d(TAG, "加载收藏 - 当前用户ID: " + currentUserId);
        
        // 优先从本地数据库加载收藏，减少等待时间
        List<LocalPost> localFavorites = localDataManager.getUserFavoritePosts();
        
        Log.d(TAG, "从本地找到 " + localFavorites.size() + " 条收藏帖子");
        
        if (!localFavorites.isEmpty()) {
            Log.d(TAG, "从本地找到 " + localFavorites.size() + " 条收藏帖子，立即显示");
            showLoading(false);
            swipeRefreshLayout.setRefreshing(true); // 继续显示刷新状态，但内容已可见
            
            // 使用本地帖子立即更新UI
            posts = localFavorites;
            
            // 设置Context
            for (LocalPost post : posts) {
                post.setContext(requireContext());
                // 输出帖子信息进行调试
                Log.d(TAG, "本地收藏帖子 - ID: " + post.getId() + 
                        ", 标题: " + post.getTitle() + 
                        ", 收藏用户: " + post.getFavorites());
            }
            
            // 按时间排序（新的在前面）
            Collections.sort(posts, (p1, p2) -> {
                if (p1.getCreateTime() == null) return 1;
                if (p2.getCreateTime() == null) return -1;
                return p2.getCreateTime().compareTo(p1.getCreateTime());
            });
            
            updatePostsUI();
        } else {
            Log.d(TAG, "本地未找到收藏帖子，等待同步...");
        }
        
        // 在后台同步最新的服务器数据
        syncManager.getUserFavoritePosts(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        
                        // 获取收藏帖子
                        List<LocalPost> syncedFavorites = localDataManager.getUserFavoritePosts();
                        Log.d(TAG, "收藏标签 - 同步后获取到 " + syncedFavorites.size() + " 条收藏帖子");
                        
                        // 如果没有显示本地收藏，或者同步后的收藏数量变化，则更新UI
                        if (posts.isEmpty() || posts.size() != syncedFavorites.size()) {
                            posts = syncedFavorites;
                            
                            // 设置Context
                            for (LocalPost post : posts) {
                                post.setContext(requireContext());
                                // 输出帖子信息进行调试
                                Log.d(TAG, "同步后收藏帖子 - ID: " + post.getId() + 
                                        ", 标题: " + post.getTitle() + 
                                        ", 收藏用户: " + post.getFavorites() +
                                        ", 当前用户ID: " + localDataManager.getCurrentUserId() +
                                        ", 是否收藏: " + post.isFavoritedBy(localDataManager.getCurrentUserId()));
                            }
                            
                            // 按时间排序（新的在前面）
                            Collections.sort(posts, (p1, p2) -> {
                                if (p1.getCreateTime() == null) return 1;
                                if (p2.getCreateTime() == null) return -1;
                                return p2.getCreateTime().compareTo(p1.getCreateTime());
                            });
                            
                            updatePostsUI();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        
                        // 如果之前没有显示本地收藏，则在错误时尝试再次从本地加载
                        if (posts.isEmpty()) {
                            posts = localDataManager.getUserFavoritePosts();
                            Log.d(TAG, "收藏错误后本地尝试 - 获取到 " + posts.size() + " 条收藏帖子");
                            
                            // 设置Context
                            for (LocalPost post : posts) {
                                post.setContext(requireContext());
                            }
                            
                            // 按时间排序
                            if (posts.size() > 0) {
                                Collections.sort(posts, (p1, p2) -> {
                                    if (p1.getCreateTime() == null) return 1;
                                    if (p2.getCreateTime() == null) return -1;
                                    return p2.getCreateTime().compareTo(p1.getCreateTime());
                                });
                            }
                            
                            updatePostsUI();
                        } else if (!errorMessage.contains("您已经收藏过了")) {
                            // 除非是"已收藏"错误，否则显示错误提示
                            Toast.makeText(requireContext(), "同步收藏帖子失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    
    /**
     * 显示创建帖子对话框
     */
    private void showCreatePostDialog() {
        try {
            // 创建对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null);
            TextInputEditText etDialogPostTitle = dialogView.findViewById(R.id.et_dialog_post_title);
            TextInputEditText etDialogPostContent = dialogView.findViewById(R.id.et_dialog_post_content);
            
            builder.setView(dialogView)
                    .setPositiveButton("发布", (dialog, which) -> {
                        // 获取标题和内容并发布
                        String title = etDialogPostTitle.getText().toString().trim();
                        String content = etDialogPostContent.getText().toString().trim();
                        
                        if (title.isEmpty()) {
                            Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        if (content.isEmpty()) {
                            Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        createPost(title, content);
                    })
                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
            
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "showCreatePostDialog: 显示发帖对话框失败", e);
            Toast.makeText(requireContext(), "显示发帖对话框失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 加载帖子
     */
    private void loadPosts() {
        Log.d(TAG, "加载帖子 - 当前标签索引: " + currentTab);
        
        switch (currentTab) {
            case TAB_LATEST:
                loadLatestPosts();
                break;
            case TAB_HOT:
                loadHotPosts();
                break;
            case TAB_FAVORITE:
                loadFavoritePosts();
                break;
        }
    }
    
    private void loadLocalPosts() {
        // 在服务器加载失败时加载本地帖子，作为备份
        if (isAdded() && getContext() != null) {
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            
            posts = localDataManager.getAllLocalPosts();
            
            // 设置Context
            for (LocalPost post : posts) {
                post.setContext(requireContext());
            }
            
            // 按时间排序（新的在前面）
            Collections.sort(posts, (p1, p2) -> {
                if (p1.getCreateTime() == null) return 1;
                if (p2.getCreateTime() == null) return -1;
                return p2.getCreateTime().compareTo(p1.getCreateTime());
            });
            
            updatePostsUI();
            
            // 如果本地没有帖子，显示提示
            if (posts.isEmpty()) {
                Toast.makeText(requireContext(), "暂无帖子，请检查网络连接", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void updatePostsUI() {
        // 更新UI
        if (posts == null || posts.isEmpty()) {
            // 没有帖子，显示空视图
            emptyView.setVisibility(View.VISIBLE);
            lvPosts.setVisibility(View.GONE);
            
            // 根据当前Tab显示不同的提示信息
            switch (currentTab) {
                case TAB_HOT:
                    tvEmptyMessage.setText("暂无热门帖子，去点赞一些帖子吧");
                    break;
                case TAB_FAVORITE:
                    tvEmptyMessage.setText("暂无收藏帖子，去收藏一些帖子吧");
                    break;
                default:
                    tvEmptyMessage.setText("暂无帖子，点击发布按钮创建新帖子");
                    break;
            }
            
            // 输出日志，帮助调试
            Log.d(TAG, "显示空视图 - 当前标签: " + (currentTab == TAB_LATEST ? "最新" : currentTab == TAB_HOT ? "热门" : "收藏"));
        } else {
            // 有帖子，显示列表
            emptyView.setVisibility(View.GONE);
            lvPosts.setVisibility(View.VISIBLE);
            
            // 输出日志，帮助调试
            Log.d(TAG, "更新UI: 共有 " + posts.size() + " 条帖子");
            for (LocalPost post : posts) {
                Log.d(TAG, "帖子: ID=" + post.getId() + ", 内容=" + post.getContent());
            }
            
            // 重新设置Adapter而不是只调用notifyDataSetChanged
            // 这确保了在切换标签时列表会完全刷新
            postAdapter = new PostAdapter(requireContext(), posts);
            lvPosts.setAdapter(postAdapter);
        }
    }
    
    /**
     * 显示或隐藏加载进度条
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 发布帖子
     */
    private void createPost(String title, String content) {
        // 检查标题和内容是否为空
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查用户是否已登录
        LocalDataManager localDataManager = LocalDataManager.getInstance(requireContext());
        if (!localDataManager.isSignedIn()) {
            Log.w(TAG, "创建帖子失败：用户未登录");
            Toast.makeText(requireContext(), "请先登录后再发布帖子", Toast.LENGTH_LONG).show();
            
            // 跳转到登录界面
            Intent intent = new Intent(requireContext(), com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        
        // 检查用户令牌是否存在
        String token = localDataManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "创建帖子失败：认证令牌为空");
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_LONG).show();
            
            // 清除登录状态
            localDataManager.signOut();
            localDataManager.clearAuthToken();
            
            // 跳转到登录界面
            Intent intent = new Intent(requireContext(), com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        
        // 获取有效的用户ID
        String effectiveUserId = localDataManager.getEffectiveUserId();
        String currentUserId = localDataManager.getCurrentUserId();
        String serverUserId = localDataManager.getServerUserId(currentUserId);
        
        Log.d(TAG, "创建帖子 - 当前用户ID: " + currentUserId);
        Log.d(TAG, "创建帖子 - 服务器用户ID: " + serverUserId);
        Log.d(TAG, "创建帖子 - 有效用户ID: " + effectiveUserId);
        Log.d(TAG, "创建帖子 - 认证令牌: " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
        
        // 显示加载进度
        showLoading(true);
        
        // 创建帖子对象
        LocalPost post = new LocalPost();
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        post.setContext(requireContext());
        post.setUserId(effectiveUserId);
        
        // 直接上传到服务器
        syncManager.createPost(post, new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        // 隐藏加载进度
                        showLoading(false);
                        
                        // 提示发布成功
                        Toast.makeText(requireContext(), "发布成功", Toast.LENGTH_SHORT).show();
                        
                        // 刷新帖子列表
                        loadPosts();
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        // 隐藏加载进度
                        showLoading(false);
                        
                        // 提示发布失败
                        Toast.makeText(requireContext(), "发布失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * 确认删除帖子
     */
    private void confirmDeletePost(LocalPost post) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("删除帖子")
                .setMessage("确定要删除这条帖子吗？")
                .setPositiveButton("确定", (dialog, which) -> deletePost(post))
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除帖子
     */
    private void deletePost(LocalPost post) {
        // 显示加载进度
        showLoading(true);
        
        if (syncManager != null) {
            syncManager.deletePost(post.getId(), new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded() && getContext() != null) {
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                            
                            // 发送广播通知其他页面帖子已删除
                            Intent intent = new Intent("com.lianxiangdaimaowang.lumina.POST_DELETED");
                            intent.putExtra("post_id", post.getId());
                            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
                            
                            // 刷新帖子列表
                            loadPosts();
                        });
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    if (isAdded() && getContext() != null) {
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            
                            // 本地删除
                            localDataManager.deleteLocalPost(post.getId());
                            
                            // 刷新帖子列表
                            loadLocalPosts();
                        });
                    }
                }
            });
        } else {
            // 本地删除
            localDataManager.deleteLocalPost(post.getId());
            showLoading(false);
            
            // 刷新帖子列表
            loadLocalPosts();
        }
    }
} 