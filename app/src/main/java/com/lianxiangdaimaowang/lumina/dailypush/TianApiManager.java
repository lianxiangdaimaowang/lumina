package com.lianxiangdaimaowang.lumina.dailypush;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 天行数据API管理类
 * 负责处理对天行数据API的调用
 */
public class TianApiManager {
    private static final String TAG = "TianApiManager";
    
    // 天行数据API密钥，实际应用中应该存储在安全的位置
    private static final String API_KEY = "0cd1cd7f52188eb63d6bad916d8fc452";
    
    // API基础URL
    private static final String BASE_URL = "https://apis.tianapi.com";
    
    // 实例化OkHttpClient
    private final OkHttpClient httpClient;
    
    // 单例模式
    private static TianApiManager instance;
    
    private TianApiManager() {
        httpClient = new OkHttpClient();
    }
    
    public static synchronized TianApiManager getInstance() {
        if (instance == null) {
            instance = new TianApiManager();
        }
        return instance;
    }
    
    /**
     * 回调接口，用于返回API调用结果
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
    
    /**
     * 获取API完整URL
     * @param endpoint API端点
     * @param params 参数字符串
     * @return 完整的URL
     */
    private String getFullUrl(String endpoint, String params) {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append(endpoint);
        url.append("?key=").append(API_KEY);
        if (params != null && !params.isEmpty()) {
            url.append("&").append(params);
        }
        return url.toString();
    }
    
    /**
     * 执行网络请求
     * @param url 请求URL
     * @param callback 回调接口
     */
    private void executeRequest(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        httpClient.newCall(request).enqueue(callback);
    }
    
    /**
     * 名人名言类型ID映射到中文名称
     * @param typeId 类型ID
     * @return 中文类型名称
     */
    public String getQuoteTypeTitle(int typeId) {
        switch (typeId) {
            case 1: return "励志名言";
            case 2: return "爱情名言";
            case 3: return "名著名言";
            case 4: return "名人名言";
            case 5: return "生活感悟";
            case 11: return "哲理名言";
            case 12: return "经典名言";
            case 13: return "个性签名";
            case 18: return "学习名言";
            case 24: return "科学名言";
            default: return "每日格言";
        }
    }
    
    /**
     * 获取名人名言类型列表
     * @return 类型ID数组
     */
    public int[] getQuoteTypeIds() {
        return new int[]{1, 2, 3, 4, 5, 11, 12, 13, 18, 24};
    }
    
    /**
     * 随机选择一个名言类型ID
     * @return 随机类型ID
     */
    public int getRandomQuoteTypeId() {
        int[] typeIds = getQuoteTypeIds();
        return typeIds[new Random().nextInt(typeIds.length)];
    }
} 