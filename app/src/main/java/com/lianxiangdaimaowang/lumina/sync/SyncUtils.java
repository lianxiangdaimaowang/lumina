package com.lianxiangdaimaowang.lumina.sync;

import android.util.Log;

import com.google.gson.Gson;
import com.lianxiangdaimaowang.lumina.model.Note;

import org.json.JSONObject;

import java.util.Map;

/**
 * 同步工具类
 * 提供各种同步相关的辅助方法
 */
public class SyncUtils {
    private static final String TAG = "SyncUtils";

    /**
     * 从JWT令牌中提取用户ID
     */
    public static String extractUserIdFromToken(String token) {
        try {
            // JWT令牌格式: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                // 解码payload部分(Base64)
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT));
                Log.d(TAG, "JWT令牌负载: " + payload);
                
                // 从payload中提取sub字段(用户ID)
                if (payload.contains("\"sub\":")) {
                    int start = payload.indexOf("\"sub\":") + 6;
                    int end = payload.indexOf(",", start);
                    if (end == -1) end = payload.indexOf("}", start);
                    String serverUserId = payload.substring(start, end).replace("\"", "").trim();
                    
                    Log.d(TAG, "从JWT令牌中提取的服务器用户ID: " + serverUserId);
                    return serverUserId;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析JWT令牌失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 规范化ID，处理可能包含小数点的ID
     */
    public static String normalizeId(String id) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        
        // 检查是否包含小数点
        if (id.contains(".")) {
            try {
                // 尝试转换为长整型
                double doubleValue = Double.parseDouble(id);
                long longValue = (long) doubleValue;
                return String.valueOf(longValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "ID无法转换为数字: " + id);
                try {
                    // 截取小数点前的部分
                    return id.substring(0, id.indexOf('.'));
                } catch (Exception ex) {
                    Log.e(TAG, "截取小数点前部分失败: " + ex.getMessage());
                }
            }
        }
        
        return id;
    }
    
    /**
     * 规范化categoryId，确保返回有效的整数值
     */
    public static Integer normalizeCategoryId(Object categoryId) {
        if (categoryId == null) {
            return 10; // 默认"其他"科目
        }
        
        try {
            // 处理数值类型
            if (categoryId instanceof Number) {
                return ((Number) categoryId).intValue();
            }
            
            // 处理字符串类型
            String categoryStr = String.valueOf(categoryId);
            if (categoryStr.equals("null") || categoryStr.isEmpty()) {
                return 10; // 默认"其他"科目
            }
            
            // 检查是否包含小数点
            if (categoryStr.contains(".")) {
                double doubleValue = Double.parseDouble(categoryStr);
                return (int) doubleValue;
            } else {
                return Integer.parseInt(categoryStr);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "无法转换categoryId: " + categoryId + ", 使用默认值10");
            return 10; // 默认"其他"科目
        }
    }
    
    /**
     * 将服务器返回的笔记ID规范化为客户端可用的格式
     * @param note 需要处理的笔记对象
     */
    public static void normalizeNoteIds(Note note) {
        if (note == null) return;
        
        // 处理笔记ID
        if (note.getId() != null) {
            String normalizedId = normalizeId(note.getId());
            note.setId(normalizedId);
        }
        
        // 处理用户ID
        if (note.getUserId() != null) {
            String normalizedUserId = normalizeId(note.getUserId());
            note.setUserId(normalizedUserId);
        }
    }

