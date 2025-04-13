package com.lianxiangdaimaowang.lumina;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.note.NoteEditActivity;
import com.lianxiangdaimaowang.lumina.note.NotesFragment;
import com.lianxiangdaimaowang.lumina.profile.ProfileFragment;
import com.lianxiangdaimaowang.lumina.dailypush.DailyPushFragment;
import com.lianxiangdaimaowang.lumina.community.CommunityFragment;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;

import java.util.List;

public class MainActivity extends BaseActivity {
    
    private static final String TAG = "MainActivity";
    private static final String TAG_NOTES = "tag_notes";
    private static final String TAG_DAILY_PUSH = "tag_daily_push";
    private static final String TAG_COMMUNITY = "tag_community";
    private static final String TAG_PROFILE = "tag_profile";
    
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;
    
    private LocalDataManager localDataManager;
    private NetworkManager networkManager;
    private NoteRepository noteRepository;
    private SyncManager syncManager;
    private boolean isSyncing = false;
    
    // 认证失败广播接收器
    private BroadcastReceiver authFailedReceiver;
    
    // 递归保护变量
    private boolean isRefreshingFragment = false;
    private boolean isSwitchingFragment = false;
    private int fragmentRefreshRetryCount = 0;
    private static final int MAX_FRAGMENT_RETRY = 3;
    
    // 当前显示的Fragment类型
    private enum FragmentType {
        NOTES, DAILY_PUSH, COMMUNITY, PROFILE
    }
    
    private FragmentType currentFragmentType = FragmentType.NOTES;
    private FragmentManager fragmentManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
            
            // 初始化必要的管理器
            localDataManager = LocalDataManager.getInstance(this);
            networkManager = NetworkManager.getInstance(this);
            fragmentManager = getSupportFragmentManager();
            
            // 初始化视图
            initViews();
            
            // 注册认证失败广播接收器
            registerAuthFailedReceiver();
            
