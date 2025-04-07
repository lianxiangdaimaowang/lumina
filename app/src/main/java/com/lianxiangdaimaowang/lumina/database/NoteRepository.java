package com.lianxiangdaimaowang.lumina.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

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

    private NoteRepository(Context context) {
        try {
            NoteDatabase database = NoteDatabase.getInstance(context);
            noteDao = database.noteDao();
            allNotes = noteDao.getAllNotes();
            executor = Executors.newFixedThreadPool(4);
            Log.d(TAG, "NoteRepository初始化成功");
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

    // 获取所有笔记
    public LiveData<List<NoteEntity>> getAllNotes() {
        return allNotes;
    }

    // 根据ID获取笔记
    public interface NoteCallback {
        void onNoteLoaded(NoteEntity note);
    }

    public void getNoteById(long id, NoteCallback callback) {
        executor.execute(() -> {
            try {
                NoteEntity note = noteDao.getNoteById(id);
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
        
        executor.execute(() -> {
            try {
                long id = noteDao.insert(note);
                Log.d(TAG, "笔记插入成功，ID: " + id);
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
        
        executor.execute(() -> {
            try {
                noteDao.update(note);
                Log.d(TAG, "笔记更新成功，ID: " + note.getId());
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
                noteDao.deleteById(id);
                Log.d(TAG, "根据ID删除笔记成功，ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "根据ID删除笔记失败，ID: " + id, e);
            }
        });
    }

    // 搜索笔记
    public LiveData<List<NoteEntity>> searchNotes(String query) {
        return noteDao.searchNotes(query);
    }

    // 根据科目获取笔记
    public LiveData<List<NoteEntity>> getNotesBySubject(String subject) {
        return noteDao.getNotesBySubject(subject);
    }

    // 获取笔记总数
    public interface CountCallback {
        void onCount(int count);
    }

    // 获取笔记总数
    public void getNotesCount(CountCallback callback) {
        executor.execute(() -> {
            try {
                int count = noteDao.getNotesCount();
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