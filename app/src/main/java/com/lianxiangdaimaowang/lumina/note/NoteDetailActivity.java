package com.lianxiangdaimaowang.lumina.note;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.BaseActivity;
import com.lianxiangdaimaowang.lumina.database.NoteEntity;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;

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
    
    private NoteRepository noteRepository;
    private NoteEntity note;
    private AttachmentsAdapter attachmentsAdapter;
    private List<String> attachments = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        
        noteRepository = NoteRepository.getInstance(getApplicationContext());
        
        long noteId = getIntent().getLongExtra("note_id", -1);
        if (noteId == -1) {
            Toast.makeText(this, R.string.error_loading, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setupViews();
        loadNote(noteId);
    }
    
    private void setupViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.note_details);
        
        textTitle = findViewById(R.id.text_title);
        textSubject = findViewById(R.id.text_subject);
        textDate = findViewById(R.id.text_date);
        textContent = findViewById(R.id.text_content);
        cardAttachments = findViewById(R.id.card_attachments);
        recyclerAttachments = findViewById(R.id.recycler_attachments);
        fabEdit = findViewById(R.id.fab_edit);
        
        fabEdit.setOnClickListener(v -> editNote());
        
        // 设置附件适配器
        recyclerAttachments.setLayoutManager(new LinearLayoutManager(this));
        attachmentsAdapter = new AttachmentsAdapter(attachments);
        attachmentsAdapter.setOnAttachmentClickListener(this::openAttachment);
        recyclerAttachments.setAdapter(attachmentsAdapter);
    }
    
    private void loadNote(long noteId) {
        noteRepository.getNoteById(noteId, loadedNote -> {
            if (loadedNote != null) {
                runOnUiThread(() -> {
                    note = loadedNote;
                    displayNote();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.note_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    private void displayNote() {
        textTitle.setText(note.getTitle());
        textSubject.setText(note.getSubject());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = dateFormat.format(note.getLastModifiedDate());
        textDate.setText(dateStr);
        
        textContent.setText(note.getContent());
        
        // 如果有附件，显示附件区域
        if (note.getAttachments() != null && !note.getAttachments().isEmpty()) {
            attachments.clear();
            attachments.addAll(note.getAttachments());
            attachmentsAdapter.notifyDataSetChanged();
            cardAttachments.setVisibility(View.VISIBLE);
        } else {
            cardAttachments.setVisibility(View.GONE);
        }
    }
    
    private void editNote() {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.putExtra("note_id", note.getId());
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
            loadNote(note.getId());
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
        } else if (id == R.id.action_share) {
            shareNote();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void shareNote() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
        
        StringBuilder text = new StringBuilder();
        text.append(note.getTitle()).append("\n\n");
        if (!note.getSubject().isEmpty()) {
            text.append(getString(R.string.subject)).append(": ").append(note.getSubject()).append("\n\n");
        }
        text.append(note.getContent());
        
        sendIntent.putExtra(Intent.EXTRA_TEXT, text.toString());
        sendIntent.setType("text/plain");
        
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_note)));
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
        noteRepository.delete(note);
        Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
        finish();
    }
} 