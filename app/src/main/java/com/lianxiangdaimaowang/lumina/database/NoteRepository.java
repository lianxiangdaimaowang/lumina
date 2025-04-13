package com.lianxiangdaimaowang.lumina.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.lianxiangdaimaowang.lumina.data.LocalDataManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 笔记仓库类，处理笔记数据的业务逻辑
 */
public class NoteRepository {
    private static final String TAG = "NoteRepository";
    private static volatile NoteRepository INSTANCE;
    private final NoteDao noteDao;
    private final LiveData<List<NoteEntity>> allNotes;
    private final ExecutorService executor;
    private final LocalDataManager localDataManager;
    private String currentUserId;

    private NoteRepository(Context context) {
        try {
            NoteDatabase database = NoteDatabase.getInstance(context);
            noteDao = database.noteDao();
            allNotes = noteDao.getAllNotes(); // 保留全部笔记查询，用于管理员或调试
            executor = Executors.newFixedThreadPool(4);
            
            // 获取LocalDataManager实例，用于获取当前用户ID
            localDataManager = LocalDataManager.getInstance(context);
            currentUserId = localDataManager.getCurrentUserId();
            
            Log.d(TAG, "NoteRepository初始化成功，当前用户ID: " + currentUserId);
        } catch (Exception e) {
            Log.e(TAG, "NoteRepository初始化失败", e);
            throw e; // 重新抛出异常以便上层处理
        }
    }

