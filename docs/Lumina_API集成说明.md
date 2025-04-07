# Lumina API 集成说明

Lumina 应用集成了多个第三方 API 服务，包括百度 OCR 和科大讯飞语音识别。本文档详细说明如何申请和配置这些 API。

## 百度 OCR 文字识别

### 申请步骤

1. 访问[百度 AI 开放平台](https://ai.baidu.com/)
2. 注册并登录百度账号
3. 创建应用并选择"文字识别"能力
4. 获取 API Key 和 Secret Key

### 配置方法

1. 打开 `app/src/main/java/com/lianxiangdaimaowang/lumina/ocr/BaiduOcrManager.java` 文件
2. 修改以下常量：

```java
private static final String API_KEY = "你的百度API Key";
private static final String SECRET_KEY = "你的百度Secret Key";
```

### 接口使用

Lumina 应用主要使用以下 OCR 功能：

- 通用文字识别
- 手写文字识别
- 表格识别

示例代码：

```java
// 初始化OCR管理器
BaiduOcrManager ocrManager = BaiduOcrManager.getInstance(context);

// 调用识别方法
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
```

## 科大讯飞语音识别

### 申请步骤

1. 访问[科大讯飞开放平台](https://www.xfyun.cn/)
2. 注册并登录科大讯飞账号
3. 创建应用并选择"语音识别"能力
4. 获取 App ID、API Key 和 Secret Key

### 配置方法

1. 打开 `app/src/main/java/com/lianxiangdaimaowang/lumina/voice/IflytekVoiceManager.java` 文件
2. 修改以下常量：

```java
private static final String APP_ID = "你的科大讯飞App ID";
private static final String API_KEY = "你的科大讯飞API Key";
private static final String SECRET_KEY = "你的科大讯飞Secret Key";
```

### 接口使用

Lumina 应用主要使用以下语音识别功能：

- 实时语音转写
- 长语音识别

示例代码：

```java
// 初始化语音管理器
IflytekVoiceManager voiceManager = IflytekVoiceManager.getInstance(context);

// 开始录音并识别
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

## API 调用限制

### 百度 OCR

- 免费版：每天500次调用限制
- 付费版：根据套餐不同，有不同的调用限制

### 科大讯飞语音识别

- 免费版：每天500次调用限制
- 付费版：根据套餐不同，有不同的调用限制

## 常见问题

### 百度 OCR

1. **问题**: Token 获取失败
   **解决方案**: 检查 API Key 和 Secret Key 是否正确，网络连接是否正常

2. **问题**: 识别结果不准确
   **解决方案**: 确保图片清晰，尝试调整图片大小或质量

### 科大讯飞语音识别

1. **问题**: 连接超时
   **解决方案**: 检查网络连接，确保 App ID 和 API Key 正确

2. **问题**: 识别结果不准确
   **解决方案**: 确保录音环境安静，减少背景噪音

## 测试账号

为方便开发测试，我们提供以下测试账号（有限额限制，仅供测试使用）：

### 百度 OCR 测试账号//测试

- API Key: `9Gw1oDrQVMbxB7YLUqZIa7Dz`
- Secret Key: `请联系管理员获取`

### 科大讯飞测试账号//测试

- App ID: `5f845ec1`
- API Key: `请联系管理员获取`
- Secret Key: `请联系管理员获取`

**注意**: 请勿在生产环境中使用测试账号，上线前请替换为自己申请的正式账号。 