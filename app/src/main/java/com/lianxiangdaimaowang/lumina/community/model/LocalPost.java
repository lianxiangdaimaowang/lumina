package com.lianxiangdaimaowang.lumina.community.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 本地帖子模型类
 * 用于与服务端Post模型区分，避免冲突
 */
public class LocalPost {
    @SerializedName(value = "id", alternate = {"postId"})
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName(value = "content", alternate = {"postContent"})
    private String content;
    
    @SerializedName(value = "title", alternate = {"postTitle"})
    private String title;
    
    @SerializedName(value = "createdAt", alternate = {"createTime"})
    @JsonAdapter(ISO8601DateAdapter.class)
    private Date createTime;
    
    @SerializedName(value = "updatedAt")
    @JsonAdapter(ISO8601DateAdapter.class)
    private Date updatedAt;
    
    @SerializedName(value = "likes", alternate = {"likesArray"})
    private List<Object> likes;
    
    @SerializedName(value = "favorites", alternate = {"favoritesArray"})
    private List<String> favorites;
    
    @SerializedName(value = "comments", alternate = {"commentsArray"})
    private List<Comment> comments;
    
    @SerializedName("viewCount")
    private int viewCount;
    
    @SerializedName("likeCount")
    private int likeCount;
    
    @SerializedName("favoriteCount")
    private int favoriteCount;
    
    @SerializedName("commentCount")
    private int commentCount;
    
    @SerializedName("status")
    private int status;
    
    // 用于处理服务器返回的用户对象
    @SerializedName("user")
    private UserInfo user;
    
    // 上下文对象，用于获取当前用户信息
    private transient Context context;
    
    /**
     * 评论类
     */
    public static class Comment {
        @SerializedName("id")
        private String id;
        
        @SerializedName("content")
        private String content;
        
        @SerializedName("userId")
        private String userId;
        
        @SerializedName("username")
        private String username;
        
        @SerializedName("createdAt")
        @JsonAdapter(ISO8601DateAdapter.class)
        private Date createdAt;
        
        public Comment() {
        }
        
        public Comment(String id, String content, String userId, String username, Date createdAt) {
            this.id = id;
            this.content = content;
            this.userId = userId;
            this.username = username;
            this.createdAt = createdAt;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public Date getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    /**
     * 处理ISO8601日期格式的适配器
     */
    public static class ISO8601DateAdapter extends TypeAdapter<Date> {
        private final SimpleDateFormat[] dateFormats = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd", Locale.US)
        };
        
        public ISO8601DateAdapter() {
            // 设置UTC时区
            for (SimpleDateFormat format : dateFormats) {
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
        }
        
        @Override
        public void write(JsonWriter out, Date date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                out.value(dateFormats[0].format(date));
            }
        }
        
        @Override
        public Date read(JsonReader in) throws IOException {
            String dateStr = in.nextString();
            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }
            
            // 尝试使用不同的格式解析日期
            for (SimpleDateFormat format : dateFormats) {
                try {
                    return format.parse(dateStr);
                } catch (ParseException e) {
                    // 继续尝试下一个格式
                }
            }
            
            // 如果所有格式都失败，返回当前时间
            return new Date();
        }
    }
    
    // 嵌套类处理服务器返回的用户信息
    public static class UserInfo {
        @SerializedName("id")
        private String id;
        
        @SerializedName("username")
        private String username;
        
        @SerializedName("email")
        private String email;
        
        @SerializedName("avatar")
        private String avatar;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getAvatar() {
            return avatar;
        }
        
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }

    public LocalPost() {
        likes = new ArrayList<>();
        comments = new ArrayList<>();
    }
    