    public static NoteRepository getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "初始化NoteRepository时context为null");
            throw new IllegalArgumentException("Context不能为null");
        }
        
        if (INSTANCE == null) {
            synchronized (NoteRepository.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = new NoteRepository(context.getApplicationContext());
                    } catch (Exception e) {
                        Log.e(TAG, "创建NoteRepository实例失败", e);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }
    
    // 更新当前用户ID，用于切换用户后刷新
    public void updateCurrentUserId() {
        if (localDataManager != null) {
            currentUserId = localDataManager.getCurrentUserId();
            Log.d(TAG, "更新当前用户ID: " + currentUserId);
        }
    }

    // 获取当前用户的所有笔记
    public LiveData<List<NoteEntity>> getAllNotes() {
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return noteDao.getNotesByUserId(currentUserId);
        } else {
            Log.w(TAG, "当前用户ID为空，返回所有笔记");
            return allNotes;
        }
    }

    // 根据ID获取笔记
    public interface NoteCallback {
        void onNoteLoaded(NoteEntity note);
    }

    public void getNoteById(long id, NoteCallback callback) {
        executor.execute(() -> {
            try {
                NoteEntity note = noteDao.getNoteById(id);
                
                // 验证笔记所属用户
                if (note != null && currentUserId != null && !currentUserId.isEmpty()) {
                    // 如果用户ID为空，设置为当前用户（兼容老数据）
                    if (note.getUserId() == null || note.getUserId().isEmpty()) {
                        note.setUserId(currentUserId);
                        noteDao.update(note);
                    }
                    // 如果不是当前用户的笔记，返回null
                    else if (!note.getUserId().equals(currentUserId)) {
                        Log.w(TAG, "尝试访问其他用户的笔记: " + id + ", 用户: " + note.getUserId() + " vs " + currentUserId);
                        note = null;
                    }
                }
                
                if (callback != null) {
                    callback.onNoteLoaded(note);
                }
            } catch (Exception e) {
                Log.e(TAG, "根据ID获取笔记失败: " + id, e);
                if (callback != null) {
                    callback.onNoteLoaded(null);
                }
            }
        });
    }

    // 插入笔记
    public interface InsertCallback {
        void onNoteInserted(long noteId);
    }

    public void insert(NoteEntity note, InsertCallback callback) {
        if (note == null) {
            Log.e(TAG, "尝试插入null笔记");
            if (callback != null) {
                callback.onNoteInserted(-1); // 返回-1表示失败
            }
            return;
        }
        
        // 设置用户ID为当前用户
        if (currentUserId != null && !currentUserId.isEmpty()) {
            note.setUserId(currentUserId);
        } else {
            Log.w(TAG, "插入笔记时无法获取当前用户ID");
        }
        
        executor.execute(() -> {
            try {
                long id = noteDao.insert(note);
                Log.d(TAG, "笔记插入成功，ID: " + id + ", 用户ID: " + note.getUserId());
                if (callback != null) {
                    callback.onNoteInserted(id);
                }
            } catch (Exception e) {
                Log.e(TAG, "插入笔记失败", e);
                if (callback != null) {
                    callback.onNoteInserted(-1); // 返回-1表示失败
                }
            }
        });
    }

    // 更新笔记
    public void update(NoteEntity note) {
        if (note == null) {
            Log.e(TAG, "尝试更新null笔记");
            return;
        }
        
        // 确保笔记有用户ID
        if ((note.getUserId() == null || note.getUserId().isEmpty()) && currentUserId != null) {
            note.setUserId(currentUserId);
        }
        
        // 验证笔记所属用户
        if (currentUserId != null && !currentUserId.isEmpty() && 
                note.getUserId() != null && !note.getUserId().equals(currentUserId)) {
            Log.e(TAG, "尝试更新其他用户的笔记: " + note.getId() + ", 用户: " + note.getUserId() + " vs " + currentUserId);
            return;
        }
        
        executor.execute(() -> {
            try {
                noteDao.update(note);
                Log.d(TAG, "笔记更新成功，ID: " + note.getId() + ", 用户ID: " + note.getUserId());
            } catch (Exception e) {
                Log.e(TAG, "更新笔记失败，ID: " + note.getId(), e);
            }
        });
    }

    // 删除笔记
    public void delete(NoteEntity note) {
        if (note == null) {
            Log.e(TAG, "尝试删除null笔记");
            return;
        }
        
        // 验证笔记所属用户
        if (currentUserId != null && !currentUserId.isEmpty() && 
                note.getUserId() != null && !note.getUserId().equals(currentUserId)) {
            Log.e(TAG, "尝试删除其他用户的笔记: " + note.getId() + ", 用户: " + note.getUserId() + " vs " + currentUserId);
            return;
        }
        
        executor.execute(() -> {
            try {
                noteDao.delete(note);
                Log.d(TAG, "笔记删除成功，ID: " + note.getId());
            } catch (Exception e) {
                Log.e(TAG, "删除笔记失败，ID: " + note.getId(), e);
            }
        });
    }

    // 根据ID删除笔记
    public void deleteById(long id) {
        executor.execute(() -> {
            try {
                // 先获取笔记以验证所属用户
                NoteEntity note = noteDao.getNoteById(id);
                if (note != null) {
                    // 验证笔记所属用户
                    if (currentUserId != null && !currentUserId.isEmpty() && 
                            note.getUserId() != null && !note.getUserId().equals(currentUserId)) {
                        Log.e(TAG, "尝试删除其他用户的笔记: " + id + ", 用户: " + note.getUserId() + " vs " + currentUserId);
                        return;
                    }
                    noteDao.delete(note);
                    Log.d(TAG, "根据ID删除笔记成功，ID: " + id);
                } else {
                    Log.w(TAG, "要删除的笔记不存在: " + id);
                }
            } catch (Exception e) {
                Log.e(TAG, "根据ID删除笔记失败，ID: " + id, e);
            }
        });
    }

    // 删除当前用户的所有笔记
    public void deleteAllNotesByCurrentUser() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "尝试删除所有笔记时用户ID为空");
            return;
        }
        
        executor.execute(() -> {
            try {
                noteDao.deleteAllByUserId(currentUserId);
                Log.d(TAG, "成功删除用户所有笔记，用户ID: " + currentUserId);
            } catch (Exception e) {
                Log.e(TAG, "删除用户所有笔记失败: " + e.getMessage(), e);
            }
        });
    }

    // 搜索笔记
    public LiveData<List<NoteEntity>> searchNotes(String query) {
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return noteDao.searchNotes(currentUserId, query);
        } else {
            return noteDao.searchNotes(query);
        }
    }

    // 根据科目获取笔记
    public LiveData<List<NoteEntity>> getNotesBySubject(String subject) {
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return noteDao.getNotesBySubject(currentUserId, subject);
        } else {
            return noteDao.getNotesBySubject(subject);
        }
    }

    // 获取笔记总数
    public interface CountCallback {
        void onCount(int count);
    }

    // 获取笔记总数
    public void getNotesCount(CountCallback callback) {
        executor.execute(() -> {
            try {
                int count;
                if (currentUserId != null && !currentUserId.isEmpty()) {
                    count = noteDao.getNotesCountByUserId(currentUserId);
                } else {
                    count = noteDao.getNotesCount();
                }
                
                if (callback != null) {
                    callback.onCount(count);
                }
            } catch (Exception e) {
                Log.e(TAG, "获取笔记总数失败", e);
                if (callback != null) {
                    callback.onCount(0);
                }
            }
        });
    }
} 