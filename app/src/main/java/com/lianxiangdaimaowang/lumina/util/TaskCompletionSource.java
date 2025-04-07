package com.lianxiangdaimaowang.lumina.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 任务完成源，用于模拟Firebase Task API
 * @param <TResult> 任务结果类型
 */
public class TaskCompletionSource<TResult> {
    
    private final Object lock = new Object();
    private TResult result;
    private Exception exception;
    private boolean isComplete = false;
    private final List<OnCompleteListener<TResult>> completeListeners = new ArrayList<>();
    private final List<OnSuccessListener<TResult>> successListeners = new ArrayList<>();
    private final List<OnFailureListener> failureListeners = new ArrayList<>();
    
    /**
     * 设置任务结果，使任务完成
     * @param result 任务结果
     */
    public void setResult(TResult result) {
        synchronized (lock) {
            if (isComplete) {
                throw new IllegalStateException("任务已完成");
            }
            
            this.result = result;
            this.isComplete = true;
            
            // 通知所有监听器
            notifyListeners();
        }
    }
    
    /**
     * 设置任务异常，使任务失败
     * @param exception 异常
     */
    public void setException(@NonNull Exception exception) {
        synchronized (lock) {
            if (isComplete) {
                throw new IllegalStateException("任务已完成");
            }
            
            this.exception = exception;
            this.isComplete = true;
            
            // 通知所有监听器
            notifyListeners();
        }
    }
    
    /**
     * 获取任务
     * @return 任务对象
     */
    public Task<TResult> getTask() {
        return new Task<>(this);
    }
    
    // 通知所有监听器
    private void notifyListeners() {
        for (OnCompleteListener<TResult> listener : completeListeners) {
            listener.onComplete(getTask());
        }
        
        if (exception == null) {
            for (OnSuccessListener<TResult> listener : successListeners) {
                listener.onSuccess(result);
            }
        } else {
            for (OnFailureListener listener : failureListeners) {
                listener.onFailure(exception);
            }
        }
    }
    
    /**
     * 添加完成监听器
     * @param listener 监听器
     */
    void addOnCompleteListener(OnCompleteListener<TResult> listener) {
        synchronized (lock) {
            completeListeners.add(listener);
            
            if (isComplete) {
                listener.onComplete(getTask());
            }
        }
    }
    
    /**
     * 添加成功监听器
     * @param listener 监听器
     */
    void addOnSuccessListener(OnSuccessListener<TResult> listener) {
        synchronized (lock) {
            successListeners.add(listener);
            
            if (isComplete && exception == null) {
                listener.onSuccess(result);
            }
        }
    }
    
    /**
     * 添加失败监听器
     * @param listener 监听器
     */
    void addOnFailureListener(OnFailureListener listener) {
        synchronized (lock) {
            failureListeners.add(listener);
            
            if (isComplete && exception != null) {
                listener.onFailure(exception);
            }
        }
    }
    
    /**
     * 任务类，模拟Firebase Task
     * @param <TResult> 任务结果类型
     */
    public static class Task<TResult> {
        private final TaskCompletionSource<TResult> source;
        
        Task(TaskCompletionSource<TResult> source) {
            this.source = source;
        }
        
        /**
         * 任务是否完成
         * @return 是否完成
         */
        public boolean isComplete() {
            return source.isComplete;
        }
        
        /**
         * 任务是否成功
         * @return 是否成功
         */
        public boolean isSuccessful() {
            return source.isComplete && source.exception == null;
        }
        
        /**
         * 获取任务结果
         * @return 任务结果
         */
        @Nullable
        public TResult getResult() {
            if (!source.isComplete) {
                throw new IllegalStateException("任务未完成");
            }
            
            if (source.exception != null) {
                throw new RuntimeException("任务失败", source.exception);
            }
            
            return source.result;
        }
        
        /**
         * 获取异常
         * @return 异常
         */
        @Nullable
        public Exception getException() {
            return source.exception;
        }
        
        /**
         * 添加完成监听器
         * @param listener 监听器
         * @return 任务对象
         */
        public Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> listener) {
            source.addOnCompleteListener(listener);
            return this;
        }
        
        /**
         * 添加完成监听器（指定执行器）
         * @param executor 执行器
         * @param listener 监听器
         * @return 任务对象
         */
        public Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> listener) {
            executor.execute(() -> source.addOnCompleteListener(listener));
            return this;
        }
        
        /**
         * 添加成功监听器
         * @param listener 监听器
         * @return 任务对象
         */
        public Task<TResult> addOnSuccessListener(OnSuccessListener<TResult> listener) {
            source.addOnSuccessListener(listener);
            return this;
        }
        
        /**
         * 添加成功监听器（指定执行器）
         * @param executor 执行器
         * @param listener 监听器
         * @return 任务对象
         */
        public Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<TResult> listener) {
            executor.execute(() -> source.addOnSuccessListener(listener));
            return this;
        }
        
        /**
         * 添加失败监听器
         * @param listener 监听器
         * @return 任务对象
         */
        public Task<TResult> addOnFailureListener(OnFailureListener listener) {
            source.addOnFailureListener(listener);
            return this;
        }
        
        /**
         * 添加失败监听器（指定执行器）
         * @param executor 执行器
         * @param listener 监听器
         * @return 任务对象
         */
        public Task<TResult> addOnFailureListener(Executor executor, OnFailureListener listener) {
            executor.execute(() -> source.addOnFailureListener(listener));
            return this;
        }
    }
    
    /**
     * 完成监听器接口
     * @param <TResult> 任务结果类型
     */
    public interface OnCompleteListener<TResult> {
        void onComplete(Task<TResult> task);
    }
    
    /**
     * 成功监听器接口
     * @param <TResult> 任务结果类型
     */
    public interface OnSuccessListener<TResult> {
        void onSuccess(TResult result);
    }
    
    /**
     * 失败监听器接口
     */
    public interface OnFailureListener {
        void onFailure(Exception e);
    }
} 