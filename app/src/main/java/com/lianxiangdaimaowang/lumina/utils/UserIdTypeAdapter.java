package com.lianxiangdaimaowang.lumina.utils;

import android.util.Log;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 用户ID类型适配器
 * 负责在JSON序列化和反序列化时处理用户ID的类型转换
 * 服务器端数据类型是Long，而客户端数据类型是String
 */
public class UserIdTypeAdapter extends TypeAdapter<String> {
    private static final String TAG = "UserIdTypeAdapter";

    @Override
    public void write(JsonWriter out, String value) throws IOException {
        if (value == null) {
            // 如果没有用户ID，输出null让服务器处理
            out.nullValue();
            Log.d(TAG, "用户ID为null，输出为null值");
            return;
        }

        try {
            // 首先检查是否包含小数点，如果包含则将其转换为整数
            if (value.contains(".")) {
                // 尝试将浮点数转换为整数
                try {
                    double doubleValue = Double.parseDouble(value);
                    long longValue = (long) doubleValue;
                    Log.d(TAG, "将浮点数用户ID转换为整数: " + value + " -> " + longValue);
                    value = String.valueOf(longValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "无法将浮点数用户ID转换为整数: " + value);
                    // 尝试截取小数点前的部分
                    try {
                        value = value.substring(0, value.indexOf('.'));
                        Log.d(TAG, "通过截取小数点前的部分获取整数: " + value);
                    } catch (Exception ex) {
                        Log.e(TAG, "截取小数点前部分失败: " + ex.getMessage());
                    }
                }
            }
            
            // 尝试将字符串转换为Long，发送到服务器
            long longValue = Long.parseLong(value);
            out.value(longValue);
            Log.d(TAG, "将用户ID从String转为Long: " + value + " -> " + longValue);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，就将原始值作为字符串写入
            Log.e(TAG, "用户ID不是有效数字: " + value + "，作为字符串写入");
            // 对于无法转换的值，输出一个默认数字值而不是字符串，避免服务器端类型不匹配
            try {
                out.value(0L);
                Log.d(TAG, "无法解析的用户ID改为输出默认值 0");
            } catch (Exception ex) {
                // 如果还是失败，则输出字符串
                out.value(value);
                Log.e(TAG, "输出默认值失败，继续输出原始字符串: " + value);
            }
        }
    }

    @Override
    public String read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            Log.d(TAG, "读取到NULL用户ID，返回null");
            return null;
        }

        try {
            if (in.peek() == JsonToken.NUMBER) {
                // 如果是数字类型，读取为长整型然后转为字符串
                long longValue = in.nextLong();
                String result = String.valueOf(longValue);
                Log.d(TAG, "将用户ID从Long转为String: " + longValue + " -> " + result);
                return result;
            } else if (in.peek() == JsonToken.STRING) {
                // 如果已经是字符串，直接返回
                String value = in.nextString();
                
                // 检查是否包含小数点，处理浮点数格式
                if (value.contains(".")) {
                    try {
                        // 尝试将浮点数转换为整数字符串
                        double doubleValue = Double.parseDouble(value);
                        long longValue = (long) doubleValue;
                        String result = String.valueOf(longValue);
                        Log.d(TAG, "将浮点数用户ID字符串转换为整数: " + value + " -> " + result);
                        return result;
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "无法将浮点数用户ID字符串转换为整数: " + value);
                        // 尝试截取小数点前的部分
                        try {
                            String result = value.substring(0, value.indexOf('.'));
                            Log.d(TAG, "通过截取小数点前的部分获取整数: " + result);
                            return result;
                        } catch (Exception ex) {
                            Log.e(TAG, "截取小数点前部分失败: " + ex.getMessage());
                            return value;
                        }
                    }
                }
                
                // 如果不是浮点数格式，直接返回原始字符串
                Log.d(TAG, "读取到的用户ID是字符串: " + value);
                return value;
            } else {
                // 其他类型，跳过并返回null
                in.skipValue();
                Log.e(TAG, "未知的用户ID类型，返回null");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "读取用户ID时出错: " + e.getMessage());
            // 尝试跳过这个值
            try {
                in.skipValue();
            } catch (Exception ex) {
                // 忽略跳过时可能发生的异常
            }
            return null;
        }
    }
} 