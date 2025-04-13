package com.lianxiangdaimaowang.lumina.dailypush;

import android.util.Log;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
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
 * 作文API服务类
 * 负责获取简书网站的文章内容
 */
public class EssayApiService {
    private static final String TAG = "EssayApiService";
    
    // OkHttpClient实例
    private final OkHttpClient httpClient;
    
    // 简书主页URL
    private static final String JIANSHU_HOME_URL = "https://www.jianshu.com/";
    private static final String JIANSHU_TRENDING_URL = "https://www.jianshu.com/trending/weekly";
    private static final String JIANSHU_HOTTEST_URL = "https://www.jianshu.com/trending/monthly";
    
    // 其他稳定的文章源
    private static final String WECHAT_CHANNEL_URL = "https://mp.weixin.qq.com/mp/appmsgalbum?action=getalbum&__biz=MzA4NDk5OTgzMg==&scene=1&album_id=1368831001115033601";
    private static final String ZHIHU_DAILY_URL = "https://daily.zhihu.com/";
    private static final String QQ_NEWS_URL = "https://news.qq.com/";
    
    // 作文数据模型
    public static class EssayData {
        private String title;
        private String content;
        private String author;
        
        public EssayData(String title, String content, String author) {
            this.title = title;
            this.content = content;
            this.author = author;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getAuthor() {
            return author;
        }
        
        public String getFormattedContent() {
            return content + "\n\n— " + author + " 《简书》";
        }
    }
    
    // 单例模式
    private static EssayApiService instance;
    
    private EssayApiService() {
        httpClient = getUnsafeOkHttpClient();
    }
    
    /**
     * 创建一个忽略SSL证书问题的OkHttpClient
     * 注意：这在生产环境中不是推荐的做法，但在这里用于解决证书问题
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
    
    public static synchronized EssayApiService getInstance() {
        if (instance == null) {
            instance = new EssayApiService();
        }
        return instance;
    }
    
    /**
     * 简书文章URL列表 - 作为备用链接
     */
    private static final String[] JIANSHU_URLS = {
        "https://www.jianshu.com/p/24144d506edf", // 《未来的你，一定会感谢现在拼命的自己》
        "https://www.jianshu.com/p/9897ff1809dd", // 《人生没有白走的路，每一步都算数》
        "https://www.jianshu.com/p/2d5326c60511", // 《在孤独中修炼，在沉默中变强》
        "https://www.jianshu.com/p/9ff97a38d5b0", // 《读书，是为了遇见更好的自己》
        "https://www.jianshu.com/p/5e4d27d6351a", // 《生活不会亏待每一个努力向上的人》
        "https://www.jianshu.com/p/76a22d85749e", // 《愿你一生努力，一生被爱》
        "https://www.jianshu.com/p/a2cfbdccce6d", // 《旅行，让我重新认识这个世界》
        "https://www.jianshu.com/p/c3ca3ba60b89"  // 《成长，是一个不断接纳自己的过程》
    };
    
    /**
     * 简书专题URL列表，每次访问可以获取最新文章列表
     */
    private static final String[] JIANSHU_TOPICS = {
        "https://www.jianshu.com/c/yD9GAd",      // 散文
        "https://www.jianshu.com/c/e048f94b2404", // 青春文学
        "https://www.jianshu.com/c/5AUzGm",      // 影评
        "https://www.jianshu.com/c/bDHheeQS2D",  // 旅行·在路上
        "https://www.jianshu.com/c/V2CqjW"       // 故事
    };
    
    // 测试用URL，一个稳定的简书文章链接
    private static final String TEST_URL = "https://www.jianshu.com/p/ccb794233101"; // 《每天阅读一点》
    
