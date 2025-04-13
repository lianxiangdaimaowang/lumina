package com.lianxiangdaimaowang.lumina.api;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.model.User;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import okhttp3.logging.HttpLoggingInterceptor;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * API客户端类，用于创建Retrofit实例和API服务
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    // 基础URL，生产环境中应该从配置文件或者BuildConfig中获取
    private static final String BASE_URL = "http://121.43.138.32:8081/";
    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;
    private static ApiService apiService = null;

    /**
     * 获取API基础URL
     * @return API基础URL
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * 创建OkHttpClient实例
     */
    private static OkHttpClient getOkHttpClient(final Context context) {
        if (okHttpClient == null && context != null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            
            // 添加日志拦截器，记录请求和响应的内容
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                if (message.startsWith("{") || message.startsWith("[")) {
                    Log.d(TAG, "API数据: " + message);
                } else {
                    Log.d(TAG, "API日志: " + message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(loggingInterceptor);
            
            // 添加认证拦截器，自动添加令牌到请求头
            builder.addInterceptor(chain -> {
                Request original = chain.request();
                
                // 如果是注册请求，不添加认证头
                if (original.url().toString().contains("/auth/register")) {
                    return chain.proceed(original);
                }
                
                // 获取认证令牌
                String token = LocalDataManager.getInstance(context).getAuthToken();
                if (token == null || token.isEmpty()) {
                    Log.d(TAG, "没有令牌，使用未认证请求");
                    return chain.proceed(original);
                }
                
                // 添加认证头
                Log.d(TAG, "使用令牌: " + token);
                if (!token.startsWith("Bearer ")) {
                    Log.d(TAG, "添加Bearer前缀: Bearer " + token);
                    token = "Bearer " + token;
                }
                
                Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", token)
                    .method(original.method(), original.body());
                
                return chain.proceed(requestBuilder.build());
            });
            
            // 添加连接超时和读写超时
            builder.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    /**
     * 获取Retrofit实例
     * @return Retrofit实例
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * 获取API服务实例
     * @return ApiService实例
     */
    public static ApiService getApiService() {
        if (retrofit == null) {
            throw new IllegalStateException("必须先调用带Context参数的getApiService方法初始化");
        }
        return retrofit.create(ApiService.class);
    }
    
    /**
     * 获取API服务实例（带Context参数版本）
     * @param context 上下文
     * @return ApiService实例
     */
    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            // 创建OkHttpClient
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            
            // 设置超时
            httpClient.connectTimeout(30, TimeUnit.SECONDS);
            httpClient.readTimeout(60, TimeUnit.SECONDS);
            httpClient.writeTimeout(60, TimeUnit.SECONDS);
            
            // 添加认证拦截器，自动添加令牌到请求头
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                
                // 如果是注册请求，不添加认证头
                if (original.url().toString().contains("/auth/register")) {
                    return chain.proceed(original);
                }
                
                // 获取认证令牌
                String token = LocalDataManager.getInstance(context).getAuthToken();
                if (token == null || token.isEmpty()) {
                    Log.d(TAG, "没有令牌，使用未认证请求");
                    return chain.proceed(original);
                }
                
                // 添加认证头
                Log.d(TAG, "使用令牌: " + token);
                if (!token.startsWith("Bearer ")) {
                    Log.d(TAG, "添加Bearer前缀: Bearer " + token);
                    token = "Bearer " + token;
                }
                
                Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", token)
                    .method(original.method(), original.body());
                
                return chain.proceed(requestBuilder.build());
            });
            
            // 添加日志拦截器
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
                if (message.startsWith("{") || message.startsWith("[")) {
                    Log.d(TAG, "API数据: " + message);
                } else {
                    Log.d(TAG, "API日志: " + message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(loggingInterceptor);
            
            // 创建一个支持ISO8601日期格式的Gson解析器，并设置通用适配器
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .registerTypeAdapter(Date.class, new ISO8601DateAdapter())
                    .registerTypeAdapter(LocalPost.class, new LocalPostDeserializer())
                    .registerTypeAdapter(User.class, new JsonSerializer<User>() {
                        @Override
                        public JsonElement serialize(User user, Type typeOfSrc, JsonSerializationContext context) {
                            JsonObject jsonObject = new JsonObject();
                            
                            // 调试日志
                            Log.d(TAG, "序列化User对象开始:");
                            Log.d(TAG, "用户名: [" + user.getUsername() + "]");
                            Log.d(TAG, "密码: [" + user.getPassword() + "]");
                            Log.d(TAG, "邮箱: [" + user.getEmail() + "]");
                            Log.d(TAG, "状态: [" + user.getStatus() + "]");
                            
                            // 必需字段：直接添加，不进行null检查
                            jsonObject.addProperty("username", user.getUsername());
                            // 确保密码字段正确添加
                            String password = user.getPassword();
                            Log.d(TAG, "添加密码字段，原始值: [" + password + "]");
                            jsonObject.addProperty("password", password != null ? password.trim() : "");
                            jsonObject.addProperty("email", user.getEmail());
                            
                            // 添加其他字段
                            jsonObject.addProperty("status", user.getStatus());
                            
                            // 可选字段：只在非null时添加
                            if (user.getId() != null) {
                                jsonObject.addProperty("id", user.getId());
                            }
                            if (user.getAvatar() != null) {
                                jsonObject.addProperty("avatar", user.getAvatar());
                            }
                            if (user.getStudentType() != null) {
                                jsonObject.addProperty("studentType", user.getStudentType());
                            }
                            
                            // 打印最终的JSON
                            String finalJson = jsonObject.toString();
                            Log.d(TAG, "序列化结果: " + finalJson);
                            Log.d(TAG, "JSON长度: " + finalJson.length());
                            Log.d(TAG, "密码字段在JSON中的位置: " + finalJson.indexOf("\"password\":"));
                            
                            return jsonObject;
                        }
                    })
                    .serializeNulls()  // 序列化null值
                    .registerTypeAdapter(List.class, new JsonDeserializer<List<?>>() {
                        @Override
                        public List<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            List<Object> list = new ArrayList<>();
                            if (json.isJsonArray()) {
                                JsonArray jsonArray = json.getAsJsonArray();
                                for (JsonElement element : jsonArray) {
                                    if (element.isJsonObject()) {
                                        // 如果是对象，优先尝试解析为UserInfo
                                        try {
                                            JsonObject jsonObject = element.getAsJsonObject();
                                            if (jsonObject.has("id") && jsonObject.has("username")) {
                                                list.add(context.deserialize(element, 
                                                    com.lianxiangdaimaowang.lumina.community.model.LocalPost.UserInfo.class));
                                            } else {
                                                // 如果不是UserInfo，保留为JsonObject
                                                list.add(jsonObject);
                                            }
                                        } catch (Exception e) {
                                            // 解析失败，保留原始JsonElement
                                            list.add(element);
                                        }
                                    } else if (element.isJsonPrimitive()) {
                                        // 如果是基本类型（如字符串），直接添加
                                        JsonPrimitive primitive = element.getAsJsonPrimitive();
                                        if (primitive.isString()) {
                                            list.add(primitive.getAsString());
                                        } else if (primitive.isNumber()) {
                                            list.add(primitive.getAsNumber());
                                        } else if (primitive.isBoolean()) {
                                            list.add(primitive.getAsBoolean());
                                        } else {
                                            list.add(primitive.toString());
                                        }
                                    } else {
                                        // 其他类型，保留为JsonElement
                                        list.add(element);
                                    }
                                }
                            }
                            return list;
                        }
                    })
                    .setLenient()
                    .create();
            
            // 创建Retrofit实例
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient.build())
                    .build();
            
            // 创建ApiService实例
            apiService = retrofit.create(ApiService.class);
        }
        
        return apiService;
    }
    
    /**
     * LocalPost类的反序列化器
     * 用于解决服务器端返回的复杂JSON结构与客户端模型不匹配的问题
     */
    public static class LocalPostDeserializer implements JsonDeserializer<LocalPost> {
        @Override
        public LocalPost deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                LocalPost post = new LocalPost();
                
                // 提取主要帖子信息
                if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull()) {
                    post.setId(jsonObject.get("id").getAsString());
                }
                
                if (jsonObject.has("title") && !jsonObject.get("title").isJsonNull()) {
                    post.setTitle(jsonObject.get("title").getAsString());
                }
                
                if (jsonObject.has("content") && !jsonObject.get("content").isJsonNull()) {
                    post.setContent(jsonObject.get("content").getAsString());
                }
                
                if (jsonObject.has("viewCount")) {
                    post.setViewCount(jsonObject.get("viewCount").getAsInt());
                }
                
                if (jsonObject.has("likeCount")) {
                    post.setLikeCount(jsonObject.get("likeCount").getAsInt());
                }
                
                if (jsonObject.has("commentCount")) {
                    post.setCommentCount(jsonObject.get("commentCount").getAsInt());
                }
                
                if (jsonObject.has("status")) {
                    post.setStatus(jsonObject.get("status").getAsInt());
                }
                
                // 解析日期字段
                if (jsonObject.has("createdAt")) {
                    String dateStr = jsonObject.get("createdAt").getAsString();
                    post.setCreateTime(parseDate(dateStr));
                }
                
                if (jsonObject.has("updatedAt")) {
                    String dateStr = jsonObject.get("updatedAt").getAsString();
                    post.setUpdatedAt(parseDate(dateStr));
                }
                
                // 解析用户信息
                if (jsonObject.has("user") && !jsonObject.get("user").isJsonNull()) {
                    JsonObject userObj = jsonObject.getAsJsonObject("user");
                    LocalPost.UserInfo userInfo = context.deserialize(userObj, LocalPost.UserInfo.class);
                    post.setUser(userInfo);
                    
                    // 从用户对象中提取用户ID和用户名
                    if (userInfo != null) {
                        post.setUserId(userInfo.getId());
                        post.setUsername(userInfo.getUsername());
                    }
                }
                
                // 解析点赞列表
                if (jsonObject.has("likes") && jsonObject.get("likes").isJsonArray()) {
                    JsonArray likesArray = jsonObject.getAsJsonArray("likes");
                    List<Object> likes = new ArrayList<>();
                    
                    for (JsonElement likeElement : likesArray) {
                        if (likeElement.isJsonObject()) {
                            JsonObject likeObj = likeElement.getAsJsonObject();
                            if (likeObj.has("user") && !likeObj.get("user").isJsonNull()) {
                                // 从点赞对象中提取用户信息
                                JsonObject likeUserObj = likeObj.getAsJsonObject("user");
                                LocalPost.UserInfo likeUserInfo = context.deserialize(likeUserObj, LocalPost.UserInfo.class);
                                likes.add(likeUserInfo);
                            }
                        }
                    }
                    
                    post.setLikes(likes);
                }
                
                // 解析评论列表
                if (jsonObject.has("comments") && jsonObject.get("comments").isJsonArray()) {
                    JsonArray commentsArray = jsonObject.getAsJsonArray("comments");
                    List<LocalPost.Comment> comments = new ArrayList<>();
                    
                    for (JsonElement commentElement : commentsArray) {
                        if (commentElement.isJsonObject()) {
                            LocalPost.Comment comment = context.deserialize(commentElement, LocalPost.Comment.class);
                            comments.add(comment);
                        }
                    }
                    
                    post.setComments(comments);
                }
                
                // 处理收藏列表
                if (jsonObject.has("favorites") && jsonObject.get("favorites").isJsonArray()) {
                    JsonArray favoritesArray = jsonObject.getAsJsonArray("favorites");
                    List<String> favorites = new ArrayList<>();
                    
                    // 直接获取当前登录用户ID
                    String currentUserId = LocalDataManager.getInstance(null).getCurrentUserId();
                    Log.d(TAG, "当前用户ID: " + currentUserId);
                    
                    // 收集所有可能的ID映射关系
                    Map<String, String> possibleMappings = new HashMap<>();
                    
                    for (JsonElement favoriteElement : favoritesArray) {
                        if (favoriteElement.isJsonObject()) {
                            JsonObject favoriteObj = favoriteElement.getAsJsonObject();
                            
                            if (favoriteObj.has("user") && !favoriteObj.get("user").isJsonNull()) {
                                JsonObject userObj = favoriteObj.getAsJsonObject("user");
                                if (userObj.has("id") && !userObj.get("id").isJsonNull()) {
                                    String userId = userObj.get("id").getAsString();
                                    favorites.add(userId);
                                    
                                    // 检查这个用户信息，探测可能的ID映射关系
                                    if (userObj.has("username") && !userObj.get("username").isJsonNull()) {
                                        String username = userObj.get("username").getAsString();
                                        
                                        // 记录ID和用户名的映射，帮助后续识别
                                        possibleMappings.put(username, userId);
                                        
                                        // 检查这个用户ID是否是当前登录用户的服务器ID
                                        if (currentUserId != null && !currentUserId.isEmpty()) {
                                            // 记录匹配情况
                                            boolean isCurrentUser = userId.equals(currentUserId);
                                            Log.d(TAG, "收藏用户信息 - 服务器ID: " + userId + ", 用户名: " + username + 
                                                    ", 与当前用户匹配: " + isCurrentUser + 
                                                    ", 当前用户ID: " + currentUserId);
                                            
                                            // 如果用户名与当前登录的用户ID相同，可能表示这是同一用户的不同表示
                                            if (username.equals(currentUserId)) {
                                                Log.d(TAG, "发现ID映射关系 - 客户端ID: " + currentUserId + " -> 服务器ID: " + userId);
                                                // 记录这个映射关系
                                                LocalDataManager dm = LocalDataManager.getInstance(null);
                                                if (dm != null) {
                                                    dm.saveValue("server_user_id_" + currentUserId, userId);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (favoriteObj.has("id") && !favoriteObj.get("id").isJsonNull()) {
                                // 有些API可能直接返回ID
                                String userId = favoriteObj.get("id").getAsString();
                                favorites.add(userId);
                            }
                        } else if (favoriteElement.isJsonPrimitive()) {
                            // 处理直接作为字符串或数字的ID
                            String userId = favoriteElement.getAsString();
                            favorites.add(userId);
                        }
                    }
                    
                    // 检查帖子作者与收藏关系
                    if (jsonObject.has("user") && !jsonObject.get("user").isJsonNull()) {
                        JsonObject authorObj = jsonObject.getAsJsonObject("user");
                        if (authorObj.has("id") && authorObj.has("username")) {
                            String authorId = authorObj.get("id").getAsString();
                            String authorName = authorObj.get("username").getAsString();
                            
                            // 如果作者名与当前用户ID相同，这可能是映射关系
                            if (currentUserId != null && authorName.equals(currentUserId)) {
                                Log.d(TAG, "从作者信息发现ID映射关系 - 客户端ID: " + currentUserId + " -> 服务器ID: " + authorId);
                                // 保存映射关系
                                LocalDataManager dm = LocalDataManager.getInstance(null);
                                if (dm != null) {
                                    dm.saveValue("server_user_id_" + currentUserId, authorId);
                                }
                            }
                        }
                    }
                    
                    Log.d(TAG, "帖子ID: " + post.getId() + " 的收藏用户IDs: " + favorites);
                    post.setFavorites(favorites);
                    post.setFavoriteCount(favorites.size());
                }
                
                return post;
            } catch (Exception e) {
                Log.e(TAG, "解析帖子时出错: " + e.getMessage(), e);
                return null;
            }
        }
        
        // 辅助方法：解析日期字符串
        private Date parseDate(String dateStr) {
            if (dateStr == null || dateStr.isEmpty()) {
                return new Date();
            }
            
            SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd", Locale.US)
            };
            
            for (SimpleDateFormat format : formats) {
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    return format.parse(dateStr);
                } catch (ParseException e) {
                    // 尝试下一个格式
                }
            }
            
            return new Date();
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
    
    /**
     * 从响应中获取错误消息
     * @param response Retrofit响应对象
     * @return 错误消息字符串
     */
    public static String getErrorMessage(retrofit2.Response<?> response) {
        if (response == null) {
            return "未知错误";
        }
        
        if (response.isSuccessful()) {
            return ""; // 成功响应没有错误
        }
        
        String errorBody = null;
        try {
            errorBody = response.errorBody() != null ? response.errorBody().string() : null;
        } catch (IOException e) {
            Log.e(TAG, "解析错误响应失败", e);
            return "解析错误响应失败";
        }
        
        if (errorBody == null || errorBody.isEmpty()) {
            return "HTTP错误: " + response.code();
        }
        
        // 尝试解析错误JSON
        try {
            JsonElement element = JsonParser.parseString(errorBody);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("message")) {
                    return object.get("message").getAsString();
                } else if (object.has("error")) {
                    return object.get("error").getAsString();
                }
            }
            return errorBody; // 如果无法解析特定字段，返回整个错误体
        } catch (Exception e) {
            Log.e(TAG, "解析错误JSON失败", e);
            return errorBody; // 如果JSON解析失败，返回原始错误体
        }
    }
    
    /**
     * 安全地获取响应体内容的字符串表示
     * 这个方法尝试从响应中读取原始内容，避免流关闭问题
     * @param response Retrofit响应对象
     * @return 响应体内容字符串，如果无法获取则返回null
     */
    public static String getRawResponseBodyAsString(retrofit2.Response<?> response) {
        if (response == null || !response.isSuccessful()) {
            return null;
        }
        
        try {
            // 首先检查响应体是否已被转换，如果已转换则直接使用转换后的对象
            if (response.body() != null) {
                // 如果响应体已被转换为对象，直接使用Gson转为字符串
                try {
                    Gson gson = new GsonBuilder().serializeNulls().create();
                    return gson.toJson(response.body());
                } catch (Exception e) {
                    Log.d(TAG, "使用Gson转换已转换的响应体失败，尝试其他方法: " + e.getMessage());
                }
            }
            
            // 复制响应以避免关闭原始响应流
            okhttp3.Response rawResponse = response.raw();
            if (rawResponse == null) {
                return null;
            }
            
            // 注意：这种方式可能在某些版本的OkHttp中不可用，因为响应体可能已经被消费
            String result = null;
            try {
                okhttp3.ResponseBody responseBody = rawResponse.peekBody(Long.MAX_VALUE);
                if (responseBody != null) {
                    result = responseBody.string();
                }
            } catch (Exception e) {
                Log.e(TAG, "获取原始响应内容失败: " + e.getMessage(), e);
                // 失败时尝试其他方式
            }
            
            // 如果上面的方法失败，尝试检查headers中的原始内容
            if (result == null || result.isEmpty()) {
                // 返回更多有用的信息，如HTTP状态码、头信息等
                StringBuilder info = new StringBuilder();
                info.append("HTTP ")
                    .append(rawResponse.code())
                    .append(" ")
                    .append(rawResponse.message())
                    .append("\n");
                
                okhttp3.Headers headers = rawResponse.headers();
                for (int i = 0; i < headers.size(); i++) {
                    info.append(headers.name(i))
                        .append(": ")
                        .append(headers.value(i))
                        .append("\n");
                }
                
                // 看是否可以从errorBody中获取内容
                if (response.errorBody() != null) {
                    try {
                        String errorContent = response.errorBody().string();
                        if (errorContent != null && !errorContent.isEmpty()) {
                            info.append(errorContent);
                            return info.toString();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "读取errorBody失败: " + e.getMessage());
                    }
                }
                
                info.append("[无法读取响应体内容]");
                result = info.toString();
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "获取原始响应内容失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 重置API客户端，在需要刷新配置时调用
     */
    public static void resetApiClient() {
        retrofit = null;
        okHttpClient = null;
        apiService = null;
        Log.d(TAG, "API客户端已重置");
    }
    
    /**
     * 打印请求和响应的详细内容（用于调试）
     * @param tag 日志标签
     * @param request 请求对象
     * @param response 响应对象
     */
    public static void logRequestAndResponse(String tag, Object request, Object response) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (request != null) {
                Log.d(tag, "请求内容: \n" + gson.toJson(request));
            }
            if (response != null) {
                Log.d(tag, "响应内容: \n" + gson.toJson(response));
            }
        } catch (Exception e) {
            Log.e(tag, "打印请求/响应内容失败: " + e.getMessage());
        }
    }
} 