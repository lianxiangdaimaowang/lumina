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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            Log.d(TAG, "开始加载笔记");
            
            // 如果noteRepository为null，则无法加载数据
            if (noteRepository == null) {
                Log.e(TAG, "无法加载笔记: noteRepository为null");
                return;
            }
            
            long noteId = getIntent().getLongExtra("note_id", -1);
            if (noteId != -1) {
                Log.d(TAG, "加载笔记ID: " + noteId);
                noteRepository.getNoteById(noteId, note -> {
                    if (note != null) {
                        runOnUiThread(() -> {
                            try {
                                Log.d(TAG, "笔记加载成功: " + note.getTitle());
                                currentNote = note;
                                
                                // 设置视图数据
                                if (titleEdit != null) titleEdit.setText(note.getTitle());
                                if (subjectEdit != null) subjectEdit.setText(note.getSubject());
                                if (contentEdit != null) contentEdit.setText(note.getContent());
                                
                                // 加载附件
                                if (note.getAttachments() != null) {
                                    attachments.clear();
                                    attachments.addAll(note.getAttachments());
                                    
                                    // 检查附件中的文件是否存在
                                    List<String> validAttachments = new ArrayList<>();
                                    for (String path : attachments) {
                                        File file = new File(path);
                                        if (file.exists() && file.canRead()) {
                                            validAttachments.add(path);
                                        } else {
                                            Log.w(TAG, "附件文件不存在或无法读取: " + path);
                                        }
                                    }
                                    
                                    // 如果有无效附件，更新列表
                                    if (validAttachments.size() != attachments.size()) {
                                        attachments.clear();
                                        attachments.addAll(validAttachments);
                                    }
                                    
                                    if (attachmentsAdapter != null) {
                                        attachmentsAdapter.notifyDataSetChanged();
                                    }
                                    updateAttachmentsVisibility();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "在UI线程中设置笔记数据时出错", e);
                                Toast.makeText(NoteEditActivity.this, 
                                        "加载笔记数据时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.e(TAG, "无法找到ID为 " + noteId + " 的笔记");
                        runOnUiThread(() -> {
                            Toast.makeText(NoteEditActivity.this, 
                                    "无法找到指定的笔记", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Log.d(TAG, "创建新笔记 (noteId = -1)");
                // 新建笔记不需要加载数据
            }
        } catch (Exception e) {
            Log.e(TAG, "加载笔记时出错", e);
            Toast.makeText(this, "加载笔记数据时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void saveNote() {
        try {
            Log.d(TAG, "开始保存笔记");
            
            // 如果noteRepository为null，则无法保存数据
            if (noteRepository == null) {
                Log.e(TAG, "无法保存笔记: noteRepository为null");
                Toast.makeText(this, "数据库未初始化，无法保存笔记", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 从视图获取数据
            final String title = titleEdit != null && titleEdit.getText() != null ? 
                    titleEdit.getText().toString().trim() : "";
            final String subject = subjectEdit != null && subjectEdit.getText() != null ? 
                    subjectEdit.getText().toString().trim() : "";
            final String content = contentEdit != null && contentEdit.getText() != null ? 
                    contentEdit.getText().toString().trim() : "";

            // 验证标题
            if (title.isEmpty()) {
                if (titleEdit != null) {
                    titleEdit.setError(getString(R.string.error_empty_title));
                }
                Toast.makeText(this, R.string.error_empty_title, Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建或更新笔记对象
            if (currentNote == null) {
                Log.d(TAG, "创建新笔记实例");
                currentNote = new NoteEntity();
                currentNote.setCreationDate(new Date());
            }
            
            Log.d(TAG, "设置笔记属性");
            currentNote.setTitle(title);
            currentNote.setSubject(subject);
            currentNote.setContent(content);
            
            // 验证并设置附件列表
            if (attachments != null) {
                // 检查附件文件是否存在
                List<String> validAttachments = new ArrayList<>();
                for (String path : attachments) {
                    File file = new File(path);
                    if (file.exists() && file.canRead()) {
                        validAttachments.add(path);
                    } else {
                        Log.w(TAG, "保存时跳过不存在的附件: " + path);
                    }
                }
                currentNote.setAttachments(validAttachments);
            } else {
                currentNote.setAttachments(new ArrayList<>());
            }
            currentNote.setLastModifiedDate(new Date());
            
            // 根据ID决定执行插入或更新操作
            if (currentNote.getId() != 0) {
                // 更新现有笔记
                Log.d(TAG, "更新现有笔记 ID: " + currentNote.getId());
                noteRepository.update(currentNote);
                
                // 同时更新LocalDataManager中的笔记
                final Note modelNote = new Note(title, content, subject);
                modelNote.setId(String.valueOf(currentNote.getId()));
                modelNote.setCreatedDate(currentNote.getCreationDate());
                modelNote.setLastModifiedDate(currentNote.getLastModifiedDate());
                
                if (currentNote.getAttachments() != null) {
                    for (String attachment : currentNote.getAttachments()) {
                        modelNote.addAttachmentPath(attachment);
                    }
                }
                
                if (localDataManager != null) {
                    localDataManager.saveNote(modelNote);
                    Log.d(TAG, "笔记同时保存到LocalDataManager, ID: " + modelNote.getId());
                }
                
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                // 创建新笔记
                Log.d(TAG, "插入新笔记");
                // 保存final副本，用于lambda表达式中使用
                final NoteEntity noteToSave = currentNote;
                
                noteRepository.insert(noteToSave, noteId -> {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "新笔记ID: " + noteId);
                            
                            if (noteId > 0) {
                                // 同时保存到LocalDataManager
                                final Note modelNote = new Note(title, content, subject);
                                modelNote.setId(String.valueOf(noteId));
                                modelNote.setCreatedDate(noteToSave.getCreationDate());
                                modelNote.setLastModifiedDate(noteToSave.getLastModifiedDate());
                                
                                if (noteToSave.getAttachments() != null) {
                                    for (String attachment : noteToSave.getAttachments()) {
                                        modelNote.addAttachmentPath(attachment);
                                    }
                                }
                                
                                if (localDataManager != null) {
                                    localDataManager.saveNote(modelNote);
                                    Log.d(TAG, "新笔记同时保存到LocalDataManager, ID: " + modelNote.getId());
                                }
                                
                                Toast.makeText(NoteEditActivity.this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                                // 返回结果给调用方
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("note_id", noteId);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } else {
                                Log.e(TAG, "保存笔记失败: 返回的ID无效 (" + noteId + ")");
                                Toast.makeText(NoteEditActivity.this, 
                                        "保存笔记失败，请重试", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "在UI线程中处理笔记插入回调时出错", e);
                            Toast.makeText(NoteEditActivity.this, 
                                    "保存笔记后处理出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "保存笔记时出错", e);
            Toast.makeText(this, "保存笔记时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
} 