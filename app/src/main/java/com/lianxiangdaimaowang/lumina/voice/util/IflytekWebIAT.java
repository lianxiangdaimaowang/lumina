package com.lianxiangdaimaowang.lumina.voice.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import android.util.Log;
import android.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 语音听写流式 WebAPI 接口调用示例 接口文档：https://doc.xfyun.cn/rest_api/语音听写（流式版）.html
 * 适配于Android应用
 */
public class IflytekWebIAT extends WebSocketListener {

    private static final String TAG = "IflytekWebIAT";
    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; //中英文，http url不支持解析ws/wss schema
    
    private String appid; // 在控制台-我的应用获取
    private String apiSecret; // 在控制台-我的应用-语音听写（流式版）获取
    private String apiKey; // 在控制台-我的应用-语音听写（流式版）获取
    
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    
    public static final Gson json = new Gson();
    private Decoder decoder = new Decoder();
    
    private WebSocket webSocket;
    private OkHttpClient client;
    private OnResultListener listener;
    private boolean isConnected = false;
    
    // 语音参数设置
    private String language = "zh_cn"; // 默认中文
    private String domain = "iat";
    private String accent = "mandarin"; // 默认普通话
    private boolean enablePunctuation = true; // 默认启用标点
    private boolean enableDynamicCorrection = true; // 默认启用动态修正
    
    /**
     * 结果回调接口
     */
    public interface OnResultListener {
        void onResult(String result, boolean isLast);
        void onError(int errorCode, String errorMessage);
        void onConnected();
        void onDisconnected();
    }
    
    /**
     * 构造函数
     *
     * @param appid 讯飞应用ID
     * @param apiKey 讯飞API Key
     * @param apiSecret 讯飞API Secret
     * @param listener 结果监听器
     */
    public IflytekWebIAT(String appid, String apiKey, String apiSecret, OnResultListener listener) {
        this.appid = appid;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.listener = listener;
        
        this.client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * 设置语音识别参数
     *
     * @param language 语言，如zh_cn（中文）、en_us（英文）
     * @param accent 方言，如mandarin（普通话）
     * @param enablePunctuation 是否启用标点符号
     * @param enableDynamicCorrection 是否启用动态修正
     */
    public void setParams(String language, String accent, boolean enablePunctuation, 
                         boolean enableDynamicCorrection) {
        this.language = language;
        this.accent = accent;
        this.enablePunctuation = enablePunctuation;
        this.enableDynamicCorrection = enableDynamicCorrection;
    }
    
    /**
     * 连接WebSocket
     */
    public void connect() {
        try {
            if (isConnected) {
                disconnect();
            }
            
            // 重置解码器
            decoder.discard();
            
            // 构建鉴权url
            String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
            // 将url中的 schema http://和https://分别替换为ws:// 和 wss://
            String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
            Log.d(TAG, "WebSocket URL: " + url);
            
            // 创建新的OkHttpClient以避免连接复用问题
            OkHttpClient newClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)  // 增加连接超时时间
                .readTimeout(10, TimeUnit.SECONDS)     // 增加读取超时时间
                .writeTimeout(10, TimeUnit.SECONDS)    // 增加写入超时时间
                .retryOnConnectionFailure(true)       // 允许自动重试
                .build();
                
            Request request = new Request.Builder()
                .url(url)
                .build();
                
            webSocket = newClient.newWebSocket(request, this);
            
        } catch (Exception e) {
            Log.e(TAG, "连接WebSocket错误: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError(-1, "连接WebSocket错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "用户主动断开连接");
            webSocket = null;
        }
        isConnected = false;
        if (listener != null) {
            listener.onDisconnected();
        }
    }
    
