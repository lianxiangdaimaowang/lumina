# Lumina API 集成说明

## 概述

Lumina应用采用前后端分离架构，包含以下API集成：
- 百度OCR API：用于文字识别
- 科大讯飞语音识别API：用于语音转文字
- Lumina服务器API：用于数据同步和用户管理

## 服务器API

### 基础信息
- 基础URL：`http://localhost:8081`
- API版本：v1
- 认证方式：JWT Token

### 主要端点
- `/` - 首页HTML
- `/api/server/info` - 服务器信息
- `/api/health` - 健康检查
- `/api/auth/*` - 认证相关端点

### 认证
```java
// 请求示例
POST /api/auth/login
Content-Type: application/json

{
    "username": "user@example.com",
    "password": "password"
}

// 响应示例
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expires": 3600
}
```

## 百度OCR API

### 申请配置

1. **注册账号**：
   - 访问[百度AI开放平台](https://ai.baidu.com/)
   - 注册开发者账号
   - 实名认证（推荐企业认证）

2. **创建应用**：
   - 创建文字识别应用
   - 选择通用OCR和手写OCR
   - 获取API Key和Secret Key

3. **配置密钥**：
   ```java
   // 文件：app/src/main/java/com/lianxiangdaimaowang/lumina/ocr/BaiduOcrManager.java
   private static final String API_KEY = "你的百度API Key";
   private static final String SECRET_KEY = "你的百度Secret Key";
   ```

### 使用示例

```java
// 初始化OCR管理器
BaiduOcrManager ocrManager = BaiduOcrManager.getInstance(context);

// 图片文字识别
ocrManager.recognizeText(imagePath, new RecognizeCallback() {
    @Override
    public void onSuccess(String text) {
        // 处理识别结果
    }
    
    @Override
    public void onFailure(String error) {
        // 处理错误
    }
});

// 手写文字识别
ocrManager.recognizeHandwriting(imagePath, new RecognizeCallback() {
    @Override
    public void onSuccess(String text) {
        // 处理识别结果
    }
    
    @Override
    public void onFailure(String error) {
        // 处理错误
    }
});
```

### 性能优化

1. **图片预处理**：
   ```java
   private Bitmap preprocessImage(Bitmap original) {
       // 压缩图片
       int maxSize = 1024;
       int width = original.getWidth();
       int height = original.getHeight();
       float scale = Math.min((float)maxSize/width, (float)maxSize/height);
       
       return Bitmap.createScaledBitmap(original, 
           (int)(width*scale), 
           (int)(height*scale), 
           true);
   }
   ```

2. **并发控制**：
   ```java
   private final ExecutorService executor = Executors.newFixedThreadPool(3);
   private final Queue<RecognizeTask> taskQueue = new ConcurrentLinkedQueue<>();
   ```

3. **缓存管理**：
   ```java
   private LruCache<String, String> resultCache;
   
   private void initCache() {
       int cacheSize = 4 * 1024 * 1024; // 4MB
       resultCache = new LruCache<String, String>(cacheSize);
   }
   ```

## 科大讯飞语音识别

### 申请配置

1. **注册账号**：
   - 访问[科大讯飞开放平台](https://www.xfyun.cn/)
   - 注册开发者账号
   - 完成实名认证

2. **创建应用**：
   - 创建语音识别应用
   - 选择实时语音识别能力
   - 获取AppID和API Key

3. **配置密钥**：
   ```java
   // 文件：app/src/main/java/com/lianxiangdaimaowang/lumina/voice/IflytekVoiceManager.java
   private static final String APP_ID = "你的讯飞AppID";
   private static final String API_KEY = "你的讯飞API Key";
   ```

### 使用示例

```java
// 初始化语音管理器
IflytekVoiceManager voiceManager = IflytekVoiceManager.getInstance(context);

// 开始录音识别
voiceManager.startRecognize(new RecognizeCallback() {
    @Override
    public void onResult(String result, boolean isLast) {
        // 处理识别结果
    }
    
    @Override
    public void onError(int errorCode, String errorMsg) {
        // 处理错误
    }
});

// 停止录音
voiceManager.stopRecognize();
```

### 性能优化

1. **音频处理**：
   ```java
   private void configureAudioParams() {
       // 设置音频参数
       audioParams.put("sampleRate", "16000");
       audioParams.put("channel", "1");
       audioParams.put("encoding", "pcm");
   }
   ```

2. **实时识别优化**：
   ```java
   private void optimizeRecognition() {
       // 设置VAD
       mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
       mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
       
       // 设置语言
       mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
       mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
   }
   ```

## 错误处理

### 通用错误处理
```java
public class ApiException extends Exception {
    private int errorCode;
    private String errorMsg;
    
    public ApiException(int code, String msg) {
        super(msg);
        this.errorCode = code;
        this.errorMsg = msg;
    }
}

public void handleApiError(ApiException e) {
    switch (e.getErrorCode()) {
        case ERROR_NETWORK:
            showNetworkError();
            break;
        case ERROR_AUTH:
            refreshToken();
            break;
        case ERROR_QUOTA:
            showQuotaExceeded();
            break;
        default:
            showGeneralError(e.getMessage());
    }
}
```

### 重试机制
```java
public class RetryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {
    private final int maxRetries;
    private final int retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> apply(Observable<? extends Throwable> attempts) {
        return attempts.flatMap((Function<Throwable, Observable<?>>) throwable -> {
            if (++retryCount <= maxRetries) {
                return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
            }
            return Observable.error(throwable);
        });
    }
}
```

## 安全建议

### 密钥保护
```java
public class SecureKeyStore {
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "LuminaApiKey";
    
    public static void storeKey(String key) {
        // 使用Android Keystore加密存储
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, 
                KEYSTORE_PROVIDER);
            
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();
            
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        }
        
        // 加密API密钥
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_ALIAS, null));
        byte[] encryptedKey = cipher.doFinal(key.getBytes());
        
        // 存储加密后的密钥
        SharedPreferences.Editor editor = context.getSharedPreferences(
            "SecurePrefs", 
            Context.MODE_PRIVATE).edit();
        editor.putString("encrypted_key", Base64.encodeToString(encryptedKey, Base64.DEFAULT));
        editor.apply();
    }
}
```

### 数据传输安全
```java
public class ApiClient {
    private static final String BASE_URL = "https://api.example.com/";
    
    private static OkHttpClient createSecureClient() {
        return new OkHttpClient.Builder()
            .certificatePinner(new CertificatePinner.Builder()
                .add("api.example.com", "sha256/XXXX")
                .build())
            .addInterceptor(new AuthInterceptor())
            .build();
    }
}
```

## 更新日志

### v1.0 (2025-04-07)
- 集成百度OCR API
- 集成科大讯飞语音识别
- 实现服务器API通信
- 添加安全存储机制
- 优化API调用性能 