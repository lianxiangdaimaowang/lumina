package com.lianxiangdaimaowang.lumina.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件工具类
 */
public class FileUtils {
    
    private static final String TAG = "FileUtils";
    
    /**
     * 创建临时图片文件
     * @param context 上下文
     * @return 临时图片文件
     * @throws IOException IO异常
     */
    public static File createImageFile(Context context) throws IOException {
        // 创建临时文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
    
    /**
     * 获取文件的MIME类型
     * @param context 上下文
     * @param uri 文件URI
     * @return MIME类型
     */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
    
    /**
     * 获取文件名
     * @param context 上下文
     * @param uri 文件URI
     * @return 文件名
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    /**
     * 将Uri指向的文件复制到应用私有存储中
     *
     * @param context    上下文
     * @param fileUri    源文件Uri
     * @param destFolderName 目标文件夹名称
     * @return 复制后的文件路径，失败返回null
     */
    public static String copyUriToPrivateStorage(Context context, Uri fileUri, String destFolderName) throws IOException {
        Log.d(TAG, "复制文件开始: " + fileUri + " -> " + destFolderName);
        
        if (context == null || fileUri == null || destFolderName == null) {
            Log.e(TAG, "参数无效: context=" + context + ", fileUri=" + fileUri + ", destFolder=" + destFolderName);
            throw new IOException("无效的参数");
        }
        
        // 创建目标目录
        File destDir = new File(context.getFilesDir(), destFolderName);
        if (!destDir.exists()) {
            boolean created = destDir.mkdirs();
            Log.d(TAG, "创建目标目录 " + destDir.getAbsolutePath() + ": " + (created ? "成功" : "失败"));
            if (!created) {
                throw new IOException("无法创建目标目录: " + destDir.getAbsolutePath());
            }
        }
        
        // 检查目录是否可写
        if (!destDir.canWrite()) {
            Log.e(TAG, "目标目录不可写: " + destDir.getAbsolutePath());
            throw new IOException("目标目录不可写: " + destDir.getAbsolutePath());
        }
        
        String fileName = getFileName(context, fileUri);
        if (fileName == null || fileName.isEmpty()) {
            fileName = "file_" + System.currentTimeMillis() + "." + getFileExtension(context, fileUri);
            Log.d(TAG, "无法确定文件名，使用生成的名称: " + fileName);
        }
        
        // 确保文件名不含特殊字符，防止路径错误
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // 避免文件名冲突
        String baseFileName = fileName;
        File destFile = new File(destDir, fileName);
        int counter = 1;
        while (destFile.exists()) {
            String extension = "";
            int dotIndex = baseFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = baseFileName.substring(dotIndex);
                fileName = baseFileName.substring(0, dotIndex) + "_" + counter + extension;
            } else {
                fileName = baseFileName + "_" + counter;
            }
            destFile = new File(destDir, fileName);
            counter++;
        }
        
        Log.d(TAG, "最终文件名: " + fileName);
        
        try (InputStream in = context.getContentResolver().openInputStream(fileUri);
             OutputStream out = new FileOutputStream(destFile)) {
            
            if (in == null) {
                Log.e(TAG, "无法打开输入流: " + fileUri);
                throw new IOException("无法打开文件: " + fileUri);
            }
            
            byte[] buffer = new byte[1024 * 4]; // 4K缓冲区
            int read;
            long totalBytes = 0;
            
            Log.d(TAG, "开始复制数据...");
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalBytes += read;
            }
            out.flush();
            
            Log.d(TAG, "复制完成，共复制 " + totalBytes + " 字节到 " + destFile.getAbsolutePath());
            
            // 验证文件大小
            if (destFile.length() == 0) {
                Log.e(TAG, "复制的文件大小为0: " + destFile.getAbsolutePath());
                throw new IOException("复制后的文件大小为0");
            }
            
            return destFile.getAbsolutePath();
        } catch (SecurityException e) {
            Log.e(TAG, "无权限访问文件: " + fileUri, e);
            throw new IOException("无权限访问文件: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "复制文件时出错: " + fileUri, e);
            throw new IOException("复制文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(Context context, Uri uri) {
        String extension = null;
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            } else {
                String fileName = getFileName(context, uri);
                if (fileName != null) {
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        extension = fileName.substring(dotIndex + 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return extension;
    }
    
    /**
     * 打开文件
     * @param context 上下文
     * @param file 文件
     */
    public static void openFile(Context context, File file) {
        // 获取文件URI
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        
        // 获取文件MIME类型
        String mimeType = getMimeType(context, uri);
        if (mimeType == null) {
            mimeType = "*/*";
        }
        
        // 创建打开文件的Intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 