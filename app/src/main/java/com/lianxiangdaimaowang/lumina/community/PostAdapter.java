package com.lianxiangdaimaowang.lumina.community;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageButton;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 帖子列表适配器
 */
public class PostAdapter extends BaseAdapter {
    private static final String TAG = "PostAdapter";
    
    private final Context context;
    private final List<LocalPost> posts;
    private final LayoutInflater inflater;
    private final SimpleDateFormat dateFormat;
    private final LocalDataManager localDataManager;
    private final SyncManager syncManager;
    
    public PostAdapter(Context context, List<LocalPost> posts) {
        this.context = context;
        this.posts = posts;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        this.localDataManager = LocalDataManager.getInstance(context);
        this.syncManager = SyncManager.getInstance(context);
        
        Log.d(TAG, "PostAdapter创建，帖子数量: " + (posts != null ? posts.size() : 0));
    }
    
    @Override
    public int getCount() {
        return posts != null ? posts.size() : 0;
    }
    
    @Override
    public Object getItem(int position) {
        return posts != null && position < posts.size() ? posts.get(position) : null;
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_post, parent, false);
            holder = new ViewHolder();
            holder.tvUsername = convertView.findViewById(R.id.tv_username);
            holder.tvContent = convertView.findViewById(R.id.tv_content);
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            holder.tvLikes = convertView.findViewById(R.id.tv_likes);
            holder.tvComments = convertView.findViewById(R.id.tv_comments);
            holder.btnLike = convertView.findViewById(R.id.btn_like);
            holder.btnComment = convertView.findViewById(R.id.btn_comment);
            holder.btnFavorite = convertView.findViewById(R.id.btn_favorite);
            holder.tvFavorites = convertView.findViewById(R.id.tv_favorites);
            holder.ivAvatar = convertView.findViewById(R.id.iv_avatar);
            holder.btnDeletePost = convertView.findViewById(R.id.btn_delete_post);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        LocalPost post = (LocalPost) getItem(position);
        if (post == null) {
            Log.e(TAG, "getView: 位置 " + position + " 的帖子为null");
            return convertView;
        }
        
        // 记录日志
        Log.d(TAG, "渲染帖子: ID=" + post.getId() + ", 内容=" + post.getContent());
        
        // 设置帖子内容
        holder.tvUsername.setText(post.getUsername() != null ? post.getUsername() : "未知用户");
        
        // 设置标题和内容
        String title = post.getTitle();
        String content = post.getContent();
        
        if (title != null && !title.isEmpty()) {
            holder.tvContent.setText(title + "\n\n" + (content != null ? content : ""));
        } else {
            holder.tvContent.setText(content != null ? content : "");
        }
        
        // 设置时间，处理可能的空值
        if (post.getCreateTime() != null) {
            holder.tvTime.setText(dateFormat.format(post.getCreateTime()));
        } else {
            holder.tvTime.setText("未知时间");
        }
        
        // 设置点赞数
        int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
        holder.tvLikes.setText(String.valueOf(likeCount));
        
        // 设置评论数
        int commentCount = post.getComments() != null ? post.getComments().size() : 0;
        holder.tvComments.setText(String.valueOf(commentCount));
        
        // 设置收藏数
        int favoriteCount = post.getFavorites() != null ? post.getFavorites().size() : 0;
        holder.tvFavorites.setText(String.valueOf(favoriteCount));
        
        // 获取当前用户ID
        String currentUserId = localDataManager.getCurrentUserId();
        String effectiveUserId = localDataManager.getEffectiveUserId();
        Log.d(TAG, "当前用户ID: " + currentUserId + ", 有效用户ID: " + effectiveUserId);
        Log.d(TAG, "帖子信息 - ID: " + post.getId() + ", 用户ID: " + post.getUserId() + ", 用户名: " + post.getUsername());
        
        // 设置当前用户是否已点赞
        boolean isLiked = post.isLikedBy(currentUserId);
        holder.btnLike.setSelected(isLiked);
        
        // 设置当前用户是否已收藏
        boolean isFavorited = post.isFavoritedBy(currentUserId);
        holder.btnFavorite.setSelected(isFavorited);
        