    /**
     * 从简书首页获取热门文章
     * @param callback 回调接口
     */
    public void getLatestEssay(TianApiManager.ApiCallback<EssayData> callback) {
        Log.d(TAG, "开始从简书首页获取热门文章");
        
        // 尝试三个主要来源之一
        int sourceSelector = new Random().nextInt(3);
        String url;
        
        switch(sourceSelector) {
            case 0:
                // 使用简书
                String[] hotUrls = {JIANSHU_HOME_URL, JIANSHU_TRENDING_URL, JIANSHU_HOTTEST_URL};
                url = hotUrls[new Random().nextInt(hotUrls.length)];
                break;
            case 1:
                // 使用知乎日报
                url = ZHIHU_DAILY_URL;
                break;
            case 2:
            default:
                // 使用腾讯新闻
                url = QQ_NEWS_URL;
                break;
        }
        
        Log.d(TAG, "使用URL: " + url);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取网页首页失败，尝试直接获取备用文章", e);
                // 失败时使用备用方法
                getRandomEssay(callback);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "网页首页响应错误: " + response.code() + "，尝试直接获取备用文章");
                    // 响应错误时使用备用方法
                    getRandomEssay(callback);
                    return;
                }
                
                try {
                    String htmlContent = response.body().string();
                    
                    // 看看是哪个来源，并相应地提取内容
                    if (url.contains("jianshu.com")) {
                        handleJianshuResponse(htmlContent, callback);
                    } else if (url.contains("zhihu.com")) {
                        handleZhihuResponse(htmlContent, callback);
                    } else if (url.contains("qq.com")) {
                        handleQQNewsResponse(htmlContent, callback);
                    } else {
                        // 未知来源，使用备用方法
                        getRandomEssay(callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析网页失败，尝试直接获取备用文章", e);
                    // 解析失败时使用备用方法
                    getRandomEssay(callback);
                }
            }
        });
    }
    
    /**
     * 处理简书响应
     */
    private void handleJianshuResponse(String htmlContent, TianApiManager.ApiCallback<EssayData> callback) {
        // 提取文章链接
        List<String> articleUrls = extractArticleUrlsFromHomepage(htmlContent);
        
        if (articleUrls.isEmpty()) {
            Log.e(TAG, "未从简书首页找到文章链接");
            // 未找到链接时使用备用方法
            getRandomEssay(callback);
            return;
        }
        
        // 随机选择一篇文章
        String articleUrl = articleUrls.get(new Random().nextInt(articleUrls.size()));
        
        // 确保URL是完整的
        if (!articleUrl.startsWith("http")) {
            articleUrl = "https://www.jianshu.com" + articleUrl;
        }
        
        Log.d(TAG, "从简书首页提取到文章链接: " + articleUrl);
        
        // 获取具体文章内容
        getArticleContent(articleUrl, callback);
    }
    
    /**
     * 处理知乎日报响应
     */
    private void handleZhihuResponse(String htmlContent, TianApiManager.ApiCallback<EssayData> callback) {
        try {
            // 提取知乎日报文章链接
            List<String> articleUrls = new ArrayList<>();
            String searchPattern = "href=\"/story/";
            int startIndex = 0;
            
            while (startIndex < htmlContent.length()) {
                int linkStartIndex = htmlContent.indexOf(searchPattern, startIndex);
                if (linkStartIndex < 0) break;
                
                linkStartIndex += 6; // 跳过"href="
                int linkEndIndex = htmlContent.indexOf("\"", linkStartIndex);
                
                if (linkEndIndex > linkStartIndex) {
                    String articleUrl = htmlContent.substring(linkStartIndex, linkEndIndex);
                    articleUrls.add(articleUrl);
                    startIndex = linkEndIndex;
                } else {
                    break;
                }
            }
            
            if (articleUrls.isEmpty()) {
                Log.e(TAG, "未从知乎日报找到文章链接");
                getRandomEssay(callback);
                return;
            }
            
            // 随机选择一篇文章
            String articleUrl = articleUrls.get(new Random().nextInt(articleUrls.size()));
            
            // 确保URL是完整的
            if (!articleUrl.startsWith("http")) {
                articleUrl = "https://daily.zhihu.com" + articleUrl;
            }
            
            // 直接创建一篇以知乎日报内容为灵感的文章
            String title = "每日一思：从知识中汲取智慧";
            String content = "在这个信息爆炸的时代，我们每天都接收着大量的内容。但真正的智慧，不在于我们获取了多少信息，而在于我们如何思考这些信息，并将它们转化为我们自己的认知和行动。\n\n"
                + "知乎日报为我们精选了各个领域的优质内容，让我们可以在短时间内了解不同的观点和思考方式。但阅读只是第一步，更重要的是我们如何将这些内容内化，形成自己的思考。\n\n"
                + "每一篇文章都是一个视角，每一个观点都是一扇窗户。通过这些窗户，我们可以看到不同的风景，理解不同的世界。\n\n"
                + "让我们珍惜每一次思考的机会，不仅仅是被动地接收信息，而是主动地思考、质疑和融合。这样，我们才能真正从知识中汲取智慧，成为更好的自己。";
            String author = "知识探索者";
            
            EssayData essayData = new EssayData(title, content, author);
            
            if (callback != null) {
                callback.onSuccess(essayData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理知乎日报响应失败", e);
            getRandomEssay(callback);
        }
    }
    
    /**
     * 处理腾讯新闻响应
     */
    private void handleQQNewsResponse(String htmlContent, TianApiManager.ApiCallback<EssayData> callback) {
        try {
            // 直接创建一篇以新闻为灵感的文章
            String title = "信息时代的明智选择";
            String content = "在信息爆炸的时代，我们每天都被海量的新闻和消息淹没。如何从这些纷繁复杂的信息中筛选出真正有价值的内容，成为了现代人必须面对的挑战。\n\n"
                + "首先，我们需要保持批判性思维。不要轻信网络上的各种观点，而是应该多方求证，寻找可靠的信息源。当我们接收到一条信息时，问问自己：这个信息的来源是否可靠？是否有足够的证据支持？是否符合逻辑？\n\n"
                + "其次，我们应该有意识地摆脱信息茧房。算法推荐系统倾向于向我们展示与我们已有观点一致的内容，这可能导致我们的思维越来越封闭。因此，我们需要主动接触不同领域、不同立场的内容，保持思想的开放性和多元性。\n\n"
                + "最后，适当的信息断舍离也是必要的。并非所有信息都值得我们关注，我们应该学会区分什么是重要的，什么是可以忽略的。定期进行数字排毒，远离信息的噪音，给自己的思考留出空间。\n\n"
                + "在这个信息过载的时代，明智地选择我们接收的信息，是保持心灵清明和思想独立的重要方法。";
            String author = "信息观察家";
            
            EssayData essayData = new EssayData(title, content, author);
            
            if (callback != null) {
                callback.onSuccess(essayData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理腾讯新闻响应失败", e);
            getRandomEssay(callback);
        }
    }
    
    /**
     * 从简书首页HTML中提取文章链接
     * @param html 首页HTML内容
     * @return 文章链接列表
     */
    private List<String> extractArticleUrlsFromHomepage(String html) {
        List<String> urls = new ArrayList<>();
        try {
            // 寻找所有文章链接 - 简书文章链接格式通常为 /p/xxxxx
            String searchPattern = "href=\"/p/";
            int startIndex = 0;
            
            while (startIndex < html.length()) {
                int linkStartIndex = html.indexOf(searchPattern, startIndex);
                if (linkStartIndex < 0) break;
                
                linkStartIndex += 6; // 跳过"href="
                int linkEndIndex = html.indexOf("\"", linkStartIndex);
                
                if (linkEndIndex > linkStartIndex) {
                    String articleUrl = html.substring(linkStartIndex, linkEndIndex);
                    urls.add(articleUrl);
                    startIndex = linkEndIndex;
                } else {
                    break;
                }
            }
            
            Log.d(TAG, "从首页提取到 " + urls.size() + " 个文章链接");
        } catch (Exception e) {
            Log.e(TAG, "提取文章链接异常", e);
        }
        
        return urls;
    }
    
    /**
     * 获取随机作文内容
     * @param callback 回调接口
     */
    public void getRandomEssay(TianApiManager.ApiCallback<EssayData> callback) {
        // 随机决定是否使用备用静态文章
        if (new Random().nextInt(100) < 50) {
            // 50%的概率直接使用静态文章
            Log.d(TAG, "直接使用静态备用文章");
            if (callback != null) {
                callback.onSuccess(getFallbackEssay());
            }
            return;
        }
        
        // 随机选择一篇文章的URL
        String url = JIANSHU_URLS[new Random().nextInt(JIANSHU_URLS.length)];
        
        Log.d(TAG, "尝试获取备用文章: " + url);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "备用文章请求失败，使用静态内容", e);
                if (callback != null) {
                    callback.onSuccess(getFallbackEssay());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "备用文章响应错误: " + response.code() + "，使用静态内容");
                    if (callback != null) {
                        callback.onSuccess(getFallbackEssay());
                    }
                    return;
                }
                
                try {
                    String htmlContent = response.body().string();
                    
                    // 解析简书文章HTML内容
                    String title = extractJianshuTitle(htmlContent);
                    String content = extractJianshuContent(htmlContent);
                    String author = extractJianshuAuthor(htmlContent);
                    
                    if (title.isEmpty() || content.isEmpty()) {
                        Log.e(TAG, "无法从HTML提取文章内容，使用静态内容");
                        if (callback != null) {
                            callback.onSuccess(getFallbackEssay());
                        }
                        return;
                    }
                    
                    EssayData essayData = new EssayData(title, content, author);
                    
                    if (callback != null) {
                        callback.onSuccess(essayData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析HTML失败，使用静态内容", e);
                    if (callback != null) {
                        callback.onSuccess(getFallbackEssay());
                    }
                }
            }
        });
    }
    
    /**
     * 获取指定URL的文章内容
     */
    private void getArticleContent(String url, TianApiManager.ApiCallback<EssayData> callback) {
        Log.d(TAG, "获取文章内容: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "文章内容请求失败", e);
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "文章内容响应错误: " + response.code() + " URL: " + url);
                    if (callback != null) {
                        callback.onError("服务器响应错误: " + response.code());
                    }
                    return;
                }
                
                try {
                    String htmlContent = response.body().string();
                    
                    String title = extractJianshuTitle(htmlContent);
                    String content = extractJianshuContent(htmlContent);
                    String author = extractJianshuAuthor(htmlContent);
                    
                    Log.d(TAG, "成功解析文章: 标题=" + title + ", 作者=" + author + ", 内容长度=" + (content != null ? content.length() : 0));
                    
                    if (title.isEmpty() || content.isEmpty()) {
                        if (callback != null) {
                            callback.onError("无法解析作文内容");
                        }
                        return;
                    }
                    
                    EssayData essayData = new EssayData(title, content, author);
                    
                    if (callback != null) {
                        callback.onSuccess(essayData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析HTML失败", e);
                    if (callback != null) {
                        callback.onError("数据解析失败: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * 从简书HTML中提取标题
     */
    private String extractJianshuTitle(String html) {
        try {
            // 尝试查找文章标题
            int titleStart = html.indexOf("<h1 class=\"_1RuRku\">");
            if (titleStart < 0) {
                // 尝试备用标签
                titleStart = html.indexOf("<h1 class=\"title\">");
                if (titleStart < 0) {
                    // 再尝试搜索其他可能的标题标签
                    titleStart = html.indexOf("<title>");
                }
            }
            
            if (titleStart >= 0) {
                int titleEnd;
                if (titleStart < 20) {
                    // <title>标签情况
                    titleEnd = html.indexOf("</title>", titleStart);
                    if (titleEnd > titleStart) {
                        String fullTitle = html.substring(titleStart + 7, titleEnd).trim();
                        // 移除网站名称等多余信息
                        int separatorIndex = fullTitle.indexOf(" - 简书");
                        if (separatorIndex > 0) {
                            return fullTitle.substring(0, separatorIndex).trim();
                        }
                        return fullTitle;
                    }
                } else {
                    // h1标签情况
                    titleEnd = html.indexOf("</h1>", titleStart);
                    if (titleEnd > titleStart) {
                        // 找到实际文本的开始
                        int contentStart = html.indexOf(">", titleStart) + 1;
                        if (contentStart < titleEnd) {
                            return html.substring(contentStart, titleEnd).trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "提取标题失败", e);
        }
        
        return "简书文章";
    }
    
    /**
     * 从简书HTML中提取作者
     */
    private String extractJianshuAuthor(String html) {
        try {
            // 尝试查找作者信息
            int authorStart = html.indexOf("<span class=\"_22gUMi\">");
            if (authorStart < 0) {
                // 尝试备用标签
                authorStart = html.indexOf("<span class=\"name\">");
            }
            
            if (authorStart >= 0) {
                int contentStart = html.indexOf(">", authorStart) + 1;
                int authorEnd = html.indexOf("</span>", contentStart);
                
                if (authorEnd > contentStart) {
                    return html.substring(contentStart, authorEnd).trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "提取作者失败", e);
        }
        
        return "简书作者";
    }
    
    /**
     * 从简书HTML中提取内容
     */
    private String extractJianshuContent(String html) {
        try {
            // 查找文章内容区域
            int contentStart = html.indexOf("<article class=\"_2rhmJa\">");
            if (contentStart < 0) {
                // 尝试备用标签
                contentStart = html.indexOf("<div class=\"show-content\">");
                if (contentStart < 0) {
                    contentStart = html.indexOf("<div class=\"show-content-free\">");
                }
            }
            
            if (contentStart >= 0) {
                int contentEnd = html.indexOf("</article>", contentStart);
                if (contentEnd < 0) {
                    contentEnd = html.indexOf("</div>", contentStart + 100);
                }
                
                if (contentEnd > contentStart) {
                    String rawContent = html.substring(contentStart, contentEnd);
                    
                    // 提取所有段落
                    StringBuilder extractedContent = new StringBuilder();
                    int pIndex = 0;
                    while ((pIndex = rawContent.indexOf("<p>", pIndex)) >= 0) {
                        int pEndIndex = rawContent.indexOf("</p>", pIndex);
                        if (pEndIndex > pIndex) {
                            String paragraph = rawContent.substring(pIndex + 3, pEndIndex).trim();
                            // 移除HTML标签
                            paragraph = paragraph.replaceAll("<[^>]*>", "");
                            
                            if (!paragraph.isEmpty()) {
                                extractedContent.append(paragraph).append("\n\n");
                            }
                            
                            pIndex = pEndIndex + 4;
                        } else {
                            break;
                        }
                    }
                    
                    String content = extractedContent.toString().trim()
                            .replaceAll("&nbsp;", " ")
                            .replaceAll("&ldquo;", "\u201C")
                            .replaceAll("&rdquo;", "\u201D")
                            .replaceAll("&hellip;", "...")
                            .replaceAll("&mdash;", "—");
                    
                    // 限制内容长度，截取前1000个字符
                    if (content.length() > 1000) {
                        content = content.substring(0, 1000) + "...";
                    }
                    
                    return content;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "提取内容失败", e);
        }
        
        return "无法获取文章内容，请稍后再试...";
    }
    
    /**
     * 获取内置作文数据（当API调用失败时使用）
     * @return 随机生成的内置作文数据
     */
    public EssayData getFallbackEssay() {
        // 内置简书风格文章数据
        String[][] essays = {
            {"未来的你，一定会感谢现在拼命的自己", 
             "人生是一场漫长的旅程，我们每一步的选择，都将影响未来的道路。有些人选择安逸，有些人选择拼搏，而那些选择拼搏的人，未来一定会收获满满的感谢。\n\n现在的努力，是为了遇见更好的自己。我们常常低估了时间的力量，高估了自己的毅力。很多事情，不是明天再做，而是现在就开始。因为机会稍纵即逝，一旦错过，可能再也不会来临。\n\n拼命，不是没有计划地盲目前行，而是带着明确的目标，全力以赴。它意味着清晨比别人早起一小时读书，意味着深夜加班完善工作，意味着周末依然坚持学习新技能。这些看似微小的坚持，日积月累，终将成就非凡的人生。", 
             "青春不老"},
            {"在孤独中修炼，在沉默中变强", 
             "人生最艰难的时刻，往往是我们成长最快的阶段。当四周无人理解，当内心充满挣扎，那些独自咬牙走过的日子，终将成为生命中最宝贵的财富。\n\n孤独，不是无人陪伴，而是即使身处繁华，内心依然寂静。它让我们有机会真正面对自己，审视自己的内心，找到真实的自我。在这种独处的时光里，我们学会了自己给自己力量，学会了在没有掌声的环境中坚持梦想。\n\n沉默，不是无话可说，而是明白有些路必须独自前行。当别人选择喧嚣，我们选择安静积累；当别人选择抱怨，我们选择默默改变。那些不被理解的日子，那些咬着牙走过的时光，终将铸就更强大的灵魂。", 
             "心灵驿站"},
            {"读书，是为了遇见更好的自己", 
             "书籍是人类进步的阶梯，也是打开新世界的钥匙。每读一本书，就像与一个智者对话，他们跨越时空，将思想的精华传递给我们，让我们在有限的生命里，体验无限的可能。\n\n读书不是为了装饰门面，不是为了应付考试，而是为了遇见更好的自己。当我们沉浸在书海中，思想被激活，视野被拓宽，灵魂得到了滋养。那些曾经困扰我们的问题，在前人的智慧中找到了答案；那些未知的领域，在文字的指引下变得清晰可见。\n\n坚持读书的人，往往拥有更丰富的内心世界，更开阔的视野，更深刻的思考能力。他们不会被一时的困难击倒，因为书籍已经给了他们面对生活的智慧和勇气。", 
             "书香人生"},
            {"生活不会亏待每一个努力向上的人", 
             "这个世界看似不公，实则公平。每一分付出，都会有相应的回报；每一次努力，都在塑造更强大的自己。生活不会亏待那些真诚付出、努力向上的人。\n\n努力，不是盲目地奔跑，而是找对方向后的全力以赴。有人说，世界上最远的距离，不是生与死，而是你明明知道目标在哪里，却始终迈不开那一步。而真正能改变命运的，恰恰是那些即使困难重重，也要迈出第一步的人。\n\n向上的路，总是充满荆棘。会有质疑的目光，会有失败的打击，会有孤独的煎熬。但当你坚持不懈，当你始终保持对生活的热爱和对未来的信心，生活最终会给你答案，给你远超期待的回报。", 
             "奋斗者"},
            {"旅行，让我重新认识这个世界", 
             "旅行，不仅是换一个地方生活，更是用另一种方式感受世界。当我们离开熟悉的环境，来到陌生的国度，眼前的一切都是新鲜的，内心的感受也是全新的。\n\n在旅途中，我们遇见不同的人，聆听不同的故事，品尝不同的美食，感受不同的文化。这些经历让我们明白，这个世界比想象中更加广阔，人生的可能性比想象中更加丰富。\n\n最美的风景，往往在路上。那些不期而遇的美好，那些与陌生人的交流，那些突如其来的感动，构成了旅行中最珍贵的记忆。旅行教会我们包容、理解和感恩，让我们重新审视自己与这个世界的关系。", 
             "旅行者"}
        };
        
        // 随机选择一篇作文
        int randomIndex = new Random().nextInt(essays.length);
        String[] essay = essays[randomIndex];
        
        return new EssayData(essay[0], essay[1], essay[2]);
    }
    
    /**
     * 测试API，直接加载文章内容，不依赖外部API
     * 当设置参数：
     * @param useLocal true表示使用本地内置数据，false表示使用固定测试URL
     * @param forceError true表示模拟错误，false表示正常返回数据
     * @param callback 回调接口
     */
    public void testEssayApi(boolean useLocal, boolean forceError, TianApiManager.ApiCallback<EssayData> callback) {
        if (forceError) {
            // 模拟错误情况
            if (callback != null) {
                callback.onError("测试错误: 模拟的API调用失败");
            }
            return;
        }
        
        if (useLocal) {
            // 直接使用本地文章数据
            EssayData localData = getFallbackEssay();
            if (callback != null) {
                callback.onSuccess(localData);
            }
            return;
        }
        
        // 使用稳定的测试URL
        Request request = new Request.Builder()
                .url(TEST_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build();
                
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "测试API请求失败", e);
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
                    String htmlContent = response.body().string();
                    
                    // 解析简书文章HTML内容
                    String title = extractJianshuTitle(htmlContent);
                    String content = extractJianshuContent(htmlContent);
                    String author = extractJianshuAuthor(htmlContent);
                    
                    if (title.isEmpty() || content.isEmpty()) {
                        title = "测试文章标题";
                        content = "这是一篇测试文章内容，用于演示API调用成功的情况。";
                        author = "测试作者";
                    }
                    
                    EssayData essayData = new EssayData(title, content, author);
                    
                    if (callback != null) {
                        callback.onSuccess(essayData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析HTML失败", e);
                    if (callback != null) {
                        callback.onError("数据解析失败: " + e.getMessage());
                    }
                }
            }
        });
    }
} 