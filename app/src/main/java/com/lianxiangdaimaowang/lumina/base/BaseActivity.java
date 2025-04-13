package com.lianxiangdaimaowang.lumina.base;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.utils.PermissionCallback;
import com.lianxiangdaimaowang.lumina.utils.PermissionManager;

/**
 * 应用的基础Activity，提供权限管理等通用功能
 */
public abstract class BaseActivity extends AppCompatActivity implements PermissionCallback {

    private static final String TAG = "BaseActivity";
    private static final int REQUEST_PERMISSION_CODE = 1001;

    private String currentRequestPermission;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (permissions.length > 0 && grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted(permissions[0]);
                } else {
                    onPermissionDenied(permissions[0]);
                }
            }
        }
    }

    /**
     * 检查并请求相机权限
     * @return 是否已有权限
     */
    protected boolean checkAndRequestCameraPermission() {
        if (PermissionManager.hasCameraPermission(this)) {
            return true;
        } else {
            PermissionManager.requestCameraPermission(this);
            return false;
        }
    }

    /**
     * 检查并请求录音权限
     * @return 是否已有权限
     */
    protected boolean checkAndRequestRecordAudioPermission() {
        if (PermissionManager.hasRecordAudioPermission(this)) {
            return true;
        } else {
            PermissionManager.requestRecordAudioPermission(this);
            return false;
        }
    }

    /**
     * 检查并请求存储权限
     * @return 是否已有权限
     */
    protected boolean checkAndRequestStoragePermission() {
        if (PermissionManager.hasStoragePermission(this)) {
            return true;
        } else {
            PermissionManager.requestStoragePermission(this);
            return false;
        }
    }

    /**
     * 检查并请求所有需要的权限
     * @return 是否已有所有权限
     */
    protected boolean checkAndRequestAllPermissions() {
        if (PermissionManager.hasAllRequiredPermissions(this)) {
            return true;
        } else {
            PermissionManager.requestAllRequiredPermissions(this);
            return false;
        }
    }

    /**
     * 显示权限被拒绝的解释对话框
     * @param permission 被拒绝的权限
     */
    protected void showPermissionDeniedDialog(String permission) {
        String message = "";

        if (permission.equals(PermissionManager.CAMERA)) {
            message = getString(R.string.permission_camera);
        } else if (permission.equals(PermissionManager.RECORD_AUDIO)) {
            message = getString(R.string.permission_microphone);
        } else if (permission.equals(PermissionManager.READ_EXTERNAL_STORAGE) || 
                permission.equals(PermissionManager.WRITE_EXTERNAL_STORAGE)) {
            message = getString(R.string.permission_storage);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_denied)
                .setMessage(message)
                .setPositiveButton(R.string.settings, (dialog, which) -> PermissionManager.openAppSettings(this))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 当权限被授予时调用
     * @param permission 权限
     */
    @Override
    public void onPermissionGranted(String permission) {
        // 子类可以重写此方法，处理特定权限被授予的情况
    }

    /**
     * 当权限被拒绝时调用
     * @param permission 权限
     */
    @Override
    public void onPermissionDenied(String permission) {
        // 默认显示权限被拒绝的解释对话框
        showPermissionDeniedDialog(permission);
    }

    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示Toast消息
     * @param resId 消息资源ID
     */
    protected void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }
} 