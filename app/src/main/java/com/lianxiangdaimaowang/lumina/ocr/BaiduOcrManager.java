package com.lianxiangdaimaowang.lumina.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import com.lianxiangdaimaowang.lumina.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * 百度OCR API管理类 - 使用在线API而非离线SDK
 */
public class BaiduOcrManager {
    private static final String TAG = "BaiduOcrManager";
    
    // 百度OCR API Key和Secret Key
    private static final String API_KEY = "gzmcAEAFlNQKSNzjmb9NBbht";
    private static final String SECRET_KEY = "ZQuVkNsqOjMl2L8d7EwlbbleTPcLGrae";
    
    // API URL
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";
    
    // 单例实例
    private static BaiduOcrManager instance;
    
    // 初始化状态
    private boolean initialized = false;
    
    // 上下文
    private Context context;
    
    // access token
    private String accessToken;
    private long tokenExpireTime = 0;
    
    // 线程池
    private final Executor executor;
    
    // OCR回调接口
    public interface OcrResultCallback {
        void onSuccess(String text);
        void onFailure(String error);
    }
    
    private BaiduOcrManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        initOcr();
    }
    
    /**
     * 获取BaiduOcrManager的单例实例
     * @param context 上下文
     * @return BaiduOcrManager实例
     */
    public static synchronized BaiduOcrManager getInstance(Context context) {
        if (instance == null) {
            instance = new BaiduOcrManager(context);
        }
        return instance;
    }
    
    /**
     * 初始化OCR
     */
    private void initOcr() {
        try {
            Log.d(TAG, "初始化百度OCR API");
            
            // 检查是否已经有有效token
            if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                Log.d(TAG, "已有有效token，OCR初始化成功");
                initialized = true;
                return;
            }
            
            // 同步获取token，确保初始化完成
            try {
                getAccessToken();
                initialized = true;
                Log.d(TAG, "百度OCR API同步初始化成功");
            } catch (Exception e) {
                Log.e(TAG, "获取百度OCR API Token失败", e);
                initialized = false;
                
                // 如果同步获取失败，尝试异步获取
                executor.execute(() -> {
                    try {
                        getAccessToken();
                        initialized = true;
                        Log.d(TAG, "百度OCR API异步初始化成功");
                    } catch (Exception ex) {
                        Log.e(TAG, "异步获取百度OCR API Token也失败", ex);
                        initialized = false;
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "百度OCR API初始化失败", e);
            initialized = false;
        }
    }
    
    /**
     * 获取百度OCR API的access token
     */
    private synchronized void getAccessToken() throws Exception {
        // 如果token还有效，不需要重新获取
        long currentTime = System.currentTimeMillis();
        if (accessToken != null && currentTime < tokenExpireTime) {
            return;
        }
        
        // 尝试3次获取token
        Exception lastException = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                Log.d(TAG, "尝试获取access token，第" + (retry + 1) + "次");
                
                URL url = new URL(TOKEN_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(10000); // 10秒连接超时
                connection.setReadTimeout(15000);    // 15秒读取超时
                connection.setDoOutput(true);
                
                String params = "grant_type=client_credentials" +
                        "&client_id=" + API_KEY +
                        "&client_secret=" + SECRET_KEY;
                
                try (java.io.OutputStream os = connection.getOutputStream()) {
                    os.write(params.getBytes("UTF-8"));
                    os.flush();
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readInputStream(connection.getInputStream());
                    JSONObject jsonResponse = new JSONObject(response);
                    accessToken = jsonResponse.getString("access_token");
                    int expiresIn = jsonResponse.getInt("expires_in");
                    tokenExpireTime = currentTime + (expiresIn - 60) * 1000; // 提前60秒过期，保险起见
                    Log.d(TAG, "Access token获取成功: " + accessToken);
                    return; // 成功获取，直接返回
                } else {
                    String errorMsg = "获取access token HTTP错误: " + responseCode;
                    Log.e(TAG, errorMsg);
                    lastException = new IOException(errorMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取access token异常: " + e.getMessage(), e);
                lastException = e;
                // 等待一段时间后重试
                if (retry < 2) {
                    try {
                        Thread.sleep(1000 * (retry + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        // 所有重试都失败了
        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("获取access token失败，未知错误");
        }
    }
    
    /**
     * 从输入流读取字符串
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
    
    /**
     * 判断OCR是否已初始化
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized && accessToken != null;
    }
    
    /**
     * 从Uri识别文字
     * @param imageUri 图片Uri
     * @param callback 回调
     */
    public void recognizeFromUri(Uri imageUri, OcrResultCallback callback) {
        if (!initialized) {
            if (callback != null) {
                callback.onFailure(context.getString(R.string.ocr_init_failed, "OCR未初始化"));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                // 从URI获取Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
                recognizeFromBitmap(bitmap, callback);
            } catch (IOException e) {
                Log.e(TAG, "从Uri获取Bitmap失败", e);
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 从Bitmap识别文字
     * @param bitmap 图片Bitmap
     * @param callback 回调
     */
    public void recognizeFromBitmap(Bitmap bitmap, OcrResultCallback callback) {
        if (bitmap == null) {
            if (callback != null) {
                callback.onFailure("图片无效");
            }
            return;
        }
        
        // 确保初始化完成
        if (!initialized) {
            try {
                initOcr();
            } catch (Exception e) {
                Log.e(TAG, "初始化OCR失败", e);
                if (callback != null) {
                    callback.onFailure("OCR服务初始化失败: " + e.getMessage());
                }
                return;
            }
        }
        
        executor.execute(() -> {
            try {
                // 如果图片太大，进行压缩
                Bitmap processedBitmap = bitmap;
                if (bitmap.getWidth() > 4096 || bitmap.getHeight() > 4096) {
                    Log.d(TAG, "图片尺寸过大，进行压缩: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    float scale = Math.min(4096f / bitmap.getWidth(), 4096f / bitmap.getHeight());
                    int newWidth = Math.round(bitmap.getWidth() * scale);
                    int newHeight = Math.round(bitmap.getHeight() * scale);
                    processedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    Log.d(TAG, "压缩后尺寸: " + processedBitmap.getWidth() + "x" + processedBitmap.getHeight());
                }
                
                // 将Bitmap转换为Base64
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                
                // 检查图片大小，确保不超过API限制
                if (imageBytes.length > 4 * 1024 * 1024) { // 4MB
                    Log.w(TAG, "图片尺寸过大: " + (imageBytes.length / 1024) + "KB，进一步压缩");
                    byteArrayOutputStream.reset();
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                    imageBytes = byteArrayOutputStream.toByteArray();
                    
                    // 如果还是太大，继续压缩
                    if (imageBytes.length > 4 * 1024 * 1024) {
                        byteArrayOutputStream.reset();
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                        imageBytes = byteArrayOutputStream.toByteArray();
                    }
                }
                
                Log.d(TAG, "图片转换为Base64，大小: " + (imageBytes.length / 1024) + "KB");
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                
                // 识别Base64图片
                recognizeFromBase64(base64Image, callback);
            } catch (Exception e) {
                Log.e(TAG, "从Bitmap识别文字失败", e);
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 从文件识别文字
     * @param file 图片文件
     * @param callback 回调
     */
    public void recognizeFromFile(File file, OcrResultCallback callback) {
        if (!initialized) {
            if (callback != null) {
                callback.onFailure(context.getString(R.string.ocr_init_failed, "OCR未初始化"));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                // 将文件转换为Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), Uri.fromFile(file));
                recognizeFromBitmap(bitmap, callback);
            } catch (Exception e) {
                Log.e(TAG, "从文件识别文字失败", e);
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }
    
    /**
     * 从Base64编码的图片识别文字
     * @param base64Image Base64编码的图片
     * @param callback 回调
     */
    private void recognizeFromBase64(String base64Image, OcrResultCallback callback) {
        if (!initialized || accessToken == null) {
            if (callback != null) {
                callback.onFailure("OCR服务未初始化");
            }
            return;
        }
        
        HttpURLConnection connection = null;
        try {
            // 确保token有效
            getAccessToken();
            
            URL url = new URL(OCR_URL + "?access_token=" + accessToken);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            
            String params = "image=" + URLEncoder.encode(base64Image, "UTF-8") +
                    "&language_type=CHN_ENG" +
                    "&detect_direction=true" +
                    "&detect_language=true" +
                    "&probability=true";
            
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(params.getBytes("UTF-8"));
                outputStream.flush();
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = readInputStream(inputStream);
                
                // 检查是否有错误
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("error_code") && jsonResponse.getInt("error_code") != 0) {
                    String errorMsg = "百度OCR API错误: " + 
                            jsonResponse.getInt("error_code") + ", " + 
                            jsonResponse.optString("error_msg", "未知错误");
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onFailure(errorMsg);
                    }
                    return;
                }
                
                // 解析OCR结果
                try {
                    String text = parseOcrResult(response);
                    if (callback != null) {
                        if (text.isEmpty()) {
                            callback.onFailure("未能识别任何文字");
                        } else {
                            callback.onSuccess(text);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析OCR结果失败", e);
                    if (callback != null) {
                        callback.onFailure("解析OCR结果失败: " + e.getMessage());
                    }
                }
            } else {
                String errorMsg = "百度OCR API HTTP错误: " + responseCode;
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onFailure(errorMsg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "百度OCR API调用失败", e);
            if (callback != null) {
                callback.onFailure(e.getMessage());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 解析OCR结果
     * @param jsonResponse OCR结果JSON
     * @return 解析后的文本
     * @throws JSONException 如果解析JSON出错
     */
    private String parseOcrResult(String jsonResponse) throws JSONException {
        StringBuilder sb = new StringBuilder();
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            // 检查是否有识别结果
            if (!jsonObject.has("words_result") || jsonObject.isNull("words_result")) {
                Log.w(TAG, "OCR结果中没有words_result字段");
                return "";
            }
            
            // 获取识别到的文字数量
            int wordsResultNum = jsonObject.optInt("words_result_num", 0);
            if (wordsResultNum == 0) {
                Log.w(TAG, "OCR未识别到任何文字");
                return "";
            }
            
            JSONArray wordsResults = jsonObject.getJSONArray("words_result");
            for (int i = 0; i < wordsResults.length(); i++) {
                JSONObject word = wordsResults.getJSONObject(i);
                if (word.has("words")) {
                    sb.append(word.getString("words"));
                    sb.append("\n");
                }
            }
            
            return sb.toString().trim();
        } catch (JSONException e) {
            Log.e(TAG, "解析OCR结果失败: " + jsonResponse, e);
            return "";
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        // 释放资源
        initialized = false;
        accessToken = null;
    }
} 