    /**
     * 设置上下文，用于获取当前用户信息
     */
    public void setContext(Context context) {
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        // 优先使用直接设置的userId，如果为空则尝试从user对象获取
        if (userId != null && !userId.isEmpty()) {
            return userId;
        } else if (user != null && user.getId() != null) {
            return user.getId();
        }
        return null;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        // 优先使用直接设置的username，如果为空则尝试从user对象获取
        if (username != null && !username.isEmpty()) {
            return username;
        } else if (user != null && user.getUsername() != null) {
            return user.getUsername();
        }
        return "";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public List<Object> getLikes() {
        return likes;
    }

    public void setLikes(List<Object> likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments != null ? comments : new ArrayList<>();
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public void addLike(String userId) {
        if (likes == null) {
            likes = new ArrayList<>();
        }
        
        // 检查是否已存在该用户ID的点赞
        boolean alreadyLiked = false;
        for (Object like : likes) {
            if (like instanceof String && like.equals(userId)) {
                alreadyLiked = true;
                break;
            } else if (like instanceof UserInfo && ((UserInfo)like).getId().equals(userId)) {
                alreadyLiked = true;
                break;
            }
        }
        
        if (!alreadyLiked) {
            likes.add(userId);
            likeCount = likes.size();
        }
    }

    public void removeLike(String userId) {
        if (likes != null) {
            likes.removeIf(like -> {
                if (like instanceof String) {
                    return like.equals(userId);
                } else if (like instanceof UserInfo) {
                    return ((UserInfo)like).getId().equals(userId);
                }
                return false;
            });
            likeCount = likes.size();
        }
    }

    public void addComment(String commentContent) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        
        // 获取当前用户信息
        String currentUserId = null;
        String currentUsername = "用户";
        
        if (context != null) {
            LocalDataManager dataManager = LocalDataManager.getInstance(context);
            currentUserId = dataManager.getCurrentUserId();
            currentUsername = dataManager.getCurrentUsername();
        }
        
        // 创建新评论
        Comment comment = new Comment();
        comment.setContent(commentContent);
        comment.setUserId(currentUserId);
        comment.setUsername(currentUsername);
        comment.setCreatedAt(new Date());
        
        comments.add(comment);
        commentCount = comments.size();
    }
    
    // 添加一个已有的评论对象
    public void addComment(Comment comment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(comment);
        commentCount = comments.size();
    }

    public boolean isLikedBy(String userId) {
        if (likes == null) return false;
        
        for (Object like : likes) {
            if (like instanceof String && like.equals(userId)) {
                return true;
            } else if (like instanceof UserInfo && ((UserInfo)like).getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getFavorites() {
        return favorites != null ? favorites : new ArrayList<>();
    }
    
    public void setFavorites(List<String> favorites) {
        this.favorites = favorites;
    }
    
    public int getFavoriteCount() {
        return favoriteCount;
    }
    
    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public boolean isFavoritedBy(String userId) {
        if (favorites == null || userId == null) return false;
        
        // 记录执行信息，帮助调试
        Log.d("LocalPost", "检查收藏状态 - 帖子ID: " + id + ", 传入用户ID: " + userId + ", 收藏列表: " + favorites);
        
        // 直接比较
        if (favorites.contains(userId)) {
            Log.d("LocalPost", "直接匹配成功 - 用户ID: " + userId);
            return true;
        }
        
        // 尝试不同类型比较 (数字类型的ID可能没有引号)
        try {
            // 如果传入的是数字字符串，也尝试匹配数字格式
            long numericUserId = Long.parseLong(userId);
            for (String favoriteId : favorites) {
                try {
                    if (Long.parseLong(favoriteId) == numericUserId) {
                        Log.d("LocalPost", "数字匹配成功 - 用户ID: " + userId + ", 收藏ID: " + favoriteId);
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // 忽略非数字的favoriteId
                }
            }
        } catch (NumberFormatException e) {
            // 如果传入的不是数字，尝试比较字符串值
            for (String favoriteId : favorites) {
                if (userId.equals(favoriteId)) {
                    Log.d("LocalPost", "字符串匹配成功 - 用户ID: " + userId + ", 收藏ID: " + favoriteId);
                    return true;
                }
            }
        }
        
        // 处理用户名是ID的特殊情况 (服务器设置的用户名可能是客户端ID)
        if (context != null) {
            LocalDataManager dataManager = LocalDataManager.getInstance(context);
            String serverUserId = dataManager.getValue("server_user_id_" + userId, null);
            if (serverUserId != null && favorites.contains(serverUserId)) {
                Log.d("LocalPost", "通过缓存服务器ID匹配成功 - 用户ID: " + userId + ", 服务器ID: " + serverUserId);
                return true;
            }
            
            // 检查用户名是否匹配
            String username = dataManager.getCurrentUsername();
            for (String favoriteId : favorites) {
                // 如果找到与当前用户名匹配的ID，可能表示这是当前用户的收藏
                if (username != null && username.equals(favoriteId)) {
                    // 保存服务器用户ID映射关系
                    dataManager.saveValue("server_user_id_" + userId, favoriteId);
                    Log.d("LocalPost", "用户名匹配成功 - 用户名: " + username + ", 收藏ID: " + favoriteId);
                    return true;
                }
            }
        }
        
        // 最后尝试统一字符串比较
        String userIdStr = String.valueOf(userId);
        for (String favoriteId : favorites) {
            if (String.valueOf(favoriteId).equals(userIdStr)) {
                Log.d("LocalPost", "统一字符串匹配成功 - 用户ID: " + userId + ", 收藏ID: " + favoriteId);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 添加用户收藏
     * @param userId 用户服务器ID
     */
    public void addFavorite(String userId) {
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        if (!favorites.contains(userId)) {
            favorites.add(userId);
            favoriteCount = favorites.size();
        }
    }
    
    /**
     * 移除用户收藏
     * @param userId 用户服务器ID
     */
    public void removeFavorite(String userId) {
        if (favorites != null) {
            favorites.remove(userId);
            favoriteCount = favorites.size();
        }
    }

    /**
     * 仅用于创建新帖子，发送到服务器时使用
     * 确保发送到服务器的新帖子不包含评论数组
     */
    public LocalPost createForServerUpload() {
        try {
            LocalPost uploadPost = new LocalPost();
            uploadPost.setId(this.id);
            uploadPost.setUserId(this.userId);
            uploadPost.setUsername(this.username);
            uploadPost.setTitle(this.title != null ? this.title : "无标题");
            uploadPost.setContent(this.content);
            
            // 安全地处理日期
            if (this.createTime != null) {
                uploadPost.setCreateTime(this.createTime);
            } else {
                uploadPost.setCreateTime(new Date()); // 使用当前时间
            }
            
            if (this.updatedAt != null) {
                uploadPost.setUpdatedAt(this.updatedAt);
            }
            
            // 设置基本属性，确保不为空
            uploadPost.setLikeCount(this.likeCount);
            uploadPost.setCommentCount(this.commentCount);
            uploadPost.setFavoriteCount(this.favoriteCount);
            uploadPost.setStatus(this.status);
            uploadPost.setViewCount(this.viewCount);
            
            // 不复制复杂对象，避免序列化问题
            uploadPost.comments = null;
            uploadPost.likes = null;
            uploadPost.favorites = null;
            
            return uploadPost;
        } catch (Exception e) {
            Log.e("LocalPost", "创建上传帖子对象失败: " + e.getMessage());
            // 创建一个最小可用的帖子对象
            LocalPost fallbackPost = new LocalPost();
            fallbackPost.setId(this.id);
            fallbackPost.setTitle(this.title != null ? this.title : "无标题");
            fallbackPost.setContent(this.content != null ? this.content : "");
            fallbackPost.setCreateTime(new Date());
            return fallbackPost;
        }
    }
} 