    /**
     * 根据categoryId映射到具体科目名称
     */
    public static String mapCategoryIdToSubject(String categoryId) {
        try {
            // 尝试解析为数字
            int id = Integer.parseInt(categoryId);
            
            // 根据ID映射到科目名称
            switch (id) {
                case 1: return "语文";
                case 2: return "数学";
                case 3: return "英语";
                case 4: return "物理";
                case 5: return "化学";
                case 6: return "生物";
                case 7: return "历史";
                case 8: return "地理";
                case 9: return "政治";
                case 10: return "其他";
                default: return "其他";
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，直接返回categoryId
            if (categoryId != null && !categoryId.equals("null") && !categoryId.isEmpty()) {
                return categoryId;
            } else {
                return "其他";
            }
        }
    }

    /**
     * 处理服务器返回的笔记数据，确保科目信息正确
     */
    public static void processServerNoteFields(Note note, Gson gson) {
        if (note == null) return;
        
        try {
            // 保存原始科目信息用于日志记录
            String originalSubject = note.getSubject();
            
            // 优先使用note对象上的categoryId属性
            Integer categoryId = note.getCategoryId();
            boolean categoryIdProcessed = (categoryId != null);
            
            // 如果没有categoryId属性，尝试从JSON中提取
            if (!categoryIdProcessed) {
                try {
                    // 将笔记转换为JSON以检查字段
                    String noteJson = gson.toJson(note);
                    Log.d(TAG, "处理笔记JSON: " + noteJson);
                    
                    JSONObject noteObj = new JSONObject(noteJson);
                    
                    // 检查是否有categoryId字段
                    if (noteObj.has("categoryId") && !noteObj.isNull("categoryId")) {
                        // 获取categoryId
                        Object categoryIdRaw = noteObj.get("categoryId");
                        categoryId = normalizeCategoryId(categoryIdRaw);
                        categoryIdProcessed = true;
                        Log.d(TAG, "从JSON中获取到categoryId: " + categoryId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "从JSON解析categoryId失败: " + e.getMessage());
                }
            }
            
            // 如果找到了categoryId，根据它设置科目
            if (categoryIdProcessed && categoryId != null) {
                // 根据categoryId设置科目
                switch (categoryId) {
                    case 1:
                        note.setSubject("语文");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 语文, 笔记ID: " + note.getId());
                        break;
                    case 2:
                        note.setSubject("数学");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 数学, 笔记ID: " + note.getId());
                        break;
                    case 3:
                        note.setSubject("英语");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 英语, 笔记ID: " + note.getId());
                        break;
                    case 4:
                        note.setSubject("物理");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 物理, 笔记ID: " + note.getId());
                        break;
                    case 5:
                        note.setSubject("化学");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 化学, 笔记ID: " + note.getId());
                        break;
                    case 6:
                        note.setSubject("生物");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 生物, 笔记ID: " + note.getId());
                        break;
                    case 7:
                        note.setSubject("历史");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 历史, 笔记ID: " + note.getId());
                        break;
                    case 8:
                        note.setSubject("地理");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 地理, 笔记ID: " + note.getId());
                        break;
                    case 9:
                        note.setSubject("政治");
                        Log.d(TAG, "根据categoryId设置科目: " + categoryId + " -> 政治, 笔记ID: " + note.getId());
                        break;
                    default:
                        // 对于其他categoryId值，设置为"其他"
                        note.setSubject("其他");
                        Log.d(TAG, "未知的categoryId: " + categoryId + "，设置默认科目为'其他', 笔记ID: " + note.getId());
                        break;
                }
            } else {
                // 如果没有categoryId，设置默认科目为"其他"
                note.setSubject("其他");
                Log.d(TAG, "笔记没有categoryId信息，设置默认科目: 其他, 笔记ID: " + note.getId());
            }
            
            // 记录科目变化
            if (originalSubject != null && note.getSubject() != null && !originalSubject.equals(note.getSubject())) {
                Log.d(TAG, "笔记科目已更改: " + originalSubject + " -> " + note.getSubject() + ", 笔记ID: " + note.getId());
            } else if (originalSubject == null && note.getSubject() != null) {
                Log.d(TAG, "笔记科目已设置: null -> " + note.getSubject() + ", 笔记ID: " + note.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "处理服务器笔记字段时出错: " + e.getMessage(), e);
            // 确保笔记有科目，即使处理失败
            if (note.getSubject() == null || note.getSubject().isEmpty()) {
                note.setSubject("其他");
                Log.d(TAG, "处理失败，设置默认科目: 其他, 笔记ID: " + note.getId());
            }
        }
    }

    /**
     * 将categoryId添加到请求体中
     */
    public static Map<String, Object> addCategoryIdToRequest(Map<String, Object> request, Note note) {
        // 获取科目对应的categoryId
        Integer categoryId = note.getCategoryIdFromSubject();
        
        // 如果categoryId为null，使用默认值10
        if (categoryId == null) {
            // 如果科目不为空，尝试转换
            if (note.getSubject() != null && !note.getSubject().isEmpty()) {
                switch(note.getSubject()) {
                    case "语文": categoryId = 1; break;
                    case "数学": categoryId = 2; break;
                    case "英语": categoryId = 3; break;
                    case "物理": categoryId = 4; break;
                    case "化学": categoryId = 5; break;
                    case "生物": categoryId = 6; break;
                    case "历史": categoryId = 7; break;
                    case "地理": categoryId = 8; break;
                    case "政治": categoryId = 9; break;
                    default: categoryId = 10; break; // 默认其他
                }
            } else {
                categoryId = 10; // 默认其他
            }
        }
        
        // 将categoryId添加到请求中
        request.put("categoryId", categoryId);
        
        // 确保记录日志
        Log.d(TAG, "添加categoryId到请求: " + categoryId + ", 对应科目: " + note.getSubject());
        
        return request;
    }
} 