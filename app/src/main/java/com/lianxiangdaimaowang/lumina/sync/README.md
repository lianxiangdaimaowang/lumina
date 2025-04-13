# Lumina 同步模块

本模块负责应用程序数据的同步操作，包括笔记和社区帖子的同步。

## 模块架构

同步模块采用了分层设计，使代码更加模块化和可维护：

1. **SyncManager**：
   - 作为整个同步系统的门面(Facade)，对外提供统一的API
   - 协调笔记和帖子的同步操作
   - 处理向后兼容性问题
   - 提供超时机制和操作跟踪

2. **NoteSynchronizer**：
   - 专门负责笔记数据的同步
   - 处理笔记的创建、更新、删除和获取操作

3. **PostSynchronizer**：
   - 专门负责社区帖子的同步
   - 处理帖子的创建、更新、删除和获取操作

4. **SyncUtils**：
   - 提供同步过程中需要的通用工具方法
   - 处理ID规范化、数据转换等功能

5. **SyncCallback**：
   - 定义同步操作的回调接口
   - 提供成功和失败的统一处理机制

6. **SyncCallbackAdapter**：
   - 适配器类，用于在不同类型的回调接口之间进行转换
   - 解决接口兼容性问题，确保新旧代码能够正常协作
   - 使用弱引用避免内存泄漏

7. **NetworkChangeReceiver**：
   - 监听网络状态变化
   - 在网络恢复时触发同步操作
   - 实现防抖动机制避免频繁触发同步

## 性能优化和稳定性保障

### 超时机制

所有同步操作都有超时机制，避免无限等待：

- 默认超时时间为30秒
- 超时后会自动中断同步操作
- 确保应用不会因为网络问题而卡死

### 防抖动处理

`NetworkChangeReceiver` 实现了防抖动机制：

- 5秒内的重复网络变化事件会被忽略
- 避免网络波动时频繁触发同步
- 减少不必要的网络请求和电量消耗

### 操作跟踪

`SyncManager` 实现了操作跟踪功能：

- 记录正在进行的同步操作数量
- 避免同时进行多个相同的同步操作
- 提供 `hasPendingOperations()` 方法让应用可以查询同步状态

### 内存泄漏预防

`SyncCallbackAdapter` 使用弱引用：

- 避免回调接口导致的内存泄漏
- 确保即使回调未被触发也不会造成内存问题

## 使用方法

### 基本同步操作

```java
// 获取SyncManager实例
SyncManager syncManager = SyncManager.getInstance(context);

// 保存并同步笔记
syncManager.saveNote(note, new SyncManager.SyncCallback() {
    @Override
    public void onSuccess() {
        // 处理同步成功
    }
    
    @Override
    public void onError(String errorMessage) {
        // 处理同步失败
    }
});

// 获取服务器数据
syncManager.fetchNotesFromServer(new SyncManager.SyncCallback() {
    @Override
    public void onSuccess() {
        // 获取同步的笔记列表
        List<Note> notes = syncManager.getServerNotes();
        // 处理获取到的笔记
    }
    
    @Override
    public void onError(String errorMessage) {
        // 处理获取失败
    }
});
```

### 检查同步状态

```java
// 检查是否有同步操作正在进行
boolean isSyncing = syncManager.hasPendingOperations();

// 获取正在进行的同步操作数量
int pendingOps = syncManager.getPendingOperationCount();

if (isSyncing) {
    // 显示同步中提示
    showSyncingIndicator();
} else {
    // 隐藏同步指示器
    hideSyncingIndicator();
}
```

### 社区帖子操作

```java
// 创建帖子
syncManager.createPost(post, new SyncManager.SyncCallback() {
    @Override
    public void onSuccess() {
        // 处理创建成功
    }
    
    @Override
    public void onError(String errorMessage) {
        // 处理创建失败
    }
});

// 获取所有帖子
syncManager.getAllPostsFromServer(new SyncManager.SyncCallback() {
    @Override
    public void onSuccess() {
        // 处理获取成功
    }
    
    @Override
    public void onError(String errorMessage) {
        // 处理获取失败
    }
});
```

### 手动同步所有数据

```java
// 同步所有待同步的数据
syncManager.syncAllPendingItems(new SyncManager.SyncCallback() {
    @Override
    public void onSuccess() {
        // 处理同步成功
    }
    
    @Override
    public void onError(String errorMessage) {
        // 处理同步失败
    }
});
```

### 在新代码中使用SyncCallback

如果你正在编写直接与NoteSynchronizer或PostSynchronizer交互的新代码，可以使用SyncCallbackAdapter进行适配：

```java
// 从外部SyncCallback转换为内部SyncCallback
SyncCallback callback = ...;
SyncManager.SyncCallback managerCallback = SyncCallbackAdapter.toSyncManagerCallback(callback);

// 从内部SyncCallback转换为外部SyncCallback
SyncManager.SyncCallback managerCallback = ...;
SyncCallback callback = SyncCallbackAdapter.toSyncCallback(managerCallback);
```

## 设计说明

1. **分离关注点**：将不同类型的同步逻辑分离到不同的类中
2. **保持兼容性**：保留原有的API接口，确保现有代码不需要大量修改
3. **统一错误处理**：所有同步操作使用统一的回调接口处理结果
4. **网络状态感知**：自动响应网络状态变化，优化用户体验
5. **适配器模式**：通过SyncCallbackAdapter解决不同接口之间的兼容性问题
6. **防线程安全问题**：使用原子变量和弱引用避免线程安全问题
7. **超时保护**：所有网络操作都有超时机制，避免应用卡死
8. **防抖动**：实现网络变化的防抖动，避免频繁触发同步 