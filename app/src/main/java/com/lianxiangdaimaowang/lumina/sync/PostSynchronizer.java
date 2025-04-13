package com.lianxiangdaimaowang.lumina.sync;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import com.lianxiangdaimaowang.lumina.api.ApiClient;
import com.lianxiangdaimaowang.lumina.api.ApiService;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * 帖子同步器
 * 负责处理帖子的同步操作
 */
public class PostSynchronizer {
    private static final String TAG = "PostSynchronizer";
    
    private final Context context;
    private final LocalDataManager localDataManager;
    private final ApiService apiService;
    private final NetworkManager networkManager;
    
    // 待同步的帖子列表
    private final Map<String, LocalPost> pendingPosts = new HashMap<>();
    
    // 存储从服务器获取的帖子列表
    private List<LocalPost> serverPosts = new ArrayList<>();
    
    /**
     * 构造函数
     */
    public PostSynchronizer(Context context) {
        this.context = context.getApplicationContext();
        this.localDataManager = LocalDataManager.getInstance(context);
        this.apiService = ApiClient.getApiService(context);
        this.networkManager = NetworkManager.getInstance(context);
    }
    
    /**
     * 添加待同步的帖子
     */
    public void addPendingPost(LocalPost post) {
        if (post != null && post.getId() != null) {
            pendingPosts.put(post.getId(), post);
            Log.d(TAG, "添加待同步帖子: " + post.getContent());
        }
    }
    
    /**
     * 获取服务器帖子列表
     */
    public List<LocalPost> getServerPosts() {
        return new ArrayList<>(serverPosts);
    }
    
    /**
     * 同步所有待同步的帖子到服务器
     */
    public void syncPendingPosts(SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        List<LocalPost> posts = new ArrayList<>(pendingPosts.values());
        for (LocalPost post : posts) {
            syncPostToServer(post, null);
        }
        
        if (callback != null) {
            callback.onSuccess();
        }
        
        Log.d(TAG, "已尝试同步所有待同步帖子: " + posts.size() + " 个帖子");
    }
    
    /**
     * 保存帖子，如果有网络则同步到服务器，否则保存到本地
     */
    public void savePost(LocalPost post, SyncCallback callback) {
        // 先保存到本地
        localDataManager.saveLocalPost(post);
        
        // 检查网络连接
        if (networkManager.isNetworkConnected()) {
            // 有网络，同步到服务器
            syncPostToServer(post, callback);
        } else {
            // 无网络，添加到待同步列表
            addPendingPost(post);
            if (callback != null) {
                callback.onSuccess();
            }
            Log.d(TAG, "无网络连接，帖子保存到本地并加入待同步队列");
        }
    }
    