        // 处理删除按钮的显示逻辑
        if (effectiveUserId != null && effectiveUserId.equals(post.getUserId())) {
            Log.d(TAG, "显示删除按钮 - 有效用户ID: " + effectiveUserId + ", 帖子用户ID: " + post.getUserId());
            holder.btnDeletePost.setVisibility(View.VISIBLE);
            holder.btnDeletePost.setOnClickListener(v -> {
                // 显示确认对话框
                new MaterialAlertDialogBuilder(context)
                    .setTitle("删除帖子")
                    .setMessage("确定要删除这条帖子吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 调用同步管理器删除帖子
                        syncManager.deletePost(post.getId(), new SyncManager.SyncCallback() {
                            @Override
                            public void onSuccess() {
                                // 从本地数据库删除
                                localDataManager.deleteLocalPost(post.getId());
                                // 从列表中移除
                                posts.remove(position);
                                // 刷新列表
                                notifyDataSetChanged();
                                
                                if (context instanceof Activity) {
                                    ((Activity) context).runOnUiThread(() -> {
                                        Toast.makeText(context, "帖子已删除", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                if (context instanceof Activity) {
                                    ((Activity) context).runOnUiThread(() -> {
                                        Toast.makeText(context, "删除失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
            });
        } else {
            Log.d(TAG, "隐藏删除按钮 - 当前用户ID: " + currentUserId + ", 帖子用户ID: " + post.getUserId());
            holder.btnDeletePost.setVisibility(View.GONE);
        }
        
        // 设置点赞按钮点击事件
        holder.btnLike.setOnClickListener(v -> {
            if (!localDataManager.isSignedIn()) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 获取当前点赞状态
            boolean currentLikeStatus = post.isLikedBy(currentUserId);
            
            // 根据当前状态进行相反操作
            if (currentLikeStatus) {
                // 当前已点赞，执行取消点赞
                post.removeLike(currentUserId);
                holder.btnLike.setSelected(false);
                holder.tvLikes.setText(String.valueOf(post.getLikes().size()));
                
                // 同步到服务器
                syncManager.likePost(post.getId(), false, new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        // 保存更新后的帖子
                        localDataManager.saveLocalPost(post);
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                // 移除点赞成功提示
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // 操作失败，恢复原状态
                        post.addLike(currentUserId);
                        
                        // 更新UI（需要在主线程执行）
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                holder.btnLike.setSelected(true);
                                holder.tvLikes.setText(String.valueOf(post.getLikes().size()));
                                Toast.makeText(context, "取消点赞失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            } else {
                // 当前未点赞，执行点赞
                post.addLike(currentUserId);
                holder.btnLike.setSelected(true);
                holder.tvLikes.setText(String.valueOf(post.getLikes().size()));
                
                // 同步到服务器
                syncManager.likePost(post.getId(), true, new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        // 保存更新后的帖子
                        localDataManager.saveLocalPost(post);
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "点赞成功", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // 操作失败，恢复原状态
                        post.removeLike(currentUserId);
                        
                        // 更新UI（需要在主线程执行）
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                holder.btnLike.setSelected(false);
                                holder.tvLikes.setText(String.valueOf(post.getLikes().size()));
                                Toast.makeText(context, "点赞失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
        });
        
        // 设置收藏按钮点击事件
        holder.btnFavorite.setOnClickListener(v -> {
            if (!localDataManager.isSignedIn()) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 获取当前收藏状态
            boolean currentFavoriteStatus = post.isFavoritedBy(currentUserId);
            
            // 根据当前状态进行相反操作
            if (currentFavoriteStatus) {
                // 当前已收藏，执行取消收藏
                post.removeFavorite(currentUserId);
                holder.btnFavorite.setSelected(false);
                holder.tvFavorites.setText(String.valueOf(post.getFavorites().size()));
                
                // 同步到服务器
                syncManager.favoritePost(post.getId(), false, new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        // 保存更新后的帖子
                        localDataManager.saveLocalPost(post);
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                // 移除收藏成功提示
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // 操作失败，恢复原状态
                        post.addFavorite(currentUserId);
                        
                        // 更新UI（需要在主线程执行）
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                holder.btnFavorite.setSelected(true);
                                holder.tvFavorites.setText(String.valueOf(post.getFavorites().size()));
                                Toast.makeText(context, "取消收藏失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            } else {
                // 当前未收藏，执行收藏
                post.addFavorite(currentUserId);
                holder.btnFavorite.setSelected(true);
                holder.tvFavorites.setText(String.valueOf(post.getFavorites().size()));
                
                // 同步到服务器
                syncManager.favoritePost(post.getId(), true, new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        // 保存更新后的帖子
                        localDataManager.saveLocalPost(post);
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                // 移除取消收藏成功提示
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // 操作失败，恢复原状态
                        post.removeFavorite(currentUserId);
                        
                        // 更新UI（需要在主线程执行）
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                holder.btnFavorite.setSelected(false);
                                holder.tvFavorites.setText(String.valueOf(post.getFavorites().size()));
                                Toast.makeText(context, "收藏失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
        });
        
        // 设置评论按钮点击事件
        holder.btnComment.setOnClickListener(v -> {
            // 跳转到评论页面或显示评论对话框
            // TODO: 实现评论功能
        });
        
        return convertView;
    }
    
    private static class ViewHolder {
        TextView tvUsername;
        TextView tvContent;
        TextView tvTime;
        TextView tvLikes;
        TextView tvComments;
        Button btnLike;
        Button btnComment;
        Button btnFavorite;
        TextView tvFavorites;
        ImageView ivAvatar;
        ImageButton btnDeletePost;
    }
} 