    /**
     * 发送语音数据
     *
     * @param audioData 音频数据
     * @param status 状态：0-第一帧，1-中间帧，2-最后一帧
     */
    public void sendAudioData(byte[] audioData, int status) {
        if (webSocket == null) {
            Log.e(TAG, "WebSocket未初始化");
            return;
        }
        
        // 如果连接未建立或状态已中断，但又不是第一帧，则先重新连接
        if (!isConnected) {
            if (status != StatusFirstFrame) {
                Log.e(TAG, "WebSocket未连接，无法发送非首帧数据");
                if (listener != null) {
                    listener.onError(10165, "连接未建立或已断开，无法发送音频数据");
                }
                return;
            } else {
                // 如果是第一帧但连接未建立，尝试重新连接
                try {
                    connect();
                    // 给连接一点时间建立
                    Thread.sleep(500);
                } catch (Exception e) {
                    Log.e(TAG, "尝试重新连接失败: " + e.getMessage(), e);
                    return;
                }
            }
        }
        
        try {
            JsonObject frame = new JsonObject();
            
            if (status == StatusFirstFrame) {  // 第一帧需要发送参数
                JsonObject common = new JsonObject();
                JsonObject business = new JsonObject();
                JsonObject data = new JsonObject();
                
                // 填充common
                common.addProperty("app_id", appid);
                
                // 填充business
                business.addProperty("language", language);
                business.addProperty("domain", domain);
                business.addProperty("accent", accent);
                if (enablePunctuation) {
                    business.addProperty("ptt", 1); // 标点符号
                } else {
                    business.addProperty("ptt", 0);
                }
                if (enableDynamicCorrection) {
                    business.addProperty("dwa", "wpgs"); // 动态修正
                }
                
                // 填充data - 确保status=0
                data.addProperty("status", StatusFirstFrame);
                data.addProperty("format", "audio/L16;rate=16000");
                data.addProperty("encoding", "raw");
                if (audioData != null && audioData.length > 0) {
                    data.addProperty("audio", Base64.encodeToString(audioData, Base64.NO_WRAP));
                } else {
                    data.addProperty("audio", "");
                }
                
                // 填充frame
                frame.add("common", common);
                frame.add("business", business);
                frame.add("data", data);
                
                String frameString = frame.toString();
                Log.d(TAG, "发送第一帧数据: " + frameString);
                boolean sent = webSocket.send(frameString);
                if (!sent) {
                    Log.e(TAG, "第一帧数据发送失败");
                    if (listener != null) {
                        listener.onError(10165, "第一帧数据发送失败，可能导致invalid handle错误");
                    }
                }
                
            } else {  // 中间帧或最后一帧
                JsonObject data = new JsonObject();
                data.addProperty("status", status);
                data.addProperty("format", "audio/L16;rate=16000");
                data.addProperty("encoding", "raw");
                if (status == StatusLastFrame) {
                    data.addProperty("audio", "");
                    Log.d(TAG, "发送最后一帧数据");
                } else if (audioData != null && audioData.length > 0) {
                    data.addProperty("audio", Base64.encodeToString(audioData, Base64.NO_WRAP));
                } else {
                    data.addProperty("audio", "");
                }
                frame.add("data", data);
                
                String frameString = frame.toString();
                if (status == StatusLastFrame) {
                    Log.d(TAG, "发送最后一帧数据: " + frameString);
                }
                boolean sent = webSocket.send(frameString);
                if (!sent && status == StatusLastFrame) {
                    Log.e(TAG, "最后一帧数据发送失败");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "发送音频数据错误: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError(-2, "发送音频数据错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 生成鉴权URL
     */
    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").
                append("date: ").append(date).append("\n").
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = android.util.Base64.encodeToString(hexDigits, android.util.Base64.NO_WRAP);
        
        // 确保正确格式化authorization，确保引号没有丢失
        String authorization = String.format(Locale.ENGLISH, 
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", 
                apiKey, "hmac-sha256", "host date request-line", sha);
        
        Log.d(TAG, "Authorization: " + authorization);
        
        // 使用HttpUrl构建URL，这样可以正确处理URL参数
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().
                addQueryParameter("authorization", android.util.Base64.encodeToString(
                        authorization.getBytes(charset), android.util.Base64.NO_WRAP)).
                addQueryParameter("date", date).
                addQueryParameter("host", url.getHost()).
                build();
        
        String authUrl = httpUrl.toString();
        Log.d(TAG, "Auth URL: " + authUrl);
        
        return authUrl;
    }
    
    // WebSocketListener回调方法
    
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d(TAG, "WebSocket连接成功，响应码：" + response.code());
        isConnected = true;
        if (listener != null) {
            listener.onConnected();
        }
        
        // 发送一个空的首帧，确保句柄创建成功
        try {
            JsonObject frame = new JsonObject();
            JsonObject common = new JsonObject();
            JsonObject business = new JsonObject();
            JsonObject data = new JsonObject();
            
            // 填充common
            common.addProperty("app_id", appid);
            
            // 填充business
            business.addProperty("language", language);
            business.addProperty("domain", domain);
            business.addProperty("accent", accent);
            if (enablePunctuation) {
                business.addProperty("ptt", 1); // 标点符号
            } else {
                business.addProperty("ptt", 0);
            }
            if (enableDynamicCorrection) {
                business.addProperty("dwa", "wpgs"); // 动态修正
            }
            
            // 填充data - 确保status=0
            data.addProperty("status", StatusFirstFrame);
            data.addProperty("format", "audio/L16;rate=16000");
            data.addProperty("encoding", "raw");
            data.addProperty("audio", "");
            
            // 填充frame
            frame.add("common", common);
            frame.add("business", business);
            frame.add("data", data);
            
            String frameString = frame.toString();
            Log.d(TAG, "发送初始化帧: " + frameString);
            boolean sent = webSocket.send(frameString);
            if (!sent) {
                Log.e(TAG, "初始化帧发送失败");
            }
        } catch (Exception e) {
            Log.e(TAG, "发送初始化帧失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d(TAG, "收到消息: " + text);
        try {
            ResponseData resp = json.fromJson(text, ResponseData.class);
            
            if (resp != null) {
                if (resp.getCode() != 0) {
                    String errorMsg = "错误码: " + resp.getCode() + ", 错误信息: " + resp.getMessage();
                    Log.e(TAG, errorMsg);
                    if (listener != null) {
                        listener.onError(resp.getCode(), resp.getMessage());
                    }
                    return;
                }
                
                if (resp.getData() != null) {
                    if (resp.getData().getResult() != null) {
                        Text te = resp.getData().getResult().getText();
                        try {
                            decoder.decode(te);
                            String result = decoder.toString();
                            Log.d(TAG, "识别结果: " + result);
                            if (listener != null) {
                                boolean isLast = resp.getData().getStatus() == 2;
                                listener.onResult(result, isLast);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析结果错误: " + e.getMessage(), e);
                        }
                    }
                    
                    if (resp.getData().getStatus() == 2) {
                        // 数据全部返回完毕，可以关闭连接，释放资源
                        Log.d(TAG, "识别完成，最终结果: " + decoder.toString());
                        if (listener != null) {
                            listener.onResult(decoder.toString(), true);
                        }
                        disconnect();
                    }
                }
            } else {
                Log.e(TAG, "收到空响应");
            }
        } catch (Exception e) {
            Log.e(TAG, "处理消息时发生错误: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError(-100, "解析响应数据错误: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        isConnected = false;
        int errorCode = -3;
        String errorMsg;
        if (response != null) {
            errorCode = response.code();
            errorMsg = "连接失败: " + t.getMessage() + "，响应码: " + errorCode;
            Log.e(TAG, errorMsg, t);
            
            // 尝试读取错误响应内容
            try {
                if (response.body() != null) {
                    String errorBody = response.body().string();
                    Log.e(TAG, "错误响应内容: " + errorBody);
                }
            } catch (Exception e) {
                Log.e(TAG, "读取错误响应内容失败", e);
            }
        } else {
            errorMsg = "连接失败: " + t.getMessage();
            Log.e(TAG, errorMsg, t);
        }
        
        if (listener != null) {
            listener.onError(errorCode, errorMsg);
        }
    }
    
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "连接关闭中: code=" + code + ", reason=" + reason);
        webSocket.close(1000, "");
    }
    
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "连接已关闭: code=" + code + ", reason=" + reason);
        isConnected = false;
        if (listener != null) {
            listener.onDisconnected();
        }
    }
    
    /**
     * 响应数据结构体
     */
    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
        
        public int getCode() {
            return code;
        }
        
        public String getMessage() {
            return this.message;
        }
        
        public String getSid() {
            return sid;
        }
        
        public Data getData() {
            return data;
        }
    }
    
    public static class Data {
        private int status;
        private Result result;
        
        public int getStatus() {
            return status;
        }
        
        public Result getResult() {
            return result;
        }
    }
    
    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;
        boolean ls;
        JsonObject vad;
        
        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            for (Ws ws : this.ws) {
                sb.append(ws.cw[0].w);
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad == null ? null : this.vad;
            return text;
        }
    }
    
    public static class Ws {
        Cw[] cw;
        int bg;
        int ed;
    }
    
    public static class Cw {
        int sc;
        String w;
    }
    
    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JsonObject vad;
        
        @Override
        public String toString() {
            return "Text{" +
                    "bg=" + bg +
                    ", ed=" + ed +
                    ", ls=" + ls +
                    ", sn=" + sn +
                    ", text='" + text + '\'' +
                    ", pgs=" + pgs +
                    ", rg=" + Arrays.toString(rg) +
                    ", deleted=" + deleted +
                    ", vad=" + (vad == null ? "null" : vad.toString()) +
                    '}';
        }
    }
    