    /**
     * 同步帖子到服务器
     */
    private void syncPostToServer(LocalPost post, final SyncCallback callback) {
        Call<LocalPost> call;
        
        // 因为服务器可能不支持PUT和路径参数更新，所以统一使用POST创建新帖子
        // 如果有ID，在内容中包含ID，服务器端处理
        call = apiService.createPost(post);
        
        call.enqueue(new Callback<LocalPost>() {
            @Override
            public void onResponse(Call<LocalPost> call, Response<LocalPost> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 同步成功，从待同步列表中移除
                    LocalPost serverPost = response.body();
                    pendingPosts.remove(post.getId());
                    
                    // 更新本地帖子（可能服务器有额外修改）
                    localDataManager.saveLocalPost(serverPost);
                    
                    Log.d(TAG, "帖子同步到服务器成功");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    // 同步失败
                    String errorMessage = "同步失败: " + ApiClient.getErrorMessage(response);
                    Log.e(TAG, errorMessage);
                    
                    // 处理认证错误
                    if (handleSyncError(errorMessage, callback)) {
                        // 错误已处理
                    } else if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<LocalPost> call, Throwable t) {
                String errorMessage = "服务器连接失败: " + t.getMessage();
                Log.e(TAG, errorMessage, t);
                
                // 处理网络错误
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * 直接从服务器获取帖子列表
     * 确保社区内容是共享的，而不是只显示本地内容
     */
    public void getAllPostsFromServer(SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        Log.d(TAG, "开始从服务器获取所有帖子");
        
        // 使用List<LocalPost>接收响应
        Call<List<LocalPost>> call = apiService.getAllPosts();
        call.enqueue(new Callback<List<LocalPost>>() {
            @Override
            public void onResponse(Call<List<LocalPost>> call, Response<List<LocalPost>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<LocalPost> posts = response.body();
                        
                        // 清空已有的服务器帖子列表
                        serverPosts.clear();
                        
                        if (!posts.isEmpty()) {
                            Log.d(TAG, "从服务器获取到 " + posts.size() + " 条帖子");
                            
                            // 先清空本地帖子数据，确保只显示服务器上的共享帖子
                            localDataManager.clearLocalPosts();
                            
                            // 使用自定义的Gson来转换帖子对象，避免直接类型转换问题
                            Gson gson = new GsonBuilder()
                                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                                .registerTypeAdapter(Date.class, new ApiClient.ISO8601DateAdapter())
                                .registerTypeAdapter(LocalPost.class, new ApiClient.LocalPostDeserializer())
                                .setLenient()
                                .create();
                                
                            // 处理每个帖子对象
                            for (Object rawPost : posts) {
                                try {
                                    // 先转成JSON字符串，再解析为LocalPost对象
                                    String jsonString = gson.toJson(rawPost);
                                    LocalPost post = gson.fromJson(jsonString, LocalPost.class);
                                    
                                    if (post != null && post.getId() != null) {
                                        // 添加到服务器帖子列表
                                        serverPosts.add(post);
                                        
                                        // 保存到本地数据库
                                        localDataManager.saveLocalPost(post);
                                        
                                        Log.d(TAG, "处理服务器帖子: ID=" + post.getId() + ", 用户ID=" + post.getUserId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "解析帖子元素失败: " + e.getMessage(), e);
                                }
                            }
                            
                            Log.d(TAG, "成功从服务器获取 " + serverPosts.size() + " 条帖子");
                            
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            Log.e(TAG, "返回的帖子列表为空");
                            if (callback != null) {
                                callback.onError("返回的帖子列表为空");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理帖子数据时出错: " + e.getMessage(), e);
                        if (callback != null) {
                            callback.onError("处理帖子数据时出错: " + e.getMessage());
                        }
                    }
                } else {
                    String errorMessage = "获取帖子失败，HTTP状态码: " + response.code();
                    Log.e(TAG, errorMessage);
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<LocalPost>> call, Throwable t) {
                Log.e(TAG, "获取帖子请求失败: " + t.getMessage(), t);
                if (callback != null) {
                    callback.onError("获取帖子请求失败: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * 创建新帖子
     */
    public void createPost(LocalPost post, SyncCallback callback) {
        if (post == null) {
            if (callback != null) {
                callback.onError("帖子不能为空");
            }
            return;
        }
        
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        // 检查标题是否为空
        if (post.getTitle() == null || post.getTitle().isEmpty()) {
            if (callback != null) {
                callback.onError("帖子标题不能为空");
            }
            return;
        }
        
        try {
            // 获取认证token
            String token = localDataManager.getAuthToken();
            if (token == null || token.isEmpty()) {
                if (callback != null) {
                    callback.onError("未登录,请先登录");
                }
                return;
            }
            
            // 设置创建帖子的用户信息
            if (post.getUserId() == null || post.getUserId().isEmpty()) {
                // 获取有效的服务器用户ID
                String effectiveUserId = localDataManager.getEffectiveUserId();
                post.setUserId(effectiveUserId);
                Log.d(TAG, "使用有效用户ID创建帖子: " + effectiveUserId);
            }
            
            if (post.getUsername() == null || post.getUsername().isEmpty()) {
                post.setUsername(localDataManager.getCurrentUsername());
            }
            
            // 设置创建时间
            if (post.getCreateTime() == null) {
                post.setCreateTime(new Date());
            }
            
            // 创建一个服务器上传专用的帖子对象，避免评论数组等字段引起服务器解析错误
            LocalPost uploadPost = post.createForServerUpload();
            
            Log.d(TAG, "开始创建新帖子: " + uploadPost.getTitle() + " - " + uploadPost.getContent() + ", 用户ID: " + uploadPost.getUserId());
            
            // 设置请求超时时间 - 30秒
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    // 添加认证头
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .method(original.method(), original.body());
                    return chain.proceed(requestBuilder.build());
                })
                .build();
            
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiClient.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
            
            ApiService timeoutApiService = retrofit.create(ApiService.class);
            
            // 使用带超时保护的API服务
            timeoutApiService.createPost(uploadPost).enqueue(new Callback<LocalPost>() {
                @Override
                public void onResponse(Call<LocalPost> call, Response<LocalPost> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            LocalPost createdPost = response.body();
                            Log.d(TAG, "帖子创建成功，ID: " + createdPost.getId());
                            
                            // 保存到本地
                            localDataManager.saveLocalPost(createdPost);
                            
                            // 更新服务器帖子列表
                            serverPosts.add(createdPost);
                            
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            String errorMessage = "创建帖子失败: " + ApiClient.getErrorMessage(response);
                            Log.e(TAG, errorMessage);
                            
                            if (callback != null) {
                                callback.onError(errorMessage);
                            }
                        }
                    } catch (Exception e) {
                        String errorMessage = "处理帖子创建响应时发生异常: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                        
                        if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<LocalPost> call, Throwable t) {
                    String errorMessage = "创建帖子请求失败: " + t.getMessage();
                    Log.e(TAG, errorMessage, t);
                    
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            String errorMessage = "发起创建帖子请求时发生异常: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            
            if (callback != null) {
                callback.onError(errorMessage);
            }
        }
    }
    
    /**
     * 删除帖子，如果有网络则从服务器删除
     * 确保用户只能删除自己发布的帖子
     */
    public void deletePost(String postId, SyncCallback callback) {
        if (postId == null || postId.isEmpty()) {
            if (callback != null) {
                callback.onError("帖子ID不能为空");
            }
            return;
        }

        // 获取认证token
        String token = localDataManager.getAuthToken();
        if (token == null || token.isEmpty()) {
            if (callback != null) {
                callback.onError("未登录,请先登录");
            }
            return;
        }

        // 获取当前用户ID
        String currentUserId = localDataManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            if (callback != null) {
                callback.onError("无法获取当前用户信息");
            }
            return;
        }

        // 获取帖子信息
        LocalPost post = localDataManager.getLocalPostById(postId);
        if (post == null) {
            if (callback != null) {
                callback.onError("帖子不存在");
            }
            return;
        }

        // 检查是否是帖子作者
        String postAuthorId = post.getUserId();
        if (!currentUserId.equals(postAuthorId)) {
            // 检查服务器端用户ID映射
            String serverUserId = localDataManager.getServerUserId(currentUserId);
            if (serverUserId == null || !serverUserId.equals(postAuthorId)) {
                if (callback != null) {
                    callback.onError("您只能删除自己发布的帖子");
                }
                return;
            }
        }

        // 检查网络连接
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }

        try {
            // 设置请求超时时间 - 30秒
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    // 添加认证头
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .method(original.method(), original.body());
                    return chain.proceed(requestBuilder.build());
                })
                .build();

            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiClient.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            ApiService timeoutApiService = retrofit.create(ApiService.class);

            // 先从本地删除
            localDataManager.deleteLocalPost(postId);
            
            // 从待同步列表中移除
            pendingPosts.remove(postId);

            // 从服务器删除
            timeoutApiService.deletePost(postId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "从服务器删除帖子成功: " + postId);
                        
                        // 从服务器帖子列表中移除
                        serverPosts.removeIf(p -> p.getId().equals(postId));
                        
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        String errorMessage = "删除帖子失败: " + ApiClient.getErrorMessage(response);
                        Log.e(TAG, errorMessage);
                        
                        // 恢复本地帖子
                        localDataManager.saveLocalPost(post);
                        
                        // 处理认证错误
                        if (handleSyncError(errorMessage, callback)) {
                            // 错误已处理
                        } else if (callback != null) {
                            callback.onError(errorMessage);
                        }
                    }
                }
                
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    String errorMessage = "服务器连接失败: " + t.getMessage();
                    Log.e(TAG, errorMessage, t);
                    
                    // 恢复本地帖子
                    localDataManager.saveLocalPost(post);
                    
                    // 处理网络错误
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            String errorMessage = "删除帖子时发生异常: " + e.getMessage();
            Log.e(TAG, errorMessage, e);
            
            // 恢复本地帖子
            localDataManager.saveLocalPost(post);
            
            if (callback != null) {
                callback.onError(errorMessage);
            }
        }
    }
    
    /**
     * 同步点赞状态到服务器
     * 确保每个用户只能给每个帖子点一次赞
     */
    public void syncLikeToServer(String postId, boolean isLike, SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        // 获取当前用户ID
        String userId = localDataManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("用户未登录");
            }
            return;
        }
        
        // 获取帖子
        LocalPost post = localDataManager.getLocalPostById(postId);
        if (post == null) {
            if (callback != null) {
                callback.onError("帖子不存在");
            }
            return;
        }
        
        // 检查用户是否已经点赞
        boolean hasLiked = post.isLikedBy(userId);
        
        // 如果当前状态与目标状态相同，则不需要操作
        if (hasLiked == isLike) {
            Log.d(TAG, "用户已经" + (isLike ? "点赞" : "取消点赞") + "该帖子，无需重复操作");
            if (callback != null) {
                // 点赞状态已经是目标状态，返回成功但提示用户
                if (isLike) {
                    callback.onError("您已经点过赞了");
                } else {
                    callback.onSuccess();
                }
            }
            return;
        }
        
        // 创建请求参数，这里不需要传递userId，因为服务器端会使用JWT中的用户信息
        Map<String, String> params = new HashMap<>();
        
        // 根据操作类型选择API
        Call<Void> call;
        if (isLike) {
            call = apiService.likePost(postId, params);
        } else {
            call = apiService.unlikePost(postId, params);
        }
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 更新本地帖子状态
                    LocalPost post = localDataManager.getLocalPostById(postId);
                    if (post != null) {
                        if (isLike) {
                            post.addLike(userId);
                        } else {
                            post.removeLike(userId);
                        }
                        localDataManager.saveLocalPost(post);
                    }
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    
                    Log.d(TAG, "点赞状态同步成功: postId=" + postId + ", 操作=" + (isLike ? "点赞" : "取消点赞"));
                } else {
                    if (callback != null) {
                        callback.onError("服务器返回错误: " + response.code());
                    }
                    
                    Log.e(TAG, "点赞状态同步失败: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (callback != null) {
                    callback.onError("请求失败: " + t.getMessage());
                }
                
                Log.e(TAG, "点赞状态同步请求失败", t);
            }
        });
    }
    
    /**
     * 同步帖子收藏状态到服务器
     * 确保每个用户只能给每个帖子收藏一次
     */
    public void syncFavoriteToServer(String postId, boolean isFavorite, SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        // 获取当前用户ID（使用服务器分配的ID）
        String userId = localDataManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("用户未登录");
            }
            return;
        }
        
