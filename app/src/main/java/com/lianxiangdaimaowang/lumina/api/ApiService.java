package com.lianxiangdaimaowang.lumina.api;

import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;
import com.lianxiangdaimaowang.lumina.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public interface ApiService {
    // 用户认证相关API
    @POST("api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> loginData);
    
    @POST("api/auth/register")
    Call<Map<String, Object>> register(@Body Map<String, String> registerData);
    
    // 忘记密码相关API
    @POST("api/auth/verification-code")
    Call<Map<String, Object>> requestVerificationCode(@Body Map<String, String> data);
    
    @POST("api/auth/reset-password")
    Call<Map<String, Object>> resetPassword(@Body Map<String, String> data);
    
    // 笔记相关API
    @GET("api/notes")
    Call<Map<String, Object>> getAllNotes();
    
    @GET("api/notes/{id}")
    Call<Note> getNote(@Path("id") String id);
    
    @POST("api/notes")
    Call<Note> createNote(@Body Note note);
    
    @PUT("api/notes/{id}")
    Call<Note> updateNote(@Path("id") String id, @Body Note note);
    
    @POST("api/notes")
    Call<Note> createNoteWithMap(@Body Map<String, Object> note);
    
    @PUT("api/notes/{id}")
    Call<Note> updateNoteWithMap(@Path("id") String id, @Body Map<String, Object> note);
    
    @DELETE("api/notes/{id}")
    Call<Void> deleteNote(@Path("id") String id);
    
    // 社区帖子相关API
    @GET("api/posts")
    Call<List<LocalPost>> getAllPosts();
    
    @GET("api/posts/hot")
    Call<JsonObject> getHotPosts(@Query("limit") int limit);
    
    @GET("api/users/{userId}/favorites")
    Call<JsonObject> getUserFavoritePosts(@Path("userId") String userId);
    
    @GET("api/users/me/favorites")
    Call<JsonObject> getCurrentUserFavorites();
    
    @POST("api/posts/{id}/like")
    Call<Void> likePost(@Path("id") String postId, @Body Map<String, String> params);
    
    @POST("api/posts/{id}/unlike")
    Call<Void> unlikePost(@Path("id") String postId, @Body Map<String, String> params);
    
    @POST("api/posts/{id}/favorite")
    Call<Void> favoritePost(@Path("id") String postId, @Body Map<String, String> params);
    
    @POST("api/posts/{id}/unfavorite")
    Call<Void> unfavoritePost(@Path("id") String postId, @Body Map<String, String> params);
    
    @GET("api/posts/{id}")
    Call<LocalPost> getPost(@Path("id") String id);
    
    @POST("api/posts")
    Call<LocalPost> createPost(@Body LocalPost post);
    
    @POST("api/posts/{id}")
    Call<LocalPost> updatePost(@Path("id") String id, @Body LocalPost post);
    
    @DELETE("api/posts/{id}")
    Call<Void> deletePost(@Path("id") String id);
    
    // 用户相关API
    @GET("api/users/me")
    Call<User> getCurrentUser();
    
    @PUT("api/users/me")
    Call<User> updateUser(@Body User user);
}