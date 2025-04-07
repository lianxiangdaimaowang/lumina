package com.lianxiangdaimaowang.lumina.ocr;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * OCR图像识别界面，用于拍照和选择图片进行文字识别
 */
public class OcrCaptureActivity extends AppCompatActivity {
    private static final String TAG = "OcrCaptureActivity";
    
    // 请求码
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int REQUEST_GALLERY = 103;
    
    // UI组件
    private ProgressBar progressBar;
    private TextView resultText;
    private TextView statusText;
    private Button btnCamera;
    private Button btnGallery;
    private ImageButton btnDone;
    
    // 临时拍照文件
    private File photoFile;
    private String recognizedText = "";
    private Uri currentPhotoUri;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_capture);
        
        setupToolbar();
        setupViews();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ocr_capture_title);
        }
    }
    
    private void setupViews() {
        progressBar = findViewById(R.id.progress_bar);
        resultText = findViewById(R.id.text_result);
        statusText = findViewById(R.id.status_text);
        btnCamera = findViewById(R.id.btn_camera);
        btnGallery = findViewById(R.id.btn_gallery);
        btnDone = findViewById(R.id.btn_done);
        
        btnCamera.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
        btnGallery.setOnClickListener(v -> checkStoragePermissionAndOpenGallery());
        btnDone.setOnClickListener(v -> finishWithResult());
        
        // 显示初始提示
        resultText.setText(R.string.ocr_initial_hint);
        btnDone.setVisibility(View.GONE);
        
        // 默认情况下隐藏状态文本
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }
    }
    
    private void checkCameraPermissionAndOpenCamera() {
        String[] permissions = {Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }
        
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (allPermissionsGranted) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSION);
        }
    }
    
    private void checkStoragePermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
            Log.d(TAG, "正在请求Android 13+媒体权限: READ_MEDIA_IMAGES");
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            Log.d(TAG, "正在请求存储权限: READ_EXTERNAL_STORAGE");
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "未获得权限，发起权限请求: " + permission);
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_STORAGE_PERMISSION);
        } else {
            Log.d(TAG, "已获得权限，直接打开相册");
            openGallery();
        }
    }
    
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        // 创建临时文件用于保存照片
        try {
            photoFile = createImageFile();
            Log.d(TAG, "创建临时图片文件: " + photoFile.getAbsolutePath());
        } catch (IOException ex) {
            Log.e(TAG, "创建图片文件失败", ex);
            Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
            if (statusText != null) {
                statusText.setText(R.string.ocr_image_create_failed);
                statusText.setVisibility(View.VISIBLE);
            }
            return;
        }
        
        // 确保文件创建成功
        if (photoFile != null) {
            try {
                String authority = getApplicationContext().getPackageName() + ".fileprovider";
                // 使用FileProvider获取URI
                Uri photoURI = FileProvider.getUriForFile(this, authority, photoFile);
                Log.d(TAG, "创建图片URI成功: " + photoURI + ", Authority: " + authority);
                
                    currentPhotoUri = photoURI;
                
                // 将URI传递给相机应用
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                
                // 授予写入URI的权限
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                // 直接启动相机意图
                try {
                    Log.d(TAG, "启动相机应用...");
                    startActivityForResult(takePictureIntent, REQUEST_CAMERA);
                    if (statusText != null) {
                        statusText.setVisibility(View.GONE);
                    }
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "没有找到相机应用", e);
                    Toast.makeText(this, "没有找到相机应用", Toast.LENGTH_SHORT).show();
                    if (statusText != null) {
                        statusText.setText(R.string.ocr_no_app_found);
                        statusText.setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "配置相机Intent时出错", e);
                Toast.makeText(this, "打开相机失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                if (statusText != null) {
                    statusText.setText(getString(R.string.ocr_camera_error, e.getMessage()));
                    statusText.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    /**
     * 创建保存拍照图片的临时文件
     */
    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        
        // 获取应用私有的外部存储目录
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        // 创建临时文件
        File image = File.createTempFile(
                imageFileName,  // 前缀
                ".jpg",         // 后缀
                storageDir      // 目录
        );
        
        return image;
    }
    
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
        startActivityForResult(intent, REQUEST_GALLERY);
            if (statusText != null) {
                statusText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "打开图库失败", e);
            Toast.makeText(this, "打开图库失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (statusText != null) {
                statusText.setText(getString(R.string.ocr_gallery_error, e.getMessage()));
                statusText.setVisibility(View.VISIBLE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                openCamera();
            } else {
                Toast.makeText(this, R.string.ocr_camera_permission_denied, Toast.LENGTH_SHORT).show();
                if (statusText != null) {
                    statusText.setText(R.string.ocr_camera_permission_denied);
                    statusText.setVisibility(View.VISIBLE);
                }
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, R.string.ocr_storage_permission_denied, Toast.LENGTH_SHORT).show();
                if (statusText != null) {
                    statusText.setText(R.string.ocr_storage_permission_denied);
                    statusText.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                // 拍照成功
                Log.d(TAG, "相机返回成功，开始处理照片");
                
                // 直接使用photoFile进行处理，不依赖URI
                if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                    Log.d(TAG, "使用文件路径处理拍照结果: " + photoFile.getAbsolutePath() + ", 大小: " + photoFile.length() + "字节");
                    Toast.makeText(this, "正在处理照片...", Toast.LENGTH_SHORT).show();
                    if (statusText != null) {
                        statusText.setVisibility(View.GONE);
                    }
                    
                    // 直接从文件识别
                    processImageFile(photoFile);
                } else {
                    // 文件不存在，尝试使用URI
                    Log.w(TAG, "照片文件无效或不存在: " + (photoFile != null ? photoFile.getAbsolutePath() : "null"));
                    
                if (currentPhotoUri != null) {
                        Log.d(TAG, "尝试使用URI方式处理照片: " + currentPhotoUri);
                        performOcr(currentPhotoUri);
                    } else {
                        Log.e(TAG, "无法获取照片文件或URI");
                        Toast.makeText(this, "无法获取照片", Toast.LENGTH_SHORT).show();
                        if (statusText != null) {
                            statusText.setText(R.string.ocr_no_image);
                            statusText.setVisibility(View.VISIBLE);
                        }
                    }
                }
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                // 从相册选择图片成功
                currentPhotoUri = data.getData();
                if (currentPhotoUri != null) {
                    if (statusText != null) {
                        statusText.setVisibility(View.GONE);
                    }
                    performOcr(currentPhotoUri);
                } else {
                    Toast.makeText(this, "无法获取选择的图片", Toast.LENGTH_SHORT).show();
                    if (statusText != null) {
                        statusText.setText(R.string.ocr_no_image_selected);
                        statusText.setVisibility(View.VISIBLE);
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "操作被取消");
            if (statusText != null) {
                statusText.setText(R.string.ocr_operation_cancelled);
                statusText.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "操作失败，结果码: " + resultCode);
            Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show();
            if (statusText != null) {
                statusText.setText(getString(R.string.ocr_operation_failed, resultCode));
                statusText.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * 直接处理图片文件进行OCR识别
     * @param imageFile 图片文件
     */
    private void processImageFile(File imageFile) {
        showProgress(true);
        if (statusText != null) {
            statusText.setText(R.string.ocr_processing);
            statusText.setVisibility(View.VISIBLE);
        }
        
        try {
            Log.d(TAG, "开始处理图片文件: " + imageFile.getAbsolutePath() + ", 大小: " + imageFile.length() + "字节");
            
            // 显示处理图片到界面以便用户确认
            Bitmap bitmap = null;
            try {
                // 尝试使用BitmapFactory直接解码文件，这通常更可靠
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                
                // 如果失败，尝试使用MediaStore方法
                if (bitmap == null) {
                    Log.d(TAG, "使用BitmapFactory解码失败，尝试使用MediaStore方法");
                    bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), Uri.fromFile(imageFile));
                }
                
            if (bitmap != null) {
                    Log.d(TAG, "成功加载图片，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                } else {
                    Log.e(TAG, "无法加载图片，bitmap为null");
                    Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show();
                    showProgress(false);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "预览图片失败", e);
                Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showProgress(false);
                return;
            }
            
            // 获取百度OCR管理器实例
            final BaiduOcrManager ocrManager = BaiduOcrManager.getInstance(this);
            final Bitmap finalBitmap = bitmap;
            
            // 强制使用百度OCR进行识别，不考虑初始化状态
            Log.d(TAG, "使用百度OCR识别图片，强制使用百度OCR");
            
            // 使用闭包确保异步识别能够正确获取变量
            Runnable recognizeRunnable = () -> {
                ocrManager.recognizeFromBitmap(finalBitmap, new BaiduOcrManager.OcrResultCallback() {
                    @Override
                    public void onSuccess(String text) {
                        runOnUiThread(() -> {
                            if (text != null && !text.isEmpty()) {
                                recognizedText = text;
                                resultText.setText(recognizedText);
                                if (statusText != null) {
                                    statusText.setText(R.string.ocr_success);
                                    statusText.setVisibility(View.VISIBLE);
                                }
                                btnDone.setVisibility(View.VISIBLE);
                            } else {
                                // 没有识别到文字
                                String noTextMessage = "未能识别到任何文字。\n\n" +
                                        "图片大小: " + (finalBitmap != null ? finalBitmap.getWidth() + "x" + finalBitmap.getHeight() + "像素" : "未知") + "\n" +
                                        "图片路径: " + imageFile.getAbsolutePath();
                                resultText.setText(noTextMessage);
                                if (statusText != null) {
                                    statusText.setText(R.string.ocr_no_text_found);
                                    statusText.setVisibility(View.VISIBLE);
                                }
                                btnDone.setVisibility(View.GONE);
                            }
                            showProgress(false);
                        });
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "OCR识别失败: " + error);
                        runOnUiThread(() -> {
                            String errorMessage = "OCR识别失败: " + error + "\n\n" +
                                    "图片大小: " + (finalBitmap != null ? finalBitmap.getWidth() + "x" + finalBitmap.getHeight() + "像素" : "未知") + "\n" +
                                    "图片路径: " + imageFile.getAbsolutePath();
                            resultText.setText(errorMessage);
                            if (statusText != null) {
                                statusText.setText(R.string.ocr_recognize_failed);
                                statusText.setVisibility(View.VISIBLE);
                            }
                            btnDone.setVisibility(View.GONE);
                            showProgress(false);
                        });
                    }
                });
            };
            
            // 检查OCR初始化状态，如果未初始化，等待短暂时间后再次尝试
            if (!ocrManager.isInitialized()) {
                Log.w(TAG, "百度OCR未初始化，等待1秒后再次尝试...");
                // 等待1秒后再次尝试，给OCR管理器时间初始化
                new Handler().postDelayed(() -> {
                    if (ocrManager.isInitialized()) {
                        Log.d(TAG, "百度OCR已初始化，继续识别");
                        recognizeRunnable.run();
                    } else {
                        Log.w(TAG, "百度OCR仍未初始化，使用本地OCR");
                        runMlKitOcr(finalBitmap, imageFile);
                    }
                }, 1000);
            } else {
                // 已初始化，直接运行
                recognizeRunnable.run();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理图片文件出错", e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (statusText != null) {
                statusText.setText(getString(R.string.ocr_processing_error, e.getMessage()));
                statusText.setVisibility(View.VISIBLE);
            }
            showProgress(false);
        }
    }
    
    /**
     * 使用ML Kit进行本地OCR识别
     * 此方法为示例，实际应用中需要集成ML Kit SDK
     */
    private void runMlKitOcr(Bitmap bitmap, File imageFile) {
        // 模拟ML Kit处理
        new Thread(() -> {
            try {
                // 模拟处理延迟
                Thread.sleep(1500);
                
                // 模拟ML Kit的OCR结果
                String recognizedTextResult = "ML Kit OCR识别结果\n\n" +
                        "图片大小: " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() + "像素" : "未知") + "\n" +
                        "图片路径: " + imageFile.getAbsolutePath() + "\n\n" +
                        "文本内容将显示在这里。这是一个模拟结果，实际应用中应使用ML Kit Text Recognition API或其他OCR库来识别文字。";
                
                runOnUiThread(() -> {
                    recognizedText = recognizedTextResult;
                    resultText.setText(recognizedText);
                    if (statusText != null) {
                        statusText.setText(R.string.ocr_success);
                        statusText.setVisibility(View.VISIBLE);
                    }
                    btnDone.setVisibility(View.VISIBLE);
                    showProgress(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "ML Kit OCR处理出错", e);
                runOnUiThread(() -> {
                    String errorMessage = "OCR识别失败: " + e.getMessage() + "\n\n" +
                            "图片大小: " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() + "像素" : "未知") + "\n" +
                            "图片路径: " + imageFile.getAbsolutePath();
                    resultText.setText(errorMessage);
                    showProgress(false);
                    if (statusText != null) {
                        statusText.setText(R.string.ocr_recognize_failed);
                        statusText.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }
    
    /**
     * 执行OCR处理
     *
     * @param imageUri 选中图片的Uri
     */
    private void performOcr(Uri imageUri) {
        showProgress(true);
        if (statusText != null) {
            statusText.setText(R.string.ocr_processing);
            statusText.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "开始从相册选择处理OCR: " + imageUri);
        
        try {
            // 获取并显示所选图片
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (bitmap != null) {
                    Log.d(TAG, "从相册加载图片成功，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                }
            } catch (Exception e) {
                Log.e(TAG, "加载所选图片失败", e);
                Toast.makeText(this, "无法加载所选图片: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                showProgress(false);
                return;
            }
            
            // 使用百度OCR管理器进行真实文字识别
            final Bitmap finalBitmap = bitmap;
            final BaiduOcrManager ocrManager = BaiduOcrManager.getInstance(this);
            
            // 强制使用百度OCR进行识别
            Log.d(TAG, "使用百度OCR识别从相册选择的图片，强制使用百度OCR");
            
            // 使用闭包确保异步识别能够正确获取变量
            Runnable recognizeRunnable = () -> {
                ocrManager.recognizeFromBitmap(finalBitmap, new BaiduOcrManager.OcrResultCallback() {
                    @Override
                    public void onSuccess(String text) {
                        runOnUiThread(() -> {
                            if (text != null && !text.isEmpty()) {
                                recognizedText = text;
                                resultText.setText(recognizedText);
                                if (statusText != null) {
                                    statusText.setText(R.string.ocr_success);
                                    statusText.setVisibility(View.VISIBLE);
                                }
                                btnDone.setVisibility(View.VISIBLE);
                            } else {
                                // 没有识别到文字
                                String noTextMessage = "未能识别到任何文字。\n\n" +
                                        "图片大小: " + (finalBitmap != null ? finalBitmap.getWidth() + "x" + finalBitmap.getHeight() + "像素" : "未知") + "\n" +
                                        "图片来源: 从相册选择";
                                resultText.setText(noTextMessage);
                                if (statusText != null) {
                                    statusText.setText(R.string.ocr_no_text_found);
                                    statusText.setVisibility(View.VISIBLE);
                                }
                                btnDone.setVisibility(View.GONE);
                            }
                            showProgress(false);
                        });
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "OCR识别失败: " + error);
                        runOnUiThread(() -> {
                            String errorMessage = "OCR识别失败: " + error + "\n\n" +
                                    "图片大小: " + (finalBitmap != null ? finalBitmap.getWidth() + "x" + finalBitmap.getHeight() + "像素" : "未知") + "\n" +
                                    "图片来源: 从相册选择";
                            resultText.setText(errorMessage);
                            if (statusText != null) {
                                statusText.setText(R.string.ocr_recognize_failed);
                                statusText.setVisibility(View.VISIBLE);
                            }
                            btnDone.setVisibility(View.GONE);
                            showProgress(false);
                        });
                    }
                });
            };
            
            // 检查OCR初始化状态，如果未初始化，等待短暂时间后再次尝试
            if (!ocrManager.isInitialized()) {
                Log.w(TAG, "百度OCR未初始化，等待1秒后再次尝试...");
                // 等待1秒后再次尝试，给OCR管理器时间初始化
                new Handler().postDelayed(() -> {
                    if (ocrManager.isInitialized()) {
                        Log.d(TAG, "百度OCR已初始化，继续识别");
                        recognizeRunnable.run();
                    } else {
                        Log.w(TAG, "百度OCR仍未初始化，使用本地OCR");
                        runMlKitOcrFromUri(finalBitmap, imageUri);
                    }
                }, 1000);
            } else {
                // 已初始化，直接运行
                recognizeRunnable.run();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理相册选择的图片时出错", e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (statusText != null) {
                statusText.setText(getString(R.string.ocr_processing_error, e.getMessage()));
                statusText.setVisibility(View.VISIBLE);
            }
            showProgress(false);
        }
    }
    
    /**
     * 使用ML Kit处理从相册选择的图片
     * 此方法为示例，实际应用中需要集成ML Kit SDK
     */
    private void runMlKitOcrFromUri(Bitmap bitmap, Uri imageUri) {
        // 模拟ML Kit处理
        new Thread(() -> {
            try {
                // 模拟处理延迟
                Thread.sleep(1500);
                
                // 模拟ML Kit的OCR结果
                String recognizedTextResult = "ML Kit OCR识别结果\n\n" +
                        "图片大小: " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() + "像素" : "未知") + "\n" +
                        "图片来源: 从相册选择\n\n" +
                        "文本内容将显示在这里。这是一个模拟结果，实际应用中应使用ML Kit Text Recognition API或其他OCR库来识别文字。";
                
                runOnUiThread(() -> {
                    recognizedText = recognizedTextResult;
                    resultText.setText(recognizedText);
                    if (statusText != null) {
                        statusText.setText(R.string.ocr_success);
                        statusText.setVisibility(View.VISIBLE);
                    }
                    btnDone.setVisibility(View.VISIBLE);
                    showProgress(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "ML Kit OCR处理相册图片出错", e);
                runOnUiThread(() -> {
                    String errorMessage = "OCR识别失败: " + e.getMessage() + "\n\n" +
                            "图片大小: " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() + "像素" : "未知") + "\n" +
                            "图片来源: 从相册选择";
                    resultText.setText(errorMessage);
                    showProgress(false);
                    if (statusText != null) {
                        statusText.setText(R.string.ocr_recognize_failed);
                        statusText.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCamera.setEnabled(!show);
        btnGallery.setEnabled(!show);
    }
    
    private void finishWithResult() {
        // 获取文字内容
        recognizedText = resultText.getText().toString();
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("ocr_result", recognizedText);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 辅助方法 - 用于格式化字符串
    private String getSetString(int resId, String param) {
        try {
            return getString(resId, param);
        } catch (Exception e) {
            Log.e(TAG, "格式化字符串出错", e);
            return param;
        }
    }
} 