        // 获取帖子
        LocalPost post = localDataManager.getLocalPostById(postId);
        if (post == null) {
            if (callback != null) {
                callback.onError("帖子不存在");
            }
            return;
        }
        
        // 检查用户是否已经收藏
        boolean hasFavorited = post.isFavoritedBy(userId);
        
        // 如果当前状态与目标状态相同，则不需要操作
        if (hasFavorited == isFavorite) {
            Log.d(TAG, "用户已经" + (isFavorite ? "收藏" : "取消收藏") + "该帖子，无需重复操作");
            if (callback != null) {
                // 收藏状态已经是目标状态，返回成功但提示用户
                if (isFavorite) {
                    callback.onError("您已经收藏过了");
                } else {
                    callback.onSuccess();
                }
            }
            return;
        }
        
        // 先更新本地状态，确保UI能立即响应
        if (isFavorite) {
            post.addFavorite(userId);
        } else {
            post.removeFavorite(userId);
        }
        // 立即保存到本地数据库
        localDataManager.saveLocalPost(post);
        Log.d(TAG, "已更新本地收藏状态: postId=" + postId + ", 操作=" + (isFavorite ? "收藏" : "取消收藏"));
        
        // 创建请求参数，这里不需要传递userId，因为服务器端会使用JWT中的用户信息
        // 服务器会根据JWT令牌中的用户ID处理收藏操作，确保使用正确的服务器ID
        Map<String, String> params = new HashMap<>();
        
