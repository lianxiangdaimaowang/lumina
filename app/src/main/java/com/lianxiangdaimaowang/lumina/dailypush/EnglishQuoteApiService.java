package com.lianxiangdaimaowang.lumina.dailypush;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 英文名人名言API服务类
 * 负责调用Quotable API获取英文名人名言
 */
public class EnglishQuoteApiService {
    private static final String TAG = "EnglishQuoteApiService";
    
    // Quotable API 端点
    private static final String QUOTABLE_API_URL = "https://api.quotable.io/random";
    
    // 备用API端点
    private static final String BACKUP_API_URL = "https://zenquotes.io/api/random";
    private static final String BACKUP_API_URL2 = " https://hitokoto.cn/";
    
    // 主题标签列表（用于按主题筛选名言）
    private static final String[] TOPICS = {
        "wisdom", "success", "education", "life", "happiness", 
        "love", "friendship", "technology", "leadership", "future",
        "courage", "science", "inspirational", "faith", "hope"
    };
    
    // 著名作者列表（用于按作者筛选名言）
    private static final String[] AUTHORS = {
        "Albert Einstein", "Winston Churchill", "Nelson Mandela", "Steve Jobs",
        "William Shakespeare", "Mark Twain", "Oscar Wilde", "Maya Angelou",
        "Mahatma Gandhi", "Martin Luther King Jr"
    };
    
    // OkHttpClient实例
    private final OkHttpClient httpClient;
    
    // 英文名人名言数据模型
    public static class EnglishQuoteData {
        private String content;
        private String author;
        private String tags; // 主题标签
        
        public EnglishQuoteData(String content, String author, String tags) {
            this.content = content;
            this.author = author;
            this.tags = tags;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getAuthor() {
            return author;
        }
        
        public String getTags() {
            return tags;
        }
        
        public String getFormattedContent() {
            return "\"" + content + "\"\n\n— " + author;
        }
    }
    
    // 单例模式
    private static EnglishQuoteApiService instance;
    
    private EnglishQuoteApiService() {
        // 创建一个更宽容的SSL验证策略的OkHttpClient
        httpClient = getUnsafeOkHttpClient();
    }
    
