package com.lianxiangdaimaowang.lumina.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * 笔记数据库类
 */
@Database(entities = {NoteEntity.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, StringListConverter.class})
public abstract class NoteDatabase extends RoomDatabase {
    private static final String TAG = "NoteDatabase";
    private static volatile NoteDatabase INSTANCE;

    // 获取DAO
    public abstract NoteDao noteDao();

    // 单例模式获取数据库实例
    public static NoteDatabase getInstance(Context context) {
        if (context == null) {
            Log.e(TAG, "初始化NoteDatabase时context为null");
            throw new IllegalArgumentException("Context不能为null");
        }
        
        if (INSTANCE == null) {
            synchronized (NoteDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        Context appContext = context.getApplicationContext();
                        Log.d(TAG, "开始创建数据库实例");
                        INSTANCE = Room.databaseBuilder(
                                appContext,
                                NoteDatabase.class,
                                "lumina_notes.db")
                                .fallbackToDestructiveMigration()
                                .build();
                        Log.d(TAG, "数据库实例创建成功");
                    } catch (Exception e) {
                        Log.e(TAG, "创建数据库实例失败", e);
                        throw e;
                    }
                }
            }
        }
        return INSTANCE;
    }
} 