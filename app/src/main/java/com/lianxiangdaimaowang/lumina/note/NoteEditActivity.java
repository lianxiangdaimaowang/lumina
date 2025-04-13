package com.lianxiangdaimaowang.lumina.note;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.BaseActivity;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.database.NoteEntity;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.ocr.OcrCaptureActivity;
import com.lianxiangdaimaowang.lumina.utils.FileUtils;
import com.lianxiangdaimaowang.lumina.voice.VoiceRecognitionActivity;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class NoteEditActivity extends BaseActivity {
    private static final String TAG = "NoteEditActivity";
    private static final int REQUEST_OCR = 1;
    private static final int REQUEST_VOICE = 2;
    private static final int REQUEST_IMAGE_PICK = 3;
    private static final int REQUEST_SELECT_ATTACHMENT = 4;

    private TextInputEditText titleEdit;
    private AutoCompleteTextView subjectEdit;
    private TextInputEditText contentEdit;
    private RecyclerView attachmentsRecycler;
    private NoteEntity currentNote;
    private NoteRepository noteRepository;
    private List<String> attachments;
    private AttachmentsAdapter attachmentsAdapter;
    private FloatingActionButton fabSave;
    private View cardAttachments;
    private LocalDataManager localDataManager;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_note_edit);

            Log.d(TAG, "开始初始化NoteEditActivity");
            
            // 初始化附件列表
            attachments = new ArrayList<>();
            
            // 先完成视图相关的设置
            setupViews();
            
            // 设置适配器
            setupAttachmentsRecycler();
            
            // 获取LocalDataManager实例
            localDataManager = LocalDataManager.getInstance(getApplicationContext());
            
            // 获取SyncManager实例
            syncManager = SyncManager.getInstance(getApplicationContext());
            
            // 然后获取数据库实例
            try {
                Log.d(TAG, "获取NoteRepository实例");
                noteRepository = NoteRepository.getInstance(getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "获取NoteRepository实例失败", e);
                Toast.makeText(this, "无法连接到数据库: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // 即使数据库初始化失败也继续，用户可以编辑但无法保存
            }
            
            // 设置科目下拉列表
            setupSubjectDropdown();
            
            // 加载笔记（如果有）
            loadNote();
            
            // 更新附件区域可见性
            updateAttachmentsVisibility();
            
            Log.d(TAG, "NoteEditActivity初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "onCreate发生异常", e);
            Toast.makeText(this, "初始化页面时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // 严重错误，无法继续，关闭Activity
            finish();
        }
    }

    private void setupViews() {
        try {
            Log.d(TAG, "初始化视图组件");
            
            // 设置工具栏
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(getString(R.string.edit_note));
            }
            
            // 初始化各个UI组件
            titleEdit = findViewById(R.id.title_edit);
            subjectEdit = findViewById(R.id.subject_edit);
            contentEdit = findViewById(R.id.content_edit);
            attachmentsRecycler = findViewById(R.id.recycler_attachments);
            fabSave = findViewById(R.id.fab_save);
            cardAttachments = findViewById(R.id.card_attachments);

            // 设置按钮点击事件
            View btnOcr = findViewById(R.id.btn_ocr);
            View btnVoice = findViewById(R.id.btn_voice);
            View btnAttachment = findViewById(R.id.btn_attachment);
            
            if (btnOcr != null) btnOcr.setOnClickListener(v -> startOcr());
            if (btnVoice != null) btnVoice.setOnClickListener(v -> startVoiceRecognition());
            if (btnAttachment != null) btnAttachment.setOnClickListener(v -> selectAttachment());
            
            // 隐藏浮动保存按钮，使用顶部的保存按钮
            if (fabSave != null) {
                fabSave.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "视图组件初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "setupViews发生异常", e);
            Toast.makeText(this, "初始化控件时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            throw e; // 重新抛出异常，因为没有正确的视图初始化无法继续
        }
    }

    private void selectAttachment() {
        Log.d(TAG, "选择附件");
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            
            // 支持多种文件类型，包括文档、图片、音频等
            String[] mimeTypes = {
                "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/zip", "text/plain", "image/*", "audio/*"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            
            // 设置描述性标题
            intent.putExtra(Intent.EXTRA_TITLE, "选择要附加的文件");
            
            // 确保已初始化附件列表
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            
            try {
                startActivityForResult(intent, REQUEST_SELECT_ATTACHMENT);
            } catch (android.content.ActivityNotFoundException e) {
                Log.e(TAG, "没有应用可以处理文件选择", e);
                Toast.makeText(this, "没有应用可以处理文件选择", Toast.LENGTH_SHORT).show();
                
                // 尝试使用更简单的选择器
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    fallbackIntent.setType("*/*");
                    startActivityForResult(fallbackIntent, REQUEST_SELECT_ATTACHMENT);
                } catch (Exception ex) {
                    Log.e(TAG, "备用文件选择器也失败", ex);
                    Toast.makeText(this, "无法打开文件选择器: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "选择附件失败", e);
            Toast.makeText(this, "选择附件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSubjectDropdown() {
        try {
            Log.d(TAG, "设置科目下拉列表");
            String[] subjects = getResources().getStringArray(R.array.subjects);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, subjects);
            subjectEdit.setAdapter(adapter);
            Log.d(TAG, "科目下拉列表设置完成");
        } catch (Exception e) {
            Log.e(TAG, "设置科目下拉列表失败", e);
            
            // 如果获取科目数组失败，至少提供一个默认的选项
            String[] defaultSubjects = {"数学", "语文", "英语", "物理", "化学", "生物", "历史", "地理", "其他"};
            try {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, defaultSubjects);
                subjectEdit.setAdapter(adapter);
            } catch (Exception innerE) {
                Log.e(TAG, "设置默认科目列表也失败", innerE);
            }
        }
    }

    private void setupAttachmentsRecycler() {
        try {
            Log.d(TAG, "设置附件列表");
            // 确保attachments已初始化
            if (attachments == null) {
                attachments = new ArrayList<>();
            }
            
            // 创建适配器
            attachmentsAdapter = new AttachmentsAdapter(attachments);
            attachmentsAdapter.setOnAttachmentDeleteListener(this::removeAttachment);
            
            // 设置布局管理器和适配器
            if (attachmentsRecycler != null) {
                attachmentsRecycler.setLayoutManager(new LinearLayoutManager(this));
                attachmentsRecycler.setAdapter(attachmentsAdapter);
            } else {
                Log.e(TAG, "附件RecyclerView为null");
            }
            
            Log.d(TAG, "附件列表设置完成");
        } catch (Exception e) {
            Log.e(TAG, "设置附件列表失败", e);
        }
    }

    private void loadNote() {
        try {
            // 获取从intent传递过来的笔记ID
            String noteId = getIntent().getStringExtra("note_id");
            
            // 如果是从其他活动传入的笔记草稿
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.containsKey("note_draft")) {
                Log.d(TAG, "加载笔记草稿");
                Note draft = (Note) extras.getSerializable("note_draft");
                if (draft != null) {
                    // 创建新笔记实体
                    currentNote = new NoteEntity();
                    currentNote.setTitle(draft.getTitle());
                    currentNote.setContent(draft.getContent());
                    
                    // 获取主题
                    String subject = getIntent().getStringExtra("subject");
                    if (subject != null && !subject.isEmpty()) {
                        currentNote.setSubject(subject);
                        Log.d(TAG, "设置笔记科目(来自Intent): " + subject);
                    } else if (draft.getSubject() != null && !draft.getSubject().isEmpty()) {
                        currentNote.setSubject(draft.getSubject());
                        Log.d(TAG, "设置笔记科目(来自Draft): " + draft.getSubject());
                    } else {
                        Log.d(TAG, "笔记科目为空，将在保存时提示用户设置");
                    }
                    
                    currentNote.setLastModifiedDate(new Date());
                    populateFormWithNote();
                    return;
                }
            }
            
            // 正常的笔记编辑流程 - 将noteId转换为long类型
            if (noteId != null && !noteId.isEmpty()) {
                // 先尝试从SyncManager中获取
                loadNoteFromServer(noteId);
            } else {
                // 如果是新笔记，从intent中获取预填值
                String title = getIntent().getStringExtra("title");
                String content = getIntent().getStringExtra("content");
                String subject = getIntent().getStringExtra("subject");
                
                currentNote = new NoteEntity();
                if (title != null) currentNote.setTitle(title);
                if (content != null) currentNote.setContent(content);
                if (subject != null && !subject.isEmpty()) {
                    currentNote.setSubject(subject);
                    Log.d(TAG, "设置新笔记科目: " + subject);
                }
                
                populateFormWithNote();
            }
        } catch (Exception e) {
            Log.e(TAG, "加载笔记时出错", e);
            Toast.makeText(this, "加载笔记时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 启动OCR文字识别
     */
    private void startOcr() {
        try {
            Intent intent = new Intent(this, OcrCaptureActivity.class);
            startActivityForResult(intent, REQUEST_OCR);
        } catch (Exception e) {
            Log.e(TAG, "启动OCR时出错", e);
            Toast.makeText(this, "无法启动OCR功能", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 启动语音识别
     */
    private void startVoiceRecognition() {
        try {
            Intent intent = new Intent(this, VoiceRecognitionActivity.class);
            startActivityForResult(intent, REQUEST_VOICE);
        } catch (Exception e) {
            Log.e(TAG, "启动语音识别时出错", e);
            Toast.makeText(this, "无法启动语音识别功能", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK && data != null) {
                if (requestCode == REQUEST_OCR) {
                    // 处理OCR识别结果
                    String ocrResult = data.getStringExtra("ocr_result");
                    if (ocrResult != null && !ocrResult.isEmpty()) {
                        // 将OCR结果追加到当前内容
                        String currentContent = contentEdit.getText() != null ? 
                                contentEdit.getText().toString() : "";
                        contentEdit.setText(currentContent + "\n" + ocrResult);
                    }
                } else if (requestCode == REQUEST_VOICE) {
                    // 处理语音识别结果
                    String voiceResult = data.getStringExtra("voice_result");
                    if (voiceResult != null && !voiceResult.isEmpty()) {
                        // 将语音识别结果追加到当前内容
                        String currentContent = contentEdit.getText() != null ? 
                                contentEdit.getText().toString() : "";
                        contentEdit.setText(currentContent + "\n" + voiceResult);
                    }
                } else if (requestCode == REQUEST_IMAGE_PICK && data.getData() != null) {
                    // 处理选择的图片添加为附件
                    Uri selectedFileUri = data.getData();
                    handleSelectedAttachment(selectedFileUri);
                } else if (requestCode == REQUEST_SELECT_ATTACHMENT && data != null && data.getData() != null) {
                    // 处理选择的文件添加为附件
                    Uri selectedFileUri = data.getData();
                    handleSelectedAttachment(selectedFileUri);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理活动结果时出错", e);
            Toast.makeText(this, "处理结果时出错", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleSelectedAttachment(Uri selectedFileUri) {
        try {
            Log.d(TAG, "处理选中的附件: " + selectedFileUri);
            if (selectedFileUri == null) {
                Toast.makeText(this, "无法获取文件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 禁用持久权限请求，改为只申请临时权限
            try {
                // 申请临时URI权限
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(selectedFileUri, takeFlags);
            } catch (SecurityException se) {
                // 部分设备可能不支持持久权限，忽略此异常
                Log.w(TAG, "无法获取持久权限，使用临时权限: " + se.getMessage());
            }
            
            // 创建附件目录
            File attachmentsDir = new File(getFilesDir(), "attachments");
            if (!attachmentsDir.exists()) {
                boolean created = attachmentsDir.mkdirs();
                Log.d(TAG, "创建附件目录: " + (created ? "成功" : "失败"));
                
                if (!created) {
                    Toast.makeText(this, "无法创建附件目录", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // 尝试复制文件
            try {
                String filePath = FileUtils.copyUriToPrivateStorage(this, selectedFileUri, "attachments");
                if (filePath != null) {
                    Log.d(TAG, "附件已复制到: " + filePath);
                    
                    // 检查文件是否真的存在
                    File file = new File(filePath);
                    if (file.exists() && file.length() > 0) {
                        // 文件成功复制
                        attachments.add(filePath);
                        attachmentsAdapter.notifyDataSetChanged();
                        updateAttachmentsVisibility();
                        Toast.makeText(this, R.string.attachment_added, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "复制后的文件不存在或大小为0: " + filePath);
                        Toast.makeText(this, "复制文件失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    throw new IOException("文件复制返回null");
                }
            } catch (IOException e) {
                Log.e(TAG, "复制文件失败", e);
                Toast.makeText(this, "添加附件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "没有权限访问此文件", e);
            Toast.makeText(this, "没有权限访问此文件", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "处理附件时发生未知错误", e);
            Toast.makeText(this, "添加附件时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存笔记
     */
    private void saveNote() {
        try {
            Log.d(TAG, "开始保存笔记");
            
            // 验证输入
            String title = titleEdit.getText() != null ? titleEdit.getText().toString().trim() : "";
            String content = contentEdit.getText() != null ? contentEdit.getText().toString().trim() : "";
            String subject = subjectEdit.getText() != null ? subjectEdit.getText().toString().trim() : "";
            
            if (title.isEmpty()) {
                titleEdit.setError(getString(R.string.error_empty_title));
                titleEdit.requestFocus();
                return;
            }
            
            // 显示保存中状态
            showProgress(true, "正在保存到云端...");
            
            // 从Intent中获取笔记ID（如果是编辑已有笔记）
            String noteId = getIntent().getStringExtra("note_id");
            Log.d(TAG, "笔记ID: " + (noteId != null ? noteId : "新笔记"));
            
            Note note;
            if (noteId != null && !noteId.isEmpty()) {
                // 更新已有笔记
                Log.d(TAG, "正在编辑现有笔记，ID: " + noteId);
                note = syncManager.getServerNotes().stream()
                        .filter(n -> noteId.equals(n.getId()))
                        .findFirst()
                        .orElse(new Note());
                note.setId(noteId);
            } else {
                // 创建新笔记
                Log.d(TAG, "正在创建新笔记");
                note = new Note();
            }
            
            // 更新笔记内容
            note.setTitle(title);
            note.setContent(content);
            
            // 确保科目信息被正确设置
            if (subject != null && !subject.isEmpty()) {
                note.setSubject(subject);
                Log.d(TAG, "设置笔记科目: " + subject);
            } else {
                // 如果用户没有选择科目，默认设置为"其他"
                note.setSubject("其他");
                Log.d(TAG, "用户未选择科目，默认设置为: 其他");
            }
            
            note.setLastModifiedDate(new Date());
            note.setAttachmentPaths(attachments);
            
            // 设置用户ID
            String userId = localDataManager.getCurrentUserId();
            if (userId != null && !userId.isEmpty()) {
                note.setUserId(userId);
                Log.d(TAG, "设置笔记用户ID: " + userId);
            } else {
                Log.e(TAG, "警告：当前用户ID为空");
            }
            
            // 保存到云端前，临时将笔记添加到SyncManager的缓存中，以便立即显示
            List<Note> serverNotes = syncManager.getServerNotes();
            if (noteId != null && !noteId.isEmpty()) {
                // 替换现有笔记
                for (int i = 0; i < serverNotes.size(); i++) {
                    if (noteId.equals(serverNotes.get(i).getId())) {
                        serverNotes.set(i, note);
                        Log.d(TAG, "临时更新SyncManager缓存中的笔记");
                        break;
                    }
                }
            } else {
                // 添加新笔记
                serverNotes.add(note);
                Log.d(TAG, "临时添加笔记到SyncManager缓存，当前缓存大小: " + serverNotes.size());
            }
            
            // 保存到云端
            syncManager.saveNote(note, new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "笔记成功保存到云端: " + note.getTitle() + ", ID: " + note.getId() + ", 科目: " + note.getSubject());
                    runOnUiThread(() -> {
                        showProgress(false, null);
                        
                        // 设置成功结果，让列表知道需要强制刷新
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("refresh_notes", true);
                        setResult(RESULT_OK, resultIntent);
                        Log.d(TAG, "已设置返回结果，包含refresh_notes=true标志");
                        
                        finish();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "保存笔记到云端失败: " + errorMessage);
                    
                    // 如果保存失败，从临时缓存中移除
                    if (noteId == null || noteId.isEmpty()) {
                        syncManager.getServerNotes().remove(note);
                        Log.d(TAG, "从临时缓存中移除保存失败的笔记");
                    }
                    
                    runOnUiThread(() -> {
                        showProgress(false, null);
                        new AlertDialog.Builder(NoteEditActivity.this)
                            .setTitle("保存失败")
                            .setMessage("无法保存到云端: " + errorMessage + "\n请检查网络连接并重试。")
                            .setPositiveButton("重试", (dialog, which) -> saveNote())
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "保存笔记时出错", e);
            showProgress(false, null);
            Toast.makeText(this, "保存笔记时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示/隐藏加载状态
     */
    private void showProgress(boolean show, String message) {
        View progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        if (show && message != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void removeAttachment(String attachment) {
        try {
            Log.d(TAG, "删除附件: " + attachment);
            attachments.remove(attachment);
            attachmentsAdapter.notifyDataSetChanged();
            updateAttachmentsVisibility();
            
            // 删除文件
            File file = new File(attachment);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Toast.makeText(this, R.string.attachment_deleted, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "删除附件时出错", e);
        }
    }

    private void updateAttachmentsVisibility() {
        try {
            if (cardAttachments != null) {
                cardAttachments.setVisibility(
                        attachments != null && !attachments.isEmpty() ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新附件区域可见性时出错", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_note_edit, menu);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "创建选项菜单时出错", e);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if (item.getItemId() == android.R.id.home) {
                onBackPressed();
                return true;
            } else if (item.getItemId() == R.id.action_save) {
                saveNote();
                return true;
            }
            return super.onOptionsItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, "处理菜单项点击时出错", e);
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (hasChanges()) {
                // 显示保存确认对话框
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_discard_changes)
                        .setMessage(R.string.confirm_discard_changes)
                        .setPositiveButton(R.string.save, (dialog, which) -> saveNote())
                        .setNegativeButton(R.string.discard, (dialog, which) -> super.onBackPressed())
                        .setNeutralButton(R.string.keep_editing, (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e(TAG, "处理返回按钮点击时出错", e);
            super.onBackPressed();
        }
    }

    private boolean hasChanges() {
        try {
            if (currentNote == null) {
                return !titleEdit.getText().toString().trim().isEmpty() ||
                       !contentEdit.getText().toString().trim().isEmpty() ||
                       !attachments.isEmpty();
            }
            
            return !titleEdit.getText().toString().trim().equals(currentNote.getTitle()) ||
                   !contentEdit.getText().toString().trim().equals(currentNote.getContent()) ||
                   !subjectEdit.getText().toString().trim().equals(currentNote.getSubject()) ||
                   !attachments.equals(currentNote.getAttachments());
        } catch (Exception e) {
            Log.e(TAG, "检查是否有更改时出错", e);
            return false;
        }
    }

    /**
     * 从服务器加载笔记
     */
    private void loadNoteFromServer(String noteId) {
        try {
            Log.d(TAG, "从服务器加载笔记: " + noteId);
            // 显示加载进度
            showProgress(true, "正在获取笔记...");
            
            // 尝试从SyncManager的缓存中获取
            List<Note> serverNotes = syncManager.getServerNotes();
            Optional<Note> serverNote = serverNotes.stream()
                    .filter(n -> noteId.equals(n.getId()))
                    .findFirst();
            
            if (serverNote.isPresent()) {
                Log.d(TAG, "在缓存中找到笔记");
                Note note = serverNote.get();
                
                // 转换为NoteEntity
                currentNote = new NoteEntity();
                // 将String类型的ID转换为Long
                try {
                    currentNote.setId(Long.parseLong(note.getId()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "无法将笔记ID转换为Long: " + note.getId(), e);
                    // 不设置ID，让系统生成新ID
                }
                
                currentNote.setTitle(note.getTitle());
                currentNote.setContent(note.getContent());
                
                // 确保科目被正确设置
                currentNote.setSubject(note.getSubject());
                Log.d(TAG, "从服务器笔记中设置科目: " + note.getSubject());
                
                if (note.getAttachmentPaths() != null) {
                    currentNote.setAttachments(note.getAttachmentPaths());
                }
                
                // 填充表单
                populateFormWithNote();
                showProgress(false, null);
            } else {
                // 缓存中没有找到，尝试从服务器获取
                Log.d(TAG, "缓存中没有找到笔记，尝试从服务器刷新笔记列表");
                
                // 使用fetchNotesFromServer来刷新服务器笔记列表
                syncManager.fetchNotesFromServer(new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "成功从服务器刷新笔记列表");
                        
                        // 重新从刷新后的列表中查找
                        List<Note> refreshedNotes = syncManager.getServerNotes();
                        Note foundNote = null;
                        for (Note note : refreshedNotes) {
                            if (noteId.equals(note.getId())) {
                                foundNote = note;
                                break;
                            }
                        }
                        
                        if (foundNote != null) {
                            final Note note = foundNote; // 需要在Lambda表达式中使用的final变量
                            
                            // 转换为NoteEntity
                            currentNote = new NoteEntity();
                            // 将String类型的ID转换为Long
                            try {
                                if (note.getId() != null) {
                                    currentNote.setId(Long.parseLong(note.getId()));
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "无法将笔记ID转换为Long: " + note.getId(), e);
                                // 不设置ID，让系统生成新ID
                            }
                            
                            currentNote.setTitle(note.getTitle());
                            currentNote.setContent(note.getContent());
                            
                            // 确保科目被正确设置
                            currentNote.setSubject(note.getSubject());
                            Log.d(TAG, "从服务器响应中设置科目: " + note.getSubject());
                            
                            if (note.getAttachmentPaths() != null) {
                                currentNote.setAttachments(note.getAttachmentPaths());
                            }
                            
                            // 在主线程上更新UI
                            runOnUiThread(() -> {
                                populateFormWithNote();
                                showProgress(false, null);
                            });
                        } else {
                            // 在刷新后的列表中也没找到
                            runOnUiThread(() -> {
                                showProgress(false, null);
                                Toast.makeText(NoteEditActivity.this, 
                                        "无法找到笔记，笔记可能已被删除", Toast.LENGTH_LONG).show();
                                
                                // 创建一个新笔记
                                currentNote = new NoteEntity();
                                populateFormWithNote();
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "从服务器获取笔记列表失败: " + errorMessage);
                        
                        runOnUiThread(() -> {
                            showProgress(false, null);
                            Toast.makeText(NoteEditActivity.this, 
                                    "无法获取笔记: " + errorMessage, Toast.LENGTH_LONG).show();
                            
                            // 如果无法获取笔记，可能是网络问题，创建一个空的NoteEntity
                            currentNote = new NoteEntity();
                            populateFormWithNote();
                        });
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "加载服务器笔记时出错", e);
            
            showProgress(false, null);
            Toast.makeText(this, "加载笔记时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // 创建一个新的空笔记，以便用户可以继续
            currentNote = new NoteEntity();
            populateFormWithNote();
        }
    }
    
    /**
     * 使用当前笔记数据填充表单
     */
    private void populateFormWithNote() {
        try {
            Log.d(TAG, "填充表单");
            if (currentNote != null) {
                titleEdit.setText(currentNote.getTitle());
                contentEdit.setText(currentNote.getContent());
                
                // 设置科目
                String subject = currentNote.getSubject();
                if (subject != null && !subject.trim().isEmpty()) {
                    subjectEdit.setText(subject);
                    Log.d(TAG, "填充科目: " + subject);
                } else {
                    Log.d(TAG, "科目为空，不填充");
                    // 不设置默认值，让用户从列表中选择
                }
                
                // 设置附件列表
                if (currentNote.getAttachments() != null) {
                    attachments.clear();
                    attachments.addAll(currentNote.getAttachments());
                    if (attachmentsAdapter != null) {
                        attachmentsAdapter.notifyDataSetChanged();
                    }
                    // 更新附件区域可见性
                    updateAttachmentsVisibility();
                }
            } else {
                Log.e(TAG, "当前笔记对象为null");
            }
        } catch (Exception e) {
            Log.e(TAG, "填充表单时出错", e);
        }
    }
} 