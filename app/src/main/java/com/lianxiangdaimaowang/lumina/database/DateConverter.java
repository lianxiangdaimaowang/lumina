package com.lianxiangdaimaowang.lumina.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Date类型转换器，用于Room数据库
 */
public class DateConverter {
    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }
} 