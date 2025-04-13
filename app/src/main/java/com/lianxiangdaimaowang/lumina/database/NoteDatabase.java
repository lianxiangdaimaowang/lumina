package com.lianxiangdaimaowang.lumina.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.lianxiangdaimaowang.lumina.data.LocalDataManager;

/**
 * 笔记数据库类
 */
@Database(entities = {NoteEntity.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class, StringListConverter.class})
public abstract class NoteDatabase extends RoomDatabase {
    private static final String TAG = "NoteDatabase";
    private static volatile NoteDatabase INSTANCE;

    // 获取DAO
    public abstract NoteDao noteDao();
    
    // 数据库版本1到版本2的迁移方案
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加userId字段
            database.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT");
            
            // 为现有数据设置当前用户ID
            Log.d(TAG, "数据库迁移：为现有笔记设置用户ID");
        }
    };

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
                        LocalDataManager localDataManager = LocalDataManager.getInstance(context);
                        final String currentUserId = localDataManager.getCurrentUserId();
                        
                        Log.d(TAG, "开始创建数据库实例，当前用户ID: " + currentUserId);
                        INSTANCE = Room.databaseBuilder(
                                appContext,
                                NoteDatabase.class,
                                "lumina_notes.db")
                                .addMigrations(MIGRATION_1_2)
                                .addCallback(new Callback() {
                                    @Override
                                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                        super.onOpen(db);
                                        // 设置所有没有用户ID的笔记为当前用户
                                        if (currentUserId != null && !currentUserId.isEmpty()) {
                                            try {
                                                db.execSQL("UPDATE notes SET userId = ? WHERE userId IS NULL OR userId = ''", 
                                                        new Object[]{currentUserId});
                                                Log.d(TAG, "数据库打开：已将没有用户ID的笔记设置为当前用户");
                                                
                                                // 记录用户的最后同步时间
                                                long currentTime = System.currentTimeMillis();
                                                db.execSQL("PRAGMA user_version = " + currentTime);
                                                Log.d(TAG, "数据库打开：更新用户最后同步时间: " + currentTime);
                                            } catch (Exception e) {
                                                Log.e(TAG, "更新用户ID失败", e);
                                            }
                                        }
                                    }
                                    
                                    @Override
                                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                        super.onCreate(db);
                                        Log.d(TAG, "创建新数据库");
                                        
                                        // 创建一些元数据表，用于跟踪同步状态
                                        try {
                                            db.execSQL("CREATE TABLE IF NOT EXISTS sync_metadata (" +
                                                    "user_id TEXT PRIMARY KEY, " +
                                                    "last_sync_time INTEGER, " +
                                                    "sync_status TEXT)");
                                            Log.d(TAG, "创建同步元数据表成功");
                                        } catch (Exception e) {
                                            Log.e(TAG, "创建同步元数据表失败", e);
                                        }
                                    }
                                })
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