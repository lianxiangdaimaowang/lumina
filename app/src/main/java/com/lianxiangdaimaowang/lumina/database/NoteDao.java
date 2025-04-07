package com.lianxiangdaimaowang.lumina.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 笔记DAO接口，用于Room数据库操作
 */
@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM notes WHERE id = :id")
    NoteEntity getNoteById(long id);

    @Query("SELECT * FROM notes ORDER BY lastModifiedDate DESC")
    LiveData<List<NoteEntity>> getAllNotes();

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY lastModifiedDate DESC")
    LiveData<List<NoteEntity>> searchNotes(String query);

    @Query("SELECT * FROM notes WHERE subject = :subject ORDER BY lastModifiedDate DESC")
    LiveData<List<NoteEntity>> getNotesBySubject(String subject);

    @Query("SELECT COUNT(*) FROM notes")
    int getNotesCount();
} 