        // 根据操作类型选择API
        Call<Void> call;
        if (isFavorite) {
            call = apiService.favoritePost(postId, params);
        } else {
            call = apiService.unfavoritePost(postId, params);
        }
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 更新本地帖子状态
                    LocalPost post = localDataManager.getLocalPostById(postId);
                    if (post != null) {
                        if (isFavorite) {
                            post.addFavorite(userId);
                        } else {
                            post.removeFavorite(userId);
                        }
                        localDataManager.saveLocalPost(post);
                    }
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    
                    Log.d(TAG, "收藏状态同步成功: postId=" + postId + ", 操作=" + (isFavorite ? "收藏" : "取消收藏"));
                } else {
                    // 服务器返回错误，恢复本地状态
                    LocalPost post = localDataManager.getLocalPostById(postId);
                    if (post != null) {
                        if (isFavorite) {
                            post.removeFavorite(userId);
                        } else {
                            post.addFavorite(userId);
                        }
                        localDataManager.saveLocalPost(post);
                    }
                    
                    if (callback != null) {
                        callback.onError("服务器返回错误: " + response.code());
                    }
                    
                    Log.e(TAG, "收藏状态同步失败: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // 网络请求失败，恢复本地状态
                LocalPost post = localDataManager.getLocalPostById(postId);
                if (post != null) {
                    if (isFavorite) {
                        post.removeFavorite(userId);
                    } else {
                        post.addFavorite(userId);
                    }
                    localDataManager.saveLocalPost(post);
                }
                
                if (callback != null) {
                    callback.onError("请求失败: " + t.getMessage());
                }
                
                Log.e(TAG, "收藏状态同步请求失败", t);
            }
        });
    }
    
    /**
     * 获取热门帖子
     * 只显示点赞数最高的3个帖子，如果点赞数相同则使用日期排序（较新的优先）
     */
    public void getHotPosts(int limit, SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        // 确保返回的热门数量为3
        final int HOT_POST_COUNT = 3;
        final int actualLimit = Math.min(limit, HOT_POST_COUNT);
        
        Log.d(TAG, "开始获取热门帖子，请求数量: " + actualLimit);
        
        // 尝试直接从服务器获取热门帖子
        Call<JsonObject> call = apiService.getHotPosts(actualLimit);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonResponse = response.body();
                        List<LocalPost> hotPosts = new ArrayList<>();
                        
                        // 创建Gson对象，确保使用自定义的LocalPost反序列化器
                        Gson gson = new GsonBuilder()
                            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .registerTypeAdapter(Date.class, new ApiClient.ISO8601DateAdapter())
                            .registerTypeAdapter(LocalPost.class, new ApiClient.LocalPostDeserializer())
                            .setLenient()
                            .create();
                        
                        Log.d(TAG, "热门帖子JSON响应: " + jsonResponse.toString());
                        
                        // 尝试从不同可能的路径获取帖子数组
                        JsonArray postsArray = null;
                        
                        if (jsonResponse.has("data") && !jsonResponse.get("data").isJsonNull()) {
                            JsonElement dataElement = jsonResponse.get("data");
                            if (dataElement.isJsonArray()) {
                                postsArray = dataElement.getAsJsonArray();
                            } else if (dataElement.isJsonObject() && dataElement.getAsJsonObject().has("posts")) {
                                postsArray = dataElement.getAsJsonObject().getAsJsonArray("posts");
                            }
                        } else if (jsonResponse.has("posts") && !jsonResponse.get("posts").isJsonNull()) {
                            postsArray = jsonResponse.getAsJsonArray("posts");
                        } else if (jsonResponse.has("results") && !jsonResponse.get("results").isJsonNull()) {
                            postsArray = jsonResponse.getAsJsonArray("results");
                        }
                        
                        // 如果找到了帖子数组，解析每个帖子对象
                        if (postsArray != null && postsArray.size() > 0) {
                            for (JsonElement postElement : postsArray) {
                                try {
                                    LocalPost post = gson.fromJson(postElement, LocalPost.class);
                                    if (post != null) {
                                        post.setContext(context);
                                        hotPosts.add(post);
                                        Log.d(TAG, "成功解析热门帖子: ID=" + post.getId() + ", 点赞数=" + post.getLikeCount());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "解析单个热门帖子时出错: " + e.getMessage(), e);
                                }
                            }
                        } else {
                            // 没有找到帖子数组，记录错误
                            Log.e(TAG, "热门帖子响应中没有找到帖子数组");
                        }
                        
                        if (!hotPosts.isEmpty()) {
                            Log.d(TAG, "从服务器获取热门帖子成功，共 " + hotPosts.size() + " 条");
                            
                            // 保存热门帖子到本地数据库
                            for (LocalPost post : hotPosts) {
                                localDataManager.saveLocalPost(post);
                                Log.d(TAG, "保存热门帖子: ID=" + post.getId() + ", 点赞数=" + post.getLikeCount());
                            }
                            
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            Log.w(TAG, "服务器返回的热门帖子列表为空，回退到从所有帖子中筛选");
                            fallbackToAllPosts(actualLimit, callback);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析热门帖子响应时出错: " + e.getMessage(), e);
                        fallbackToAllPosts(actualLimit, callback);
                    }
                } else {
                    // 如果服务器接口调用失败，回退到从所有帖子中筛选热门帖子
                    Log.w(TAG, "热门帖子接口调用失败，状态码: " + response.code() + ", 尝试从所有帖子中筛选");
                    fallbackToAllPosts(actualLimit, callback);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "获取热门帖子失败: " + t.getMessage(), t);
                // 网络请求失败时，回退到从所有帖子中筛选热门帖子
                fallbackToAllPosts(actualLimit, callback);
            }
        });
    }
    
    /**
     * 从所有帖子中筛选热门帖子的回退方法
     */
    private void fallbackToAllPosts(int limit, SyncCallback callback) {
        // 首先尝试从服务器获取所有帖子
        getAllPostsFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                // 从本地数据库获取所有帖子
                List<LocalPost> allPosts = localDataManager.getAllLocalPosts();
                Log.d(TAG, "准备从 " + allPosts.size() + " 个帖子中筛选热门帖子");
                
                // 输出所有帖子的点赞数情况
                for (LocalPost post : allPosts) {
                    Log.d(TAG, "帖子点赞情况 - ID: " + post.getId() + ", 内容: " + post.getContent() + ", 点赞数: " + post.getLikeCount());
                }
                
                // 直接使用Collections.sort按照自定义比较器排序
                Collections.sort(allPosts, (p1, p2) -> {
                    // 首先按点赞数降序排序
                    int likeCompare = Integer.compare(p2.getLikeCount(), p1.getLikeCount());
                    if (likeCompare != 0) {
                        return likeCompare;
                    }
                    // 点赞数相同时，按创建时间降序排序（新的排前面）
                    if (p1.getCreateTime() == null) return 1;
                    if (p2.getCreateTime() == null) return -1;
                    return p2.getCreateTime().compareTo(p1.getCreateTime());
                });
                
                // 获取前N条记录作为热门帖子
                List<LocalPost> hotPosts = new ArrayList<>();
                int count = 0;
                for (LocalPost post : allPosts) {
                    hotPosts.add(post);
                    count++;
                    if (count >= limit) {
                        break;
                    }
                }
                
                Log.d(TAG, "获取到 " + hotPosts.size() + " 条热门帖子，点赞数从高到低排序，同点赞数时新帖子优先");
                
                if (!hotPosts.isEmpty()) {
                    // 再次保存一次热门帖子，确保本地存在
                    for (LocalPost post : hotPosts) {
                        localDataManager.saveLocalPost(post);
                        Log.d(TAG, "热门帖子: ID=" + post.getId() + ", 点赞数=" + post.getLikeCount() + ", 创建时间=" + post.getCreateTime());
                    }
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    if (callback != null) {
                        callback.onError("没有热门帖子");
                    }
                    Log.e(TAG, "没有找到热门帖子");
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                // 如果从服务器获取失败，尝试从本地获取
                List<LocalPost> allPosts = localDataManager.getAllLocalPosts();
                
                // 记录本地帖子情况
                Log.d(TAG, "服务器获取帖子失败，尝试从本地获取。本地共有 " + allPosts.size() + " 个帖子");
                
                // 使用相同的排序逻辑处理本地帖子
                Collections.sort(allPosts, (p1, p2) -> {
                    // 首先按点赞数降序排序
                    int likeCompare = Integer.compare(p2.getLikeCount(), p1.getLikeCount());
                    if (likeCompare != 0) {
                        return likeCompare;
                    }
                    // 点赞数相同时，按创建时间降序排序（新的排前面）
                    if (p1.getCreateTime() == null) return 1;
                    if (p2.getCreateTime() == null) return -1;
                    return p2.getCreateTime().compareTo(p1.getCreateTime());
                });
                
                // 获取前N条记录作为热门帖子
                List<LocalPost> hotPosts = new ArrayList<>();
                int count = 0;
                for (LocalPost post : allPosts) {
                    hotPosts.add(post);
                    count++;
                    if (count >= limit) {
                        break;
                    }
                }
                
                if (!hotPosts.isEmpty()) {
                    Log.d(TAG, "从本地获取到 " + hotPosts.size() + " 条热门帖子");
                    
                    // 保存热门帖子到本地
                    for (LocalPost post : hotPosts) {
                        localDataManager.saveLocalPost(post);
                        Log.d(TAG, "本地热门帖子: ID=" + post.getId() + ", 点赞数=" + post.getLikeCount() + ", 创建时间=" + post.getCreateTime());
                    }
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    if (callback != null) {
                        callback.onError("没有热门帖子: " + errorMessage);
                    }
                    Log.e(TAG, "无法获取热门帖子: " + errorMessage);
                }
            }
        });
    }
    
    /**
     * 获取当前用户收藏的帖子
     */
    public void getUserFavoritePosts(SyncCallback callback) {
        if (!networkManager.isNetworkConnected()) {
            if (callback != null) {
                callback.onError("无网络连接");
            }
            return;
        }
        
        // 获取当前用户信息
        String userId = localDataManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("用户未登录");
            }
            return;
        }
        
        // 尝试从服务器获取用户的收藏帖子
        Log.d(TAG, "开始从服务器获取用户收藏帖子，用户ID: " + userId);
        
        // 首先尝试使用JWT令牌中的用户身份从"me"接口获取收藏
        Call<JsonObject> call = apiService.getCurrentUserFavorites();
        
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleResponse(call, response);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else if (response.code() == 403) {
                    // 如果403错误，可能是权限问题，尝试从全部帖子中筛选当前用户收藏的帖子
                    Log.w(TAG, "获取用户收藏帖子失败: 权限不足 (403)，尝试从所有帖子中筛选");
                    
                    // 尝试从已同步的所有帖子中筛选
                    filterUserFavoritePosts(userId, callback);
                } else {
                    // 其他错误，尝试从本地筛选
                    Log.e(TAG, "获取用户收藏帖子失败: " + response.code());
                    
                    // 从已同步的所有帖子中筛选当前用户收藏的帖子
                    filterUserFavoritePosts(userId, callback);
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "获取用户收藏帖子网络请求失败: " + t.getMessage(), t);
                
                // 网络请求失败，尝试从本地筛选
                filterUserFavoritePosts(userId, callback);
            }
        });
    }

    /**
     * 从所有帖子中筛选用户收藏的帖子
     * 先尝试从服务器获取所有帖子，然后进行本地筛选
     */
    private void filterUserFavoritePosts(String userId, SyncCallback callback) {
        // 先尝试从服务器获取所有帖子
        getAllPostsFromServer(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "成功获取所有帖子，开始筛选用户收藏帖子");
                
                // 获取可能存在的服务器ID映射
                String serverUserId = localDataManager.getValue("server_user_id_" + userId, null);
                Log.d(TAG, "当前用户ID: " + userId + ", 服务器映射ID: " + serverUserId);
                
                final String finalServerUserId = serverUserId;
                
                // 从所有本地帖子中筛选用户收藏的帖子
                List<LocalPost> allPosts = localDataManager.getAllLocalPosts();
                List<LocalPost> favoritePosts = new ArrayList<>();
                
                // 尝试检查并建立服务器ID映射
                if (serverUserId == null) {
                    // 如果没有服务器ID映射，尝试分析帖子中的作者信息
                    for (LocalPost post : allPosts) {
                        if (post.getUser() != null && 
                            post.getUser().getUsername() != null && 
                            post.getUser().getUsername().equals(userId)) {
                            // 如果帖子作者用户名与当前客户端用户ID相同，可能是映射关系
                            String authorId = post.getUser().getId();
                            if (authorId != null && !authorId.isEmpty() && !authorId.equals(userId)) {
                                // 保存映射关系
                                localDataManager.saveValue("server_user_id_" + userId, authorId);
                                Log.d(TAG, "发现服务器ID映射关系 - 客户端ID: " + userId + " -> 服务器ID: " + authorId);
                                // 更新映射ID
                                serverUserId = authorId;
                                break;
                            }
                        }
                    }
                }
                
                // 完成筛选工作
                for (LocalPost post : allPosts) {
                    // 设置上下文，确保可以使用更多方式匹配
                    post.setContext(context);
                    
                    // 检查当前用户ID
                    boolean isFavorited = post.isFavoritedBy(userId);
                    
                    // 如果存在服务器ID映射，也尝试使用服务器ID检查
                    if (!isFavorited && serverUserId != null) {
                        isFavorited = post.isFavoritedBy(serverUserId);
                        if (isFavorited) {
                            Log.d(TAG, "通过服务器ID匹配到收藏 - 帖子ID: " + post.getId() + 
                                    ", 客户端ID: " + userId + ", 服务器ID: " + serverUserId);
                            
                            // 确保客户端ID也被添加到收藏列表中
                            post.addFavorite(userId);
                            localDataManager.saveLocalPost(post);
                        }
                    }
                    
                    // 如果已发现是收藏的帖子，添加到列表
                    if (isFavorited) {
                        Log.d(TAG, "找到用户收藏的帖子 - ID: " + post.getId() + ", 标题: " + post.getTitle());
                        favoritePosts.add(post);
                    }
                }
                
                Log.d(TAG, "用户收藏帖子总数: " + favoritePosts.size());
                
                if (callback != null) {
                    if (favoritePosts.isEmpty()) {
                        // 如果没有找到收藏帖子，返回一个轻微的错误提示
                        callback.onError("没有找到收藏的帖子");
                    } else {
                        callback.onSuccess();
                    }
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "获取所有帖子失败，无法筛选收藏: " + errorMessage);
                
                // 即使获取所有帖子失败，仍然尝试从本地数据中筛选收藏
                List<LocalPost> favoritePosts = localDataManager.getUserFavoritePosts();
                Log.d(TAG, "从本地数据中找到 " + favoritePosts.size() + " 条收藏帖子");
                
                if (callback != null) {
                    if (favoritePosts.isEmpty()) {
                        callback.onError("没有找到收藏的帖子");
                    } else {
                        callback.onSuccess();
                    }
                }
            }
        });
    }

    /**
     * 处理服务器响应
     * @param call 调用对象
     * @param response 响应对象
     */
    private void handleResponse(Call<JsonObject> call, Response<JsonObject> response) {
        Log.d(TAG, "handleResponse: " + response.isSuccessful());
        if (response.isSuccessful()) {
            JsonObject jsonResponse = response.body();
            if (jsonResponse != null) {
                try {
                    // 从JsonObject获取data字段
                    if (jsonResponse.has("data")) {
                        JsonElement dataElement = jsonResponse.get("data");
                        if (dataElement != null && !dataElement.isJsonNull()) {
                            // 使用ApiClient中的自定义Gson进行反序列化
                            Gson gson = new GsonBuilder()
                                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                                .registerTypeAdapter(Date.class, new ApiClient.ISO8601DateAdapter())
                                .registerTypeAdapter(LocalPost.class, new ApiClient.LocalPostDeserializer())
                                .setLenient()
                                .create();
                            
                            // 如果data是一个JSON对象，尝试反序列化为LocalPost
                            if (dataElement.isJsonObject()) {
                                LocalPost post = gson.fromJson(dataElement, LocalPost.class);
                                if (post != null) {
                                    savePost(post, null);
                                    updateSyncStatus(post.getId(), true);
                                    Log.d(TAG, "已同步并保存帖子: " + post.getId());
                                } else {
                                    Log.e(TAG, "无法反序列化帖子，结果为null");
                                }
                            } 
                            // 如果data是一个JSON数组，尝试反序列化为LocalPost列表
                            else if (dataElement.isJsonArray()) {
                                JsonArray postsArray = dataElement.getAsJsonArray();
                                for (JsonElement postElement : postsArray) {
                                    LocalPost post = gson.fromJson(postElement, LocalPost.class);
                                    if (post != null) {
                                        savePost(post, null);
                                        updateSyncStatus(post.getId(), true);
                                        Log.d(TAG, "已从数组同步并保存帖子: " + post.getId());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "处理响应时出错: " + e.getMessage(), e);
                    if (e.getMessage() != null && e.getMessage().contains("Expected BEGIN_OBJECT")) {
                        Log.e(TAG, "JSON解析错误: 期望对象开始但获取到其他内容", e);
                    }
                }
            }
        } else {
            // 错误处理
            try {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "未知错误";
                Log.e(TAG, "服务器响应错误: " + response.code() + ", " + errorBody);
            } catch (IOException e) {
                Log.e(TAG, "读取错误响应时发生异常", e);
            }
        }
    }

    /**
     * 更新帖子的同步状态
     * @param postId 帖子ID
     * @param synced 是否已同步
     */
    private void updateSyncStatus(String postId, boolean synced) {
        if (postId == null || postId.isEmpty()) {
            return;
        }
        
        // 从本地获取帖子
        LocalPost post = localDataManager.getLocalPostById(postId);
        if (post != null) {
            // 更新帖子状态，例如设置为已同步状态
            // 这里可以根据实际需求设置更多的状态信息
            post.setStatus(synced ? 1 : 0);
            
            // 保存回本地
            localDataManager.saveLocalPost(post);
            
            Log.d(TAG, "更新帖子同步状态: " + postId + ", 状态: " + (synced ? "已同步" : "未同步"));
        } else {
            Log.w(TAG, "无法更新同步状态，找不到帖子: " + postId);
        }
    }

    /**
     * 处理同步错误
     * @param errorMessage 错误消息
     * @param callback 回调接口
     * @return 是否已处理错误
     */
    private boolean handleSyncError(String errorMessage, SyncCallback callback) {
        if (errorMessage.contains("需要登录") || 
            errorMessage.contains("访问此资源需要登录") ||
            errorMessage.contains("未授权") || 
            errorMessage.contains("无效的令牌") ||
            errorMessage.contains("token") ||
            errorMessage.contains("unauthorized") ||
            errorMessage.contains("forbidden") ||
            errorMessage.contains("用户未找到") ||
            errorMessage.contains("用户ID不匹配") ||
            errorMessage.contains("401") ||
            errorMessage.contains("HTTP错误: 401")) {
            
            Log.e(TAG, "操作失败: " + errorMessage);
            Log.w(TAG, "检测到认证错误: " + errorMessage);
            
            // 获取当前令牌进行检查
            String token = localDataManager.getAuthToken();
            if (token != null && !token.isEmpty()) {
                Log.d(TAG, "当前仍有令牌存在: " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
                // 检查用户登录状态
                if (localDataManager.isSignedIn()) {
                    Log.d(TAG, "用户登录状态为已登录，但令牌可能已过期或用户ID不匹配");
                    
                    // 检查用户ID映射关系
                    String clientUserId = localDataManager.getCurrentUserId();
                    String serverUserId = localDataManager.getServerUserId(clientUserId);
                    
                    if (serverUserId == null || serverUserId.isEmpty()) {
                        // 尝试从JWT令牌中提取用户信息
                        try {
                            String[] parts = token.split("\\.");
                            if (parts.length >= 2) {
                                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT));
                                Log.d(TAG, "JWT令牌负载: " + payload);
                                
                                // 检查服务器日志，识别可能的ID映射关系
                                // 在社区帖子中可能有该用户的帖子，可以检查他们的用户ID
                                if (errorMessage.contains("用户ID不匹配") || errorMessage.contains("用户未找到")) {
                                    Log.w(TAG, "可能存在用户ID映射问题，请检查服务器上该用户名对应的真实ID");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析JWT令牌出错: " + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "已有用户ID映射: 客户端ID=" + clientUserId + ", 服务器ID=" + serverUserId);
                    }
                } else {
                    Log.d(TAG, "用户登录状态为未登录，但令牌仍存在");
                }
            } else {
                Log.w(TAG, "当前令牌为空");
            }
            
            // 不要自动清除token，即使有认证错误也保持登录状态
            Log.w(TAG, "检测到同步错误，但保持用户登录状态");
            
            // 通知回调同步失败，但不要触发登出流程
            if (callback != null) {
                callback.onError("操作失败: " + errorMessage);
            }
            
            return true; // 已处理错误
        }
        
        return false; // 未处理错误
    }
} 