package com.lianxiangdaimaowang.lumina.note;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.BaseActivity;
import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoteDetailActivity extends BaseActivity {
    private static final String TAG = "NoteDetailActivity";
    private static final int REQUEST_EDIT_NOTE = 1;
    
    private Toolbar toolbar;
    private TextView textTitle;
    private TextView textSubject;
    private TextView textDate;
    private TextView textContent;
    private View cardAttachments;
    private RecyclerView recyclerAttachments;
    private FloatingActionButton fabEdit;
    private ProgressBar progressBar;
    
    private SyncManager syncManager;
    private Note note;
    private AttachmentsAdapter attachmentsAdapter;
    private List<String> attachments = new ArrayList<>();
    private String noteId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        
        syncManager = SyncManager.getInstance(getApplicationContext());
        
        noteId = getIntent().getStringExtra("note_id");
        if (noteId == null || noteId.isEmpty()) {
            Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupViews();
        loadNote();
    }
    
    private void setupViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.note_details);
        }
        
        textTitle = findViewById(R.id.text_title);
        textSubject = findViewById(R.id.text_subject);
        textDate = findViewById(R.id.text_date);
        textContent = findViewById(R.id.text_content);
        cardAttachments = findViewById(R.id.card_attachments);
        recyclerAttachments = findViewById(R.id.recycler_attachments);
        fabEdit = findViewById(R.id.fab_edit);
        progressBar = findViewById(R.id.progress_bar);
        
        fabEdit.setOnClickListener(v -> editNote());
        
        // 设置附件适配器
        recyclerAttachments.setLayoutManager(new LinearLayoutManager(this));
        attachmentsAdapter = new AttachmentsAdapter(attachments);
        attachmentsAdapter.setOnAttachmentClickListener(this::openAttachment);
        recyclerAttachments.setAdapter(attachmentsAdapter);
    }
    
    private void loadNote() {
        showLoading(true);
        
        // 从服务器获取的笔记列表中查找指定ID的笔记
        List<Note> serverNotes = syncManager.getServerNotes();
        
        if (serverNotes != null && !serverNotes.isEmpty()) {
            for (Note serverNote : serverNotes) {
                if (noteId.equals(serverNote.getId())) {
                    note = serverNote;
                    break;
                }
            }
        }
        
        if (note != null) {
            // 找到了笔记，显示详情
            displayNote();
            showLoading(false);
        } else {
            // 如果本地缓存没有，尝试从服务器刷新
            syncManager.fetchNotesFromServer(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    // 再次查找笔记
                    List<Note> refreshedNotes = syncManager.getServerNotes();
                    for (Note refreshedNote : refreshedNotes) {
                        if (noteId.equals(refreshedNote.getId())) {
                            note = refreshedNote;
                            break;
                        }
                    }
                    
                    runOnUiThread(() -> {
                        showLoading(false);
                        if (note != null) {
                            displayNote();
                        } else {
                            Toast.makeText(NoteDetailActivity.this, R.string.note_not_found, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(NoteDetailActivity.this, R.string.error_loading, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
        }
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void displayNote() {
        if (note == null) return;
        
        textTitle.setText(note.getTitle());
        textSubject.setText(note.getSubject());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = dateFormat.format(note.getLastModifiedDate());
        textDate.setText(dateStr);
        
        textContent.setText(note.getContent());
        
        // 如果有附件，显示附件区域
        List<String> noteAttachments = note.getAttachmentPaths();
        if (noteAttachments != null && !noteAttachments.isEmpty()) {
            attachments.clear();
            attachments.addAll(noteAttachments);
            attachmentsAdapter.notifyDataSetChanged();
            cardAttachments.setVisibility(View.VISIBLE);
        } else {
            cardAttachments.setVisibility(View.GONE);
        }
    }
    
    private void editNote() {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("note_id", noteId);
        startActivityForResult(intent, REQUEST_EDIT_NOTE);
    }
    
    private void openAttachment(String attachmentPath) {
        // 打开附件
        try {
            File file = new File(attachmentPath);
            if (!file.exists()) {
                Toast.makeText(this, R.string.error_open_attachment, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    file
            );
            
            intent.setDataAndType(fileUri, getMimeType(attachmentPath));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.error_no_app_to_open, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_open_attachment, Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
        
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            default:
                return "*/*";
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_NOTE && resultCode == RESULT_OK) {
            // 笔记被编辑，重新加载
            loadNote();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_detail, menu);
        // 移除编辑按钮，只使用浮动按钮
        MenuItem editItem = menu.findItem(R.id.action_edit);
        if (editItem != null) {
            editItem.setVisible(false);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        } else if (id == R.id.action_share) {
            shareNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void shareNote() {
        if (note != null) {
            String shareText = String.format("%s\n\n%s", note.getTitle(), note.getContent());
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)));
        }
    }
    
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteNote())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void deleteNote() {
        // 显示正在删除
        showLoading(true);
        
        // 直接返回结果，让用户体验更流畅
        Toast.makeText(NoteDetailActivity.this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
        
        // 设置返回结果，确保传递刷新标志
        Intent resultIntent = new Intent();
        resultIntent.putExtra("refresh_notes", true);
        setResult(RESULT_OK, resultIntent);
        
        // 立即结束当前活动，回到笔记列表页面
        finish();
        
        // 在后台执行实际删除操作
        syncManager.deleteNote(noteId, new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                // 操作已经完成，不需要其他UI反馈
                Log.d(TAG, "笔记删除已成功同步到服务器: " + noteId);
            }
            
            @Override
            public void onError(String errorMessage) {
                // 在后台记录错误，但不再向用户显示
                Log.e(TAG, "删除笔记时出错: " + errorMessage);
            }
        });
    }
} 