package com.lianxiangdaimaowang.lumina.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lianxiangdaimaowang.lumina.api.ApiService;
import com.lianxiangdaimaowang.lumina.api.ApiClient;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同步管理器
 * 用于协调笔记和帖子的同步操作
 */
public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 默认超时时间30秒
    
    private Context context;
    private NoteSynchronizer noteSynchronizer;
    private PostSynchronizer postSynchronizer;
    private Handler mainHandler;
    private AtomicInteger pendingOperations = new AtomicInteger(0);
    
    private static SyncManager instance;
    
    /**
     * 为了向后兼容定义内部SyncCallback接口
     */
    public interface SyncCallback {
        /**
         * 同步操作成功时调用
         */
        void onSuccess();
        
        /**
         * 同步操作失败时调用
         * @param errorMessage 错误信息
         */
        void onError(String errorMessage);
    }
    
    /**
     * 获取SyncManager单例
     */
    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 构造函数
     */
    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化同步器
        this.noteSynchronizer = new NoteSynchronizer(context);
        this.postSynchronizer = new PostSynchronizer(context);
        
        Log.d(TAG, "SyncManager初始化完成");
    }
    
    /**
     * 获取笔记同步器中的服务器笔记列表
     */
    public List<Note> getServerNotes() {
        return noteSynchronizer.getServerNotes();
    }
    
    /**
     * 保存笔记并同步到服务器
     */
    public void saveNote(Note note, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                noteSynchronizer.saveNote(note, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 删除笔记
     */
    public void deleteNote(String noteId, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                noteSynchronizer.deleteNote(noteId, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 从服务器获取所有笔记
     */
    public void fetchNotesFromServer(SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
                        @Override
            public void run() {
                noteSynchronizer.fetchNotesFromServer(wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 保存帖子并同步到服务器
     */
    public void savePost(LocalPost post, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.savePost(post, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 删除帖子
     */
    public void deletePost(String postId, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.deletePost(postId, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 从服务器获取所有帖子
     */
    public void fetchPostsFromServer(SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.getAllPostsFromServer(wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 从服务器获取所有帖子
     * 为保持向后兼容性添加的别名方法
     */
    public void getAllPostsFromServer(SyncCallback callback) {
        fetchPostsFromServer(callback);
    }
    
    /**
     * 创建新帖子
     */
    public void createPost(LocalPost post, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.createPost(post, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 同步所有待同步的数据到服务器
     * 优化嵌套回调，简化逻辑
     */
    public void syncAllPendingItems(SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                // 使用原子布尔值跟踪笔记同步状态
                final AtomicBoolean notesSyncCompleted = new AtomicBoolean(false);
        
        // 同步笔记
                noteSynchronizer.syncPendingNotes(new com.lianxiangdaimaowang.lumina.sync.SyncCallback() {
                @Override
                    public void onSuccess() {
                        notesSyncCompleted.set(true);
                        Log.d(TAG, "笔记同步成功，开始同步帖子");
                        syncPendingPosts(callback);
                }
                
                @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "同步待同步笔记失败: " + errorMessage);
                        // 尝试同步帖子，无论笔记同步是否成功
                        syncPendingPosts(callback);
                    }
                });
            }
        }, DEFAULT_TIMEOUT_MS * 2); // 双倍超时时间，因为需要两个操作
    }
    
    /**
     * 同步所有待同步的帖子
     * 抽取为单独方法减少嵌套
     */
    private void syncPendingPosts(SyncCallback callback) {
        postSynchronizer.syncPendingPosts(wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
    }
    
    /**
     * 向下兼容：同步本地数据库与服务器笔记
     */
    public void syncLocalDatabaseWithServer(SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
                @Override
            public void run() {
                noteSynchronizer.syncLocalDatabaseWithServer(wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 向下兼容：同步笔记与服务器
     */
    public void syncNotesWithServer(SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                noteSynchronizer.syncNotesWithServer(wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 向下兼容：同步所有笔记
     */
    public void syncAllNotesFromServer(SyncCallback callback) {
                syncNotesWithServer(callback);
    }
    
    /**
     * 点赞或取消点赞帖子
     */
    public void likePost(String postId, boolean isLike, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.syncLikeToServer(postId, isLike, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 收藏或取消收藏帖子
     */
    public void favoritePost(String postId, boolean isFavorite, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.syncFavoriteToServer(postId, isFavorite, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 获取热门帖子
     */
    public void getHotPosts(int limit, SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.getHotPosts(limit, wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 获取用户收藏的帖子
     */
    public void getUserFavoritePosts(SyncCallback callback) {
        trackOperationStart();
        executeWithTimeout(new Runnable() {
            @Override
            public void run() {
                postSynchronizer.getUserFavoritePosts(wrapCallbackWithTracking(SyncCallbackAdapter.toSyncCallback(callback)));
            }
        }, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 添加超时机制执行同步操作
     */
    private void executeWithTimeout(final Runnable task, final long timeoutMs) {
        final AtomicBoolean isCompleted = new AtomicBoolean(false);
        final Thread[] taskThread = new Thread[1];
        
        // 创建超时处理Runnable
        final Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCompleted.get() && taskThread[0] != null) {
                    try {
                        // 尝试中断线程
                        taskThread[0].interrupt();
                        Log.e(TAG, "同步操作超时，已中断");
                        } catch (Exception e) {
                        Log.e(TAG, "中断超时操作失败: " + e.getMessage());
                    } finally {
                        // 确保操作计数减少
                        trackOperationEnd();
                    }
                }
            }
        };
        
        // 设置超时
        mainHandler.postDelayed(timeoutRunnable, timeoutMs);
        
        // 创建并执行任务线程
        taskThread[0] = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                            } catch (Exception e) {
                    Log.e(TAG, "同步任务执行异常: " + e.getMessage(), e);
                    trackOperationEnd();
                } finally {
                    // 标记完成，取消超时
                    isCompleted.set(true);
                    mainHandler.removeCallbacks(timeoutRunnable);
                }
            }
        });
        
        taskThread[0].start();
    }
    
    /**
     * 包装回调以跟踪操作结束
     */
    private com.lianxiangdaimaowang.lumina.sync.SyncCallback wrapCallbackWithTracking(
            final com.lianxiangdaimaowang.lumina.sync.SyncCallback originalCallback) {
        
        return new com.lianxiangdaimaowang.lumina.sync.SyncCallback() {
            @Override
            public void onSuccess() {
                trackOperationEnd();
                if (originalCallback != null) {
                    originalCallback.onSuccess();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                trackOperationEnd();
                if (originalCallback != null) {
                    originalCallback.onError(errorMessage);
                }
            }
        };
    }
    
    /**
     * 跟踪操作开始
     */
    private void trackOperationStart() {
        pendingOperations.incrementAndGet();
        Log.d(TAG, "同步操作开始，当前进行中操作数: " + pendingOperations.get());
    }
    
    /**
     * 跟踪操作结束
     */
    private void trackOperationEnd() {
        int count = pendingOperations.decrementAndGet();
        Log.d(TAG, "同步操作结束，剩余操作数: " + count);
    }
    
    /**
     * 获取是否有正在进行的操作
     */
    public boolean hasPendingOperations() {
        return pendingOperations.get() > 0;
    }
    
    /**
     * 获取当前进行中的操作数
     */
    public int getPendingOperationCount() {
        return pendingOperations.get();
    }
} 