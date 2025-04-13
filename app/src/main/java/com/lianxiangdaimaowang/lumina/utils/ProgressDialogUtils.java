package com.lianxiangdaimaowang.lumina.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.TextView;

import com.lianxiangdaimaowang.lumina.R;

/**
 * 进度对话框工具类，提供显示和隐藏进度对话框的方法
 */
public class ProgressDialogUtils {
    private static Dialog progressDialog;
    private static TextView messageTextView;
    
    /**
     * 显示进度对话框
     * @param context 上下文
     * @param message 要显示的消息
     */
    public static void showProgress(Context context, String message) {
        if (context == null) {
            return;
        }
        
        // 隐藏之前的对话框
        hideProgress();
        
        // 创建新的对话框
        progressDialog = new Dialog(context);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(R.layout.dialog_progress);
        
        // 设置对话框背景透明
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // 设置消息
        messageTextView = progressDialog.findViewById(R.id.tv_progress_message);
        if (messageTextView != null) {
            messageTextView.setText(message);
        }
        
        // 不可取消
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        
        // 显示对话框
        progressDialog.show();
    }
    
    /**
     * 更新进度对话框消息
     * @param message 新的消息内容
     */
    public static void updateProgress(String message) {
        if (progressDialog != null && progressDialog.isShowing() && messageTextView != null) {
            messageTextView.setText(message);
        }
    }
    
    /**
     * 隐藏进度对话框
     */
    public static void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                // 忽略异常
            }
            progressDialog = null;
            messageTextView = null;
        }
    }
    
    /**
     * 创建自定义进度对话框
     * @param context 上下文
     * @param layoutResId 布局资源ID
     * @param message 要显示的消息
     */
    public static Dialog createCustomProgressDialog(Context context, int layoutResId, String message) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // 加载自定义布局
        LayoutInflater inflater = LayoutInflater.from(context);
        dialog.setContentView(inflater.inflate(layoutResId, null));
        
        // 设置对话框背景透明
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // 不可取消
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        
        return dialog;
    }
} 