package com.lianxiangdaimaowang.lumina.note;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 笔记列表适配器
 */
public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteMenuClick(Note note, View view);
    }

    public NotesAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.titleText.setText(note.getTitle());
        
        // 确保科目正确显示
        if (note.getSubject() != null && !note.getSubject().trim().isEmpty()) {
            holder.subjectText.setText(note.getSubject());
            holder.subjectText.setVisibility(View.VISIBLE);
            Log.d("NotesAdapter", "显示科目: " + note.getSubject() + ", 笔记ID: " + note.getId() + ", 标题: " + note.getTitle());
        } else {
            holder.subjectText.setText("未分类");
            holder.subjectText.setVisibility(View.VISIBLE);
            Log.d("NotesAdapter", "笔记没有科目，显示未分类. 笔记ID: " + note.getId() + ", 标题: " + note.getTitle());
        }
        
        // 显示内容的开头部分
        String content = note.getContent();
        if (content != null && !content.isEmpty()) {
            if (content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            holder.contentText.setText(content);
            holder.contentText.setVisibility(View.VISIBLE);
        } else {
            holder.contentText.setVisibility(View.GONE);
        }
        
        // 格式化日期
        if (note.getLastModifiedDate() != null) {
            holder.dateText.setText(dateFormat.format(note.getLastModifiedDate()));
        } else {
            holder.dateText.setText("");
        }
        
        // 设置附件图标
        if (note.getAttachmentPaths() != null && !note.getAttachmentPaths().isEmpty()) {
            holder.attachmentIcon.setVisibility(View.VISIBLE);
        } else {
            holder.attachmentIcon.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });
        
        holder.menuButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteMenuClick(note, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }
    
    /**
     * 获取指定位置的笔记
     */
    public Note getNoteAt(int position) {
        if (position >= 0 && position < notes.size()) {
            return notes.get(position);
        }
        return null;
    }
    
    public void updateNotes(List<Note> newNotes) {
        this.notes = new ArrayList<>(newNotes);
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView subjectText;
        TextView contentText;
        TextView dateText;
        ImageButton menuButton;
        View attachmentIcon;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_title);
            subjectText = itemView.findViewById(R.id.text_subject);
            contentText = itemView.findViewById(R.id.text_content);
            dateText = itemView.findViewById(R.id.text_date);
            menuButton = itemView.findViewById(R.id.button_menu);
            attachmentIcon = itemView.findViewById(R.id.icon_attachment);
        }
    }
} 