            // 设置默认Fragment
            if (savedInstanceState == null) {
                switchFragment(FragmentType.NOTES);
                bottomNav.setSelectedItemId(R.id.action_notes);
                
                // 延迟初始化其他组件和同步数据，减轻启动负担
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 延迟初始化同步管理器
                        syncManager = SyncManager.getInstance(this);
                        
                        // 确保NoteRepository与当前用户匹配
                        try {
                            noteRepository = NoteRepository.getInstance(this);
                            noteRepository.updateCurrentUserId();
                            Log.d(TAG, "数据库用户ID已更新为: " + localDataManager.getCurrentUserId());
                        } catch (Exception e) {
                            Log.e(TAG, "初始化NoteRepository失败", e);
                        }
                        
                        // 应用启动后进一步延迟同步数据
                        if (networkManager.isNetworkConnected() && localDataManager.isSignedIn()) {
                            // 再次延迟2秒以确保界面完全加载
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                // 强制同步
                                syncManager.fetchNotesFromServer(new SyncManager.SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "应用启动时成功同步数据");
                                        runOnUiThread(MainActivity.this::refreshCurrentFragment);
                                    }
                                    
                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e(TAG, "应用启动时同步失败: " + errorMessage);
                                    }
                                });
                            }, 2000);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "延迟初始化组件失败", e);
                    }
                }, 1500);
            }
        } catch (Exception e) {
            Log.e(TAG, "MainActivity onCreate发生异常", e);
            Toast.makeText(this, "应用启动时发生错误，请重试", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 注册认证失败广播接收器
     */
    private void registerAuthFailedReceiver() {
        authFailedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "收到认证失败广播");
                
                // 获取错误信息
                String errorMessage = intent.getStringExtra("error_message");
                Log.d(TAG, "认证失败详情: " + errorMessage);
                
                // 检查是否是致命的认证错误还是可恢复性错误
                if (errorMessage != null && (
                    errorMessage.contains("继续尝试") || 
                    errorMessage.contains("可能有问题") ||
                    errorMessage.contains("但仍继续"))) {
                    
                    // 非致命错误，显示提示但不退出
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "认证可能有问题，但仍会尝试继续操作", 
                            Toast.LENGTH_LONG).show();
                        
                        // 尝试重新同步数据
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000); // 等待2秒
                                runOnUiThread(() -> syncDataWithServer());
                            } catch (InterruptedException e) {
                                Log.e(TAG, "重试同步时线程被中断", e);
                            }
                        }).start();
                    });
                } else {
                    // 致命的认证错误，需要重新登录
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "登录已过期，请重新登录", 
                            Toast.LENGTH_LONG).show();
                        
                        // 跳转到登录界面
                        Intent loginIntent = new Intent(MainActivity.this, 
                            com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
                        startActivity(loginIntent);
                        finish();
                    });
                }
            }
        };
        
        // 注册广播接收器，添加RECEIVER_NOT_EXPORTED标志
        IntentFilter filter = new IntentFilter("com.lianxiangdaimaowang.lumina.AUTH_FAILED");
        registerReceiver(authFailedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    @Override
    protected void onDestroy() {
        // 取消注册广播接收器
        if (authFailedReceiver != null) {
            try {
                unregisterReceiver(authFailedReceiver);
            } catch (Exception e) {
                Log.e(TAG, "取消注册广播接收器失败", e);
            }
        }
        
        super.onDestroy();
    }
    
    private void initViews() {
        // 设置底部导航
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_notes) {
                if (currentFragmentType != FragmentType.NOTES) {
                    currentFragmentType = FragmentType.NOTES;
                    switchFragment(FragmentType.NOTES);
                    fabAdd.setOnClickListener(v -> {
                        // 创建新笔记 - 使用startActivityForResult
                        Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                        startActivityForResult(intent, 1001); // 使用请求码1001表示新建笔记
                        Log.d(TAG, "启动笔记编辑活动，等待结果返回");
                    });
                    fabAdd.setVisibility(View.VISIBLE);
                }
                return true;
            } else if (id == R.id.action_daily_push) {
                if (currentFragmentType != FragmentType.DAILY_PUSH) {
                    currentFragmentType = FragmentType.DAILY_PUSH;
                    switchFragment(FragmentType.DAILY_PUSH);
                    // 在每日推送中，不需要悬浮按钮
                    fabAdd.setVisibility(View.GONE);
                }
                return true;
            } else if (id == R.id.action_community) {
                if (currentFragmentType != FragmentType.COMMUNITY) {
                    currentFragmentType = FragmentType.COMMUNITY;
                    switchFragment(FragmentType.COMMUNITY);
                    fabAdd.setVisibility(View.GONE);
                }
                return true;
            } else if (id == R.id.action_profile) {
                if (currentFragmentType != FragmentType.PROFILE) {
                    currentFragmentType = FragmentType.PROFILE;
                    switchFragment(FragmentType.PROFILE);
                    fabAdd.setVisibility(View.GONE);
                }
                return true;
            }
            return false;
        });
        
        // 初始化浮动按钮
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 默认创建新笔记 - 使用startActivityForResult
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                startActivityForResult(intent, 1001); // 使用请求码1001表示新建笔记
                Log.d(TAG, "从悬浮按钮启动笔记编辑活动，等待结果返回");
            }
        });
    }
    
    /**
     * 从服务器同步数据
     */
    private void syncDataWithServer() {
        if (isSyncing) {
            return; // 避免重复同步
        }
        
        if (!networkManager.isNetworkConnected()) {
            Log.d(TAG, "无网络连接，无法同步数据");
            Toast.makeText(this, "无网络连接，无法获取云端数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否有认证令牌
        String token = localDataManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            Log.d(TAG, "没有认证令牌，无法同步数据");
            
            // 清除登录状态并重新登录
            localDataManager.setLoggedIn(false);
            Intent intent = new Intent(this, com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        isSyncing = true;
        
        // 从服务器加载笔记
        syncManager.fetchNotesFromServer(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "数据同步成功");
                isSyncing = false;
                
                // 通知用户同步成功
                runOnUiThread(() -> {
                    // 刷新当前Fragment
                    refreshCurrentFragment();
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "数据同步失败: " + errorMessage);
                isSyncing = false;
                
                // 检查是否是需要重新登录的错误
                if (errorMessage.contains("令牌") || 
                    errorMessage.contains("token") ||
                    errorMessage.contains("认证") ||
                    errorMessage.contains("登录") ||
                    errorMessage.contains("需要登录") ||
                    errorMessage.contains("访问此资源需要登录")) {
                    
                    // 检查当前令牌是否存在
                    String currentToken = localDataManager.getAuthToken();
                    if (currentToken == null || currentToken.isEmpty()) {
                        Log.w(TAG, "当前令牌为空，需要重新登录");
                        // 清除登录状态
                        localDataManager.signOut();
                        localDataManager.clearAuthToken();
                    } else {
                        Log.w(TAG, "当前令牌不为空，但认证失败: " + 
                              (currentToken.length() > 20 ? currentToken.substring(0, 20) + "..." : currentToken));
                        
                        // 清除登录状态和令牌
                        localDataManager.signOut();
                        localDataManager.clearAuthToken();
                    }
                    
                    // 通知用户需要重新登录
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "登录已过期，请重新登录", Toast.LENGTH_LONG).show();
                        
                        // 跳转到登录界面
                        Intent intent = new Intent(MainActivity.this, com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    // 其他错误
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "获取云端数据失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    /**
     * 刷新当前Fragment
     */
    private void refreshCurrentFragment() {
        // 递归调用保护
        if (isRefreshingFragment) {
            Log.w(TAG, "已经在刷新Fragment中，忽略重复调用");
            return;
        }
        
        // 重试次数保护
        if (fragmentRefreshRetryCount >= MAX_FRAGMENT_RETRY) {
            Log.e(TAG, "刷新Fragment重试次数过多，放弃刷新: " + fragmentRefreshRetryCount);
            fragmentRefreshRetryCount = 0;
            return;
        }
        
        isRefreshingFragment = true;
        fragmentRefreshRetryCount++;
        
        Fragment currentFragment = null;
        
        try {
            switch (currentFragmentType) {
                case NOTES:
                    currentFragment = fragmentManager.findFragmentByTag(TAG_NOTES);
                    break;
                case DAILY_PUSH:
                    currentFragment = fragmentManager.findFragmentByTag(TAG_DAILY_PUSH);
                    break;
                case COMMUNITY:
                    currentFragment = fragmentManager.findFragmentByTag(TAG_COMMUNITY);
                    break;
                case PROFILE:
                    currentFragment = fragmentManager.findFragmentByTag(TAG_PROFILE);
                    break;
            }
            
            if (currentFragment != null && currentFragment.isAdded()) {
                // 如果当前Fragment是NotesFragment，调用其刷新方法
                if (currentFragment instanceof NotesFragment) {
                    try {
                        ((NotesFragment) currentFragment).refreshNotes();
                    } catch (Exception e) {
                        Log.e(TAG, "刷新Notes Fragment时出错", e);
                    }
                }
                // 对于DailyPushFragment和CommunityFragment，通过发送消息触发刷新
                // 避免直接调用其私有方法
                else if (currentFragment instanceof DailyPushFragment || currentFragment instanceof CommunityFragment) {
                    // 通过发送事件或启用SwipeRefreshLayout来刷新
                    try {
                        View view = currentFragment.getView();
                        if (view != null) {
                            SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
                            if (swipeRefreshLayout != null) {
                                // 通过触发下拉刷新来刷新数据
                                swipeRefreshLayout.setRefreshing(true);
                                // 使用final变量来避免Lambda表达式中的变量引用问题
                                final Fragment fragmentForCallback = currentFragment;
                                final SwipeRefreshLayout refreshLayoutForCallback = swipeRefreshLayout;
                                
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        if (refreshLayoutForCallback != null && fragmentForCallback.isAdded()) {
                                            refreshLayoutForCallback.setRefreshing(false);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "延迟结束刷新时出错", e);
                                    }
                                }, 1000);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "尝试通过SwipeRefreshLayout刷新Fragment时出错", e);
                    }
                }
                
                // 如果Fragment中有SwipeRefreshLayout，更新其状态
                try {
                    View view = currentFragment.getView();
                    if (view != null) {
                        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "更新SwipeRefreshLayout状态时出错", e);
                }
                
                // 成功刷新，重置重试计数器
                fragmentRefreshRetryCount = 0;
            } else {
                // 如果当前Fragment不存在，重新创建一个
                Log.d(TAG, "当前Fragment不存在或未添加，重新创建: " + currentFragmentType);
                
                // 在这里不要递归调用，通过标记确保switchFragment会执行但不会递归回refreshCurrentFragment
                if (!isSwitchingFragment) {
                    switchFragment(currentFragmentType);
                } else {
                    Log.w(TAG, "正在切换Fragment中，避免递归调用");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新当前Fragment时发生异常", e);
        } finally {
            // 确保状态标记被重置
            isRefreshingFragment = false;
        }
    }
    
    // 强制进行垃圾回收
    private void forceGC() {
        // 建议系统进行垃圾回收
        Runtime.getRuntime().gc();
        
        try {
            // 等待GC完成
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Log.e(TAG, "等待GC时被中断", e);
        }
    }
    
    // 确保布局容器完全清空
    private void clearFragmentContainer() {
        // 获取所有已添加的Fragment
        List<Fragment> fragments = fragmentManager.getFragments();
        if (!fragments.isEmpty()) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (Fragment fragment : fragments) {
                if (fragment != null) {
                    transaction.remove(fragment);
                    Log.d(TAG, "准备移除额外的Fragment: " + fragment.getClass().getSimpleName());
                }
            }
            transaction.commitNow();
            Log.d(TAG, "已清理容器中的所有Fragment");
        }
    }
    
    private void switchFragment(FragmentType type) {
        // 递归保护
        if (isSwitchingFragment) {
            Log.w(TAG, "已经在切换Fragment过程中，忽略重复调用");
            return;
        }
        
        isSwitchingFragment = true;
        
        // 记录即将切换的类型
        Log.d(TAG, "切换到Fragment类型: " + type.name() + ", 之前类型: " + currentFragmentType.name());
        
        try {
            // 如果类型相同，只是刷新
            if (type == currentFragmentType && !isRefreshingFragment) {
                // 在刷新过程中，不要递归调用refreshCurrentFragment
                // 利用单独的Handler延迟调用，避免递归
                new Handler(Looper.getMainLooper()).post(() -> {
                    refreshCurrentFragment();
                });
                return;
            }
            
            // 创建新事务
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            
            // 强行移除所有旧的Fragment
            try {
                Fragment oldNotesFragment = fragmentManager.findFragmentByTag(TAG_NOTES);
                Fragment oldDailyPushFragment = fragmentManager.findFragmentByTag(TAG_DAILY_PUSH);
                Fragment oldCommunityFragment = fragmentManager.findFragmentByTag(TAG_COMMUNITY);
                Fragment oldProfileFragment = fragmentManager.findFragmentByTag(TAG_PROFILE);
                
                if (oldNotesFragment != null) {
                    transaction.remove(oldNotesFragment);
                    Log.d(TAG, "已移除旧的Notes Fragment");
                }
                if (oldDailyPushFragment != null) {
                    transaction.remove(oldDailyPushFragment);
                    Log.d(TAG, "已移除旧的DailyPush Fragment");
                }
                if (oldCommunityFragment != null) {
                    transaction.remove(oldCommunityFragment);
                    Log.d(TAG, "已移除旧的Community Fragment");
                }
                if (oldProfileFragment != null) {
                    transaction.remove(oldProfileFragment);
                    Log.d(TAG, "已移除旧的Profile Fragment");
                }
                
                // 先提交一次移除操作
                transaction.commitNow();
            } catch (Exception e) {
                Log.e(TAG, "移除旧Fragment时出错", e);
            }
            
            // 强制垃圾回收，清理旧的Fragment资源
            forceGC();
            
            // 确保Fragment容器完全清空
            clearFragmentContainer();
            
            // 创建新的事务
            transaction = fragmentManager.beginTransaction();
            
            // 创建新Fragment
            Fragment newFragment = null;
            String newTag = "";
            
            switch (type) {
                case NOTES:
                    newFragment = new NotesFragment();
                    newTag = TAG_NOTES;
                    break;
                case DAILY_PUSH:
                    newFragment = new DailyPushFragment();
                    newTag = TAG_DAILY_PUSH;
                    break;
                case COMMUNITY:
                    newFragment = new CommunityFragment();
                    newTag = TAG_COMMUNITY;
                    break;
                case PROFILE:
                    newFragment = new ProfileFragment();
                    newTag = TAG_PROFILE;
                    break;
            }
            
            // 添加新Fragment
            if (newFragment != null) {
                transaction.add(R.id.fragment_container, newFragment, newTag);
                Log.d(TAG, "添加了新的" + type.name() + " Fragment");
                
                // 保存最终引用以用于回调
                final Fragment finalFragment = newFragment;
                
                try {
                    // 立即提交
                    transaction.commitNow();
                    
                    // 必须等待Fragment完全添加后再尝试访问其视图
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            if (finalFragment.isAdded() && finalFragment.getView() != null) {
                                // 手动触发Fragment加载/刷新数据，使用SwipeRefreshLayout模拟刷新
                                SwipeRefreshLayout swipeRefreshLayout = finalFragment.getView().findViewById(R.id.swipe_refresh);
                                if (swipeRefreshLayout != null) {
                                    // 对于所有类型的Fragment，使用相同的刷新方法
                                    swipeRefreshLayout.setRefreshing(true);
                                    
                                    // 使用final变量避免Lambda表达式中的变量引用问题
                                    final Fragment fragmentForCallback = finalFragment;
                                    final SwipeRefreshLayout refreshLayoutForCallback = swipeRefreshLayout;
                                    
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        try {
                                            if (refreshLayoutForCallback != null && fragmentForCallback.isAdded()) {
                                                refreshLayoutForCallback.setRefreshing(false);
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "关闭SwipeRefreshLayout刷新状态时出错", e);
                                        }
                                    }, 1000);
                                }
                                
                                // 特殊处理NotesFragment，因为它有公开的刷新方法
                                if (type == FragmentType.NOTES && finalFragment instanceof NotesFragment) {
                                    Log.d(TAG, "刷新Notes Fragment数据");
                                    try {
                                        ((NotesFragment) finalFragment).refreshNotes();
                                    } catch (Exception e) {
                                        Log.e(TAG, "刷新Notes数据时出错", e);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Fragment视图初始化后处理时出错", e);
                        }
                    }, 300); // 给Fragment一些时间完成视图初始化
                } catch (Exception e) {
                    Log.e(TAG, "提交Fragment事务或设置刷新时出错", e);
                }
            }
            
            // 更新当前Fragment类型
            currentFragmentType = type;
            
            // 重置重试计数器
            fragmentRefreshRetryCount = 0;
        } catch (Exception e) {
            Log.e(TAG, "切换Fragment时发生未处理异常", e);
            Toast.makeText(this, "加载页面时出错，请重试", Toast.LENGTH_SHORT).show();
        } finally {
            // 确保标记被重置
            isSwitchingFragment = false;
        }
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "收到活动结果: requestCode=" + requestCode + ", resultCode=" + resultCode + 
              ", data=" + (data != null ? "非空" : "空"));
        
        // 检查是否从笔记编辑活动返回
        if ((requestCode == 1001 || requestCode == 1002) && resultCode == RESULT_OK) {
            boolean shouldRefresh = data != null && data.getBooleanExtra("refresh_notes", false);
            Log.d(TAG, "从笔记编辑活动返回，需要刷新: " + shouldRefresh);
            
            // 无论如何都强制刷新笔记列表
            if (currentFragmentType == FragmentType.NOTES) {
                NotesFragment notesFragment = (NotesFragment) fragmentManager.findFragmentByTag(TAG_NOTES);
                if (notesFragment != null && notesFragment.isVisible()) {
                    try {
                        Log.d(TAG, "强制刷新笔记列表");
                        notesFragment.forceRefreshNotes();
                    } catch (Exception e) {
                        Log.e(TAG, "强制刷新笔记列表时出错", e);
                    }
                } else {
                    Log.d(TAG, "笔记Fragment不可见或为null，无法刷新");
                }
            } else {
                Log.d(TAG, "当前不在笔记页面，不刷新笔记列表");
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        try {
            // 检查用户是否已登录，如果没有登录则跳转到登录页面
            if (localDataManager != null && !localDataManager.isSignedIn()) {
                Intent intent = new Intent(this, com.lianxiangdaimaowang.lumina.login.LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            
            // 确保数据库使用正确的用户ID
            if (noteRepository != null) {
                noteRepository.updateCurrentUserId();
            }
            
            // 每次返回应用时刷新数据
            refreshCurrentFragment();
            
            // 每次返回应用时尝试同步数据
            if (networkManager != null && networkManager.isNetworkConnected() && !isSyncing) {
                syncDataWithServer();
            }
        } catch (Exception e) {
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        // 处理从通知或深层链接启动的情况
        boolean shouldSync = intent.getBooleanExtra("sync_data", false);
        if (shouldSync && !isSyncing && networkManager.isNetworkConnected()) {
            syncDataWithServer();
        }
    }
}