    /**
     * 创建一个忽略SSL证书问题的OkHttpClient
     * 注意：这在生产环境中不是推荐的做法，但在这里用于解决证书过期问题
     */
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // 创建一个信任所有证书的TrustManager
            final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            // 安装自定义TrustManager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // 创建SSL Socket Factory
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true; // 接受所有主机名
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        } catch (Exception e) {
            // 如果创建失败，回退到标准OkHttpClient
            Log.e(TAG, "创建自定义OkHttpClient失败，使用标准客户端", e);
            return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        }
    }
    
    public static synchronized EnglishQuoteApiService getInstance() {
        if (instance == null) {
            instance = new EnglishQuoteApiService();
        }
        return instance;
    }
    
    /**
     * 获取随机英文名人名言
     * @param callback 回调接口
     */
    public void getRandomQuote(TianApiManager.ApiCallback<EnglishQuoteData> callback) {
        // 构建API URL
        String url = QUOTABLE_API_URL;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "主API请求失败，尝试备用API", e);
                // 尝试使用备用API
                getQuoteFromBackupApi(callback);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "主API响应错误: " + response.code() + "，尝试备用API");
                    // 尝试使用备用API
                    getQuoteFromBackupApi(callback);
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    
                    // 解析API响应
                    String content = jsonObject.getString("content");
                    String author = jsonObject.getString("author");
                    String tags = jsonObject.optString("tags", "");
                    
                    EnglishQuoteData quoteData = new EnglishQuoteData(content, author, tags);
                    
                    if (callback != null) {
                        callback.onSuccess(quoteData);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析JSON失败，尝试备用API", e);
                    // 尝试使用备用API
                    getQuoteFromBackupApi(callback);
                }
            }
        });
    }
    
    /**
     * 从备用API获取名言
     */
    private void getQuoteFromBackupApi(TianApiManager.ApiCallback<EnglishQuoteData> callback) {
        // 使用ZenQuotes备用API
        Request request = new Request.Builder()
                .url(BACKUP_API_URL)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "备用API请求也失败，尝试第二个备用API", e);
                // 尝试第二个备用API
                getQuoteFromSecondBackupApi(callback);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "备用API响应错误: " + response.code() + "，尝试第二个备用API");
                    // 尝试第二个备用API
                    getQuoteFromSecondBackupApi(callback);
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    
                    // ZenQuotes返回一个数组，但我们只需要第一个元素
                    JSONObject jsonObject = new JSONObject(responseData.substring(1, responseData.length() - 1));
                    
                    String content = jsonObject.getString("q");
                    String author = jsonObject.getString("a");
                    
                    EnglishQuoteData quoteData = new EnglishQuoteData(content, author, "");
                    
                    if (callback != null) {
                        callback.onSuccess(quoteData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析备用API JSON失败，尝试第二个备用API", e);
                    // 尝试第二个备用API
                    getQuoteFromSecondBackupApi(callback);
                }
            }
        });
    }
    
    /**
     * 从第二个备用API获取名言
     */
    private void getQuoteFromSecondBackupApi(TianApiManager.ApiCallback<EnglishQuoteData> callback) {
        // 使用type.fit备用API
        Request request = new Request.Builder()
                .url(BACKUP_API_URL2)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "所有API都失败，使用本地备用数据", e);
                // 所有API都失败，使用本地备用数据
                if (callback != null) {
                    callback.onSuccess(getFallbackQuote());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "第二个备用API响应错误: " + response.code() + "，使用本地备用数据");
                    // 所有API都失败，使用本地备用数据
                    if (callback != null) {
                        callback.onSuccess(getFallbackQuote());
                    }
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    
                    // 这个API返回一个数组，随机选择一个元素
                    org.json.JSONArray jsonArray = new org.json.JSONArray(responseData);
                    int randomIndex = new Random().nextInt(jsonArray.length());
                    JSONObject jsonObject = jsonArray.getJSONObject(randomIndex);
                    
                    String content = jsonObject.getString("text");
                    String author = jsonObject.optString("author", "Anonymous");
                    
                    // 防止null作者
                    if (author == null || author.equals("null")) {
                        author = "Anonymous";
                    }
                    
                    EnglishQuoteData quoteData = new EnglishQuoteData(content, author, "");
                    
                    if (callback != null) {
                        callback.onSuccess(quoteData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析第二个备用API JSON失败，使用本地备用数据", e);
                    // 所有API都失败，使用本地备用数据
                    if (callback != null) {
                        callback.onSuccess(getFallbackQuote());
                    }
                }
            }
        });
    }
    
    /**
     * 按主题获取英文名人名言
     * @param topic 主题标签
     * @param callback 回调接口
     */
    public void getQuoteByTopic(String topic, TianApiManager.ApiCallback<EnglishQuoteData> callback) {
        // 构建API URL，添加主题标签参数
        String url = QUOTABLE_API_URL + "?tags=" + topic;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API请求失败", e);
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (callback != null) {
                        callback.onError("服务器响应错误: " + response.code());
                    }
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    
                    // 解析API响应
                    String content = jsonObject.getString("content");
                    String author = jsonObject.getString("author");
                    String tags = jsonObject.optString("tags", topic);
                    
                    EnglishQuoteData quoteData = new EnglishQuoteData(content, author, tags);
                    
                    if (callback != null) {
                        callback.onSuccess(quoteData);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析JSON失败", e);
                    if (callback != null) {
                        callback.onError("数据解析失败: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * 按作者获取英文名人名言
     * @param author 作者名称
     * @param callback 回调接口
     */
    public void getQuoteByAuthor(String author, TianApiManager.ApiCallback<EnglishQuoteData> callback) {
        // 构建API URL，添加作者参数
        String url = QUOTABLE_API_URL + "?author=" + author.replace(" ", "+");
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API请求失败", e);
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (callback != null) {
                        callback.onError("服务器响应错误: " + response.code());
                    }
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    
                    // 解析API响应
                    String content = jsonObject.getString("content");
                    String authorName = jsonObject.getString("author");
                    String tags = jsonObject.optString("tags", "");
                    
                    EnglishQuoteData quoteData = new EnglishQuoteData(content, authorName, tags);
                    
                    if (callback != null) {
                        callback.onSuccess(quoteData);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析JSON失败", e);
                    if (callback != null) {
                        callback.onError("数据解析失败: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * 获取随机主题
     * @return 随机主题标签
     */
    public String getRandomTopic() {
        return TOPICS[new Random().nextInt(TOPICS.length)];
    }
    
    /**
     * 获取随机作者
     * @return 随机作者名称
     */
    public String getRandomAuthor() {
        return AUTHORS[new Random().nextInt(AUTHORS.length)];
    }
    
    /**
     * 获取内置英文名人名言数据（当API调用失败时使用）
     * @return 随机生成的内置英文名言数据
     */
    public EnglishQuoteData getFallbackQuote() {
        // 内置英文名言数据
        String[][] quotes = {
            {"The greatest glory in living lies not in never falling, but in rising every time we fall.", "Nelson Mandela", "inspirational"},
            {"The way to get started is to quit talking and begin doing.", "Walt Disney", "motivation"},
            {"Your time is limited, so don't waste it living someone else's life.", "Steve Jobs", "life"},
            {"If life were predictable it would cease to be life, and be without flavor.", "Eleanor Roosevelt", "life"},
            {"If you look at what you have in life, you'll always have more. If you look at what you don't have in life, you'll never have enough.", "Oprah Winfrey", "gratitude"},
            {"If you set your goals ridiculously high and it's a failure, you will fail above everyone else's success.", "James Cameron", "success"},
            {"Life is what happens when you're busy making other plans.", "John Lennon", "life"},
            {"The future belongs to those who believe in the beauty of their dreams.", "Eleanor Roosevelt", "dreams"},
            {"Tell me and I forget. Teach me and I remember. Involve me and I learn.", "Benjamin Franklin", "education"},
            {"The best and most beautiful things in the world cannot be seen or even touched — they must be felt with the heart.", "Helen Keller", "beauty"},
            {"It is during our darkest moments that we must focus to see the light.", "Aristotle", "hope"},
            {"Do not go where the path may lead, go instead where there is no path and leave a trail.", "Ralph Waldo Emerson", "leadership"},
            {"Always remember that you are absolutely unique. Just like everyone else.", "Margaret Mead", "humor"},
            {"Don't judge each day by the harvest you reap but by the seeds that you plant.", "Robert Louis Stevenson", "life"},
            {"The real test is not whether you avoid this failure, because you won't. It's whether you let it harden or shame you into inaction, or whether you learn from it.", "Barack Obama", "failure"}
        };
        
        // 随机选择一条名言
        int randomIndex = new Random().nextInt(quotes.length);
        String[] quote = quotes[randomIndex];
        
        return new EnglishQuoteData(quote[0], quote[1], quote[2]);
    }
} 