    /**
     * 解析返回数据
     */
    public static class Decoder {
        private Text[] texts;
        private int defc = 10;
        
        public Decoder() {
            this.texts = new Text[this.defc];
        }
        
        public synchronized void decode(Text text) {
            if (text == null) {
                Log.e(TAG, "解析到空Text对象");
                return;
            }
            
            try {
                if (text.sn >= this.defc) {
                    this.resize();
                }
                
                if ("rpl".equals(text.pgs)) {
                    // 替换前面的部分结果
                    if (text.rg != null && text.rg.length >= 2) {
                        for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                            if (i >= 0 && i < this.texts.length && this.texts[i] != null) {
                                this.texts[i].deleted = true;
                            }
                        }
                    }
                }
                this.texts[text.sn] = text;
            } catch (Exception e) {
                Log.e(TAG, "解码过程发生错误: " + e.getMessage(), e);
            }
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            try {
                for (Text t : this.texts) {
                    if (t != null && !t.deleted && t.text != null) {
                        sb.append(t.text);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "转换文本发生错误: " + e.getMessage(), e);
            }
            return sb.toString();
        }
        
        public void resize() {
            try {
                int oc = this.defc;
                this.defc <<= 1;
                Text[] old = this.texts;
                this.texts = new Text[this.defc];
                for (int i = 0; i < oc; i++) {
                    this.texts[i] = old[i];
                }
                Log.d(TAG, "Decoder大小调整: " + oc + " -> " + this.defc);
            } catch (Exception e) {
                Log.e(TAG, "调整Decoder大小时发生错误: " + e.getMessage(), e);
                // 恢复初始大小
                this.texts = new Text[10];
                this.defc = 10;
            }
        }
        
        public void discard() {
            Log.d(TAG, "重置Decoder");
            this.texts = new Text[this.defc];
        }
    }
} 