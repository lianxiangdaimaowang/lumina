package com.lianxiangdaimaowang.lumina.note;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lianxiangdaimaowang.lumina.R;

import java.io.File;
import java.util.List;

/**
 * 附件适配器，用于显示笔记附件列表
 */
public class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentsAdapter.AttachmentViewHolder> {
    private List<String> attachments;
    private OnAttachmentClickListener clickListener;
    private OnAttachmentDeleteListener deleteListener;

    public interface OnAttachmentClickListener {
        void onAttachmentClick(String attachmentPath);
    }
    
    public interface OnAttachmentDeleteListener {
        void onAttachmentDelete(String attachmentPath);
    }

    public AttachmentsAdapter(List<String> attachments) {
        this.attachments = attachments;
    }

    public void setOnAttachmentClickListener(OnAttachmentClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setOnAttachmentDeleteListener(OnAttachmentDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void updateAttachments(List<String> newAttachments) {
        this.attachments.clear();
        this.attachments.addAll(newAttachments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttachmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attachment, parent, false);
        return new AttachmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttachmentViewHolder holder, int position) {
        String attachmentPath = attachments.get(position);
        File file = new File(attachmentPath);
        
        // 设置文件名
        holder.fileName.setText(file.getName());
        
        // 根据文件类型设置不同的图标
        int iconResId = getIconForFile(file.getName());
        holder.attachmentIcon.setImageResource(iconResId);
        
        // 如果是图片类型，尝试使用Glide加载缩略图
        if (isImageFile(file.getName())) {
            Glide.with(holder.itemView.getContext())
                    .load(file)
                    .placeholder(iconResId)
                    .error(iconResId)
                    .centerCrop()
                    .into(holder.attachmentIcon);
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAttachmentClick(attachmentPath);
            }
        });
        
        // 设置删除图标的点击事件
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onAttachmentDelete(attachmentPath);
            }
        });
    }

    private boolean isImageFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".jpg") || 
               lowerCaseName.endsWith(".jpeg") || 
               lowerCaseName.endsWith(".png") || 
               lowerCaseName.endsWith(".gif");
    }
    
    private int getIconForFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        
        if (lowerCaseName.endsWith(".jpg") || 
            lowerCaseName.endsWith(".jpeg") || 
            lowerCaseName.endsWith(".png") || 
            lowerCaseName.endsWith(".gif")) {
            return R.drawable.ic_image;
        } else if (lowerCaseName.endsWith(".pdf")) {
            return R.drawable.ic_pdf;
        } else if (lowerCaseName.endsWith(".doc") || 
                   lowerCaseName.endsWith(".docx")) {
            return R.drawable.ic_document;
        } else if (lowerCaseName.endsWith(".xls") || 
                   lowerCaseName.endsWith(".xlsx")) {
            return R.drawable.ic_spreadsheet;
        } else if (lowerCaseName.endsWith(".txt")) {
            return R.drawable.ic_text;
        } else {
            return R.drawable.ic_attachment;
        }
    }

    @Override
    public int getItemCount() {
        return attachments.size();
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        ImageView attachmentIcon;
        TextView fileName;
        ImageView deleteButton;

        public AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            attachmentIcon = itemView.findViewById(R.id.icon_attachment);
            fileName = itemView.findViewById(R.id.text_filename);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }
} 