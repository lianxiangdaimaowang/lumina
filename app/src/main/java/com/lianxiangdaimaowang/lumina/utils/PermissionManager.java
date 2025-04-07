package com.lianxiangdaimaowang.lumina.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.lianxiangdaimaowang.lumina.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    // 权限请求码
    public static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;
    public static final int REQUEST_STORAGE_PERMISSION = 102;
    public static final int REQUEST_MULTIPLE_PERMISSIONS = 103;

    // 需要的权限
    public static final String CAMERA = Manifest.permission.CAMERA;
    public static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    public static final String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    public static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    /**
     * 检查相机权限
     * @param context 上下文
     * @return 是否已授权
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求相机权限
     * @param activity 活动
     */
    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    /**
     * 请求相机权限（Fragment）
     * @param fragment 片段
     */
    public static void requestCameraPermission(Fragment fragment) {
        fragment.requestPermissions(new String[]{CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    /**
     * 检查录音权限
     * @param context 上下文
     * @return 是否已授权
     */
    public static boolean hasRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 请求录音权限
     * @param activity 活动
     */
    public static void requestRecordAudioPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    /**
     * 请求录音权限（Fragment）
     * @param fragment 片段
     */
    public static void requestRecordAudioPermission(Fragment fragment) {
        fragment.requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    /**
     * 检查存储权限
     * @param context 上下文
     * @return 是否已授权
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不再需要READ_EXTERNAL_STORAGE和WRITE_EXTERNAL_STORAGE权限
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 只需要READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 10及以下 需要READ_EXTERNAL_STORAGE和WRITE_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 请求存储权限
     * @param activity 活动
     */
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要请求权限
            return;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 只需要READ_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(activity, new String[]{READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            // Android 10及以下 需要READ_EXTERNAL_STORAGE和WRITE_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(activity, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    /**
     * 请求存储权限（Fragment）
     * @param fragment 片段
     */
    public static void requestStoragePermission(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要请求权限
            return;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 只需要READ_EXTERNAL_STORAGE
            fragment.requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            // Android 10及以下 需要READ_EXTERNAL_STORAGE和WRITE_EXTERNAL_STORAGE
            fragment.requestPermissions(new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    /**
     * 检查多个权限
     * @param context 上下文
     * @param permissions 需要检查的权限列表
     * @return 未授权的权限列表
     */
    public static List<String> checkMultiplePermissions(Context context, String[] permissions) {
        List<String> notGrantedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermissions.add(permission);
            }
        }
        return notGrantedPermissions;
    }

    /**
     * 请求多个权限
     * @param activity 活动
     * @param permissions 需要请求的权限列表
     */
    public static void requestMultiplePermissions(Activity activity, String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_MULTIPLE_PERMISSIONS);
    }

    /**
     * 请求多个权限（Fragment）
     * @param fragment 片段
     * @param permissions 需要请求的权限列表
     */
    public static void requestMultiplePermissions(Fragment fragment, String[] permissions) {
        fragment.requestPermissions(permissions, REQUEST_MULTIPLE_PERMISSIONS);
    }

    /**
     * 检查所有需要的权限
     * @param context 上下文
     * @return 是否所有权限都已授权
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        return hasCameraPermission(context) && hasRecordAudioPermission(context) && hasStoragePermission(context);
    }

    /**
     * 请求所有需要的权限
     * @param activity 活动
     */
    public static void requestAllRequiredPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        
        if (!hasCameraPermission(activity)) {
            permissions.add(CAMERA);
        }
        
        if (!hasRecordAudioPermission(activity)) {
            permissions.add(RECORD_AUDIO);
        }
        
        if (!hasStoragePermission(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permissions.add(READ_EXTERNAL_STORAGE);
            } else {
                permissions.add(READ_EXTERNAL_STORAGE);
                permissions.add(WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), REQUEST_MULTIPLE_PERMISSIONS);
        }
    }

    /**
     * 请求所有需要的权限（Fragment）
     * @param fragment 片段
     */
    public static void requestAllRequiredPermissions(Fragment fragment) {
        Context context = fragment.getContext();
        if (context == null) return;
        
        List<String> permissions = new ArrayList<>();
        
        if (!hasCameraPermission(context)) {
            permissions.add(CAMERA);
        }
        
        if (!hasRecordAudioPermission(context)) {
            permissions.add(RECORD_AUDIO);
        }
        
        if (!hasStoragePermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permissions.add(READ_EXTERNAL_STORAGE);
            } else {
                permissions.add(READ_EXTERNAL_STORAGE);
                permissions.add(WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissions.isEmpty()) {
            fragment.requestPermissions(permissions.toArray(new String[0]), REQUEST_MULTIPLE_PERMISSIONS);
        }
    }

    /**
     * 显示权限说明
     * @param context 上下文
     * @param permission 被拒绝的权限
     */
    public static void showPermissionExplanation(Context context, String permission) {
        String message = "";
        
        if (permission.equals(CAMERA)) {
            message = context.getString(R.string.permission_camera);
        } else if (permission.equals(RECORD_AUDIO)) {
            message = context.getString(R.string.permission_microphone);
        } else if (permission.equals(READ_EXTERNAL_STORAGE) || permission.equals(WRITE_EXTERNAL_STORAGE)) {
            message = context.getString(R.string.permission_storage);
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 打开应用设置页面，用于用户手动授权
     * @param context 上下文
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    /**
     * 处理权限请求结果
     * @param requestCode 请求码
     * @param permissions 权限
     * @param grantResults 结果
     * @param listener 回调监听器
     */
    public static void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, OnPermissionResultListener listener) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listener.onPermissionGranted(CAMERA);
                } else {
                    listener.onPermissionDenied(CAMERA);
                }
                break;
                
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listener.onPermissionGranted(RECORD_AUDIO);
                } else {
                    listener.onPermissionDenied(RECORD_AUDIO);
                }
                break;
                
            case REQUEST_STORAGE_PERMISSION:
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    listener.onPermissionGranted(READ_EXTERNAL_STORAGE);
                } else {
                    listener.onPermissionDenied(READ_EXTERNAL_STORAGE);
                }
                break;
                
            case REQUEST_MULTIPLE_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        listener.onPermissionGranted(permissions[i]);
                    } else {
                        listener.onPermissionDenied(permissions[i]);
                    }
                }
                break;
        }
    }

    /**
     * 权限结果监听器接口
     */
    public interface OnPermissionResultListener {
        /**
         * 权限被授予
         * @param permission 权限
         */
        void onPermissionGranted(String permission);

        /**
         * 权限被拒绝
         * @param permission 权限
         */
        void onPermissionDenied(String permission);
    }
} 