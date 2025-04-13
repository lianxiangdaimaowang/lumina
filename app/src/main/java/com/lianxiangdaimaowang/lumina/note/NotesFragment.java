package com.lianxiangdaimaowang.lumina.note;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.database.NoteEntity;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.model.Note;
import com.lianxiangdaimaowang.lumina.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment implements NotesAdapter.OnNoteClickListener {
    private static final String TAG = "NotesFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private NotesAdapter adapter;
    private List<Note> notes = new ArrayList<>();
    private LocalDataManager localDataManager;
    private NoteRepository noteRepository;
    private String currentSearchQuery = "";
    private SyncManager syncManager;
    private ProgressBar loadingView;
    private TextView errorView;
    
    // 广播接收器，用于接收笔记删除通知
    private BroadcastReceiver noteDeletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.lianxiangdaimaowang.lumina.NOTE_DELETED".equals(intent.getAction())) {
                String deletedNoteId = intent.getStringExtra("note_id");
                Log.d(TAG, "收到笔记删除广播: " + deletedNoteId);
                
                // 刷新UI以反映笔记删除
                refreshNotes();
            }
        }
    };
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        localDataManager = LocalDataManager.getInstance(requireContext());
        syncManager = SyncManager.getInstance(requireContext());
        
        try {
            noteRepository = NoteRepository.getInstance(requireContext());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "数据库初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        emptyView = view.findViewById(R.id.empty_view);
        loadingView = view.findViewById(R.id.loading_view);
        errorView = view.findViewById(R.id.error_view);
        
        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 创建适配器
        adapter = new NotesAdapter(notes, this);
        recyclerView.setAdapter(adapter);
        
        // 设置下拉刷新监听器
        swipeRefresh.setOnRefreshListener(this::refreshNotes);
        
        // 加载笔记
        loadNotesFromCloud();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupSwipeRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Log.d(TAG, "NotesFragment onResume - 刷新笔记列表");
        
        // 如果已登录，强制加载笔记数据
        if (localDataManager.isSignedIn()) {
            // 执行完整刷新，确保列表正确显示
            forceRefreshNotes();
        } else {
            // 未登录，显示登录提示
            showError("请先登录后查看笔记");
        }
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notes, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty() && !currentSearchQuery.isEmpty()) {
                    clearSearch();
                }
                return false;
            }
        });
        
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::refreshNotes);
    }
    
    /**
     * 从云端加载笔记
     */
    private void loadNotesFromCloud() {
        // 显示加载中状态
        showLoading(true);
        hideError();
        
        // 首先获取本地缓存的云端笔记
        List<Note> cachedNotes = syncManager.getServerNotes();
        Log.d(TAG, "从缓存获取笔记列表，数量: " + (cachedNotes != null ? cachedNotes.size() : 0));
        
        if (cachedNotes != null && cachedNotes.size() > 0) {
            // 立即显示缓存的笔记，提高用户体验
            Log.d(TAG, "显示缓存笔记列表，第一条笔记标题: " + cachedNotes.get(0).getTitle());
            displayNotes(cachedNotes);
        } else {
            Log.d(TAG, "缓存笔记列表为空");
            // 更新UI以显示"暂无笔记"
            notes.clear();
            adapter.notifyDataSetChanged();
            updateEmptyView();
        }
        
        // 从服务器获取笔记数据
        fetchFromServer();
    }
    
    /**
     * 从服务器获取最新数据
     */
    private void fetchFromServer() {
        // 从服务器刷新最新笔记
        Log.d(TAG, "开始从服务器获取笔记列表");
        syncManager.fetchNotesFromServer(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null || !isAdded()) return;
                
                // 在主线程更新UI
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    
                    // 获取最新的服务器笔记
                    List<Note> serverNotes = syncManager.getServerNotes();
                    Log.d(TAG, "成功从服务器获取笔记，数量: " + serverNotes.size());
                    
                    // 显示笔记
                    if (serverNotes.size() > 0) {
                        Log.d(TAG, "服务器第一条笔记ID: " + serverNotes.get(0).getId() + 
                               ", 标题: " + serverNotes.get(0).getTitle() + 
                               ", 用户ID: " + serverNotes.get(0).getUserId());
                    }
                    
                    displayNotes(serverNotes);
                    
                    // 停止刷新动画
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null || !isAdded()) return;
                
                Log.e(TAG, "从服务器获取笔记失败: " + errorMessage);
                
                // 在主线程更新UI
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    // 只有在笔记列表为空时才显示错误
                    if (notes.isEmpty()) {
                        showError("无法从云端加载笔记: " + errorMessage);
                    } else {
                        // 已有数据，仅显示短提示
                        Toast.makeText(requireContext(), "同步失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                    
                    // 停止刷新动画
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }
    
    /**
     * 强制从服务器刷新笔记列表
     * 在用户返回笔记页面或创建新笔记后调用
     */
    public void forceRefreshNotes() {
        showLoading(true);
        hideError();
        
        // 强制从服务器获取数据，并同步本地数据库
        syncManager.syncLocalDatabaseWithServer(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null || !isAdded()) return;
                
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return; // 再次检查，确保Fragment仍然附加到Activity
                    showLoading(false);
                    List<Note> serverNotes = syncManager.getServerNotes();
                    displayNotes(serverNotes);
                    Log.d(TAG, "强制刷新成功，笔记数量: " + serverNotes.size());
                    
                    // 使用getActivity()替代requireActivity()
                    if (getActivity() != null && isAdded()) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) { // 确保Fragment仍然附加
                                refreshNotes();
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null || !isAdded()) return;
                
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return; // 再次检查Fragment状态
                    showLoading(false);
                    
                    // 如果有缓存数据，则显示缓存
                    List<Note> cachedNotes = syncManager.getServerNotes();
                    if (cachedNotes != null && !cachedNotes.isEmpty()) {
                        displayNotes(cachedNotes);
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "使用缓存数据，同步失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showError("无法获取笔记: " + errorMessage);
                    }
                });
            }
        });
    }
    
    /**
     * 刷新笔记
     */
    public void refreshNotes() {
        // 检查是否为下拉刷新
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            forceRefreshNotes(); // 下拉刷新时强制从服务器获取
        } else {
            loadNotesFromCloud(); // 普通刷新
        }
    }
    
    /**
     * 显示笔记列表
     */
    private void displayNotes(List<Note> noteList) {
        // 准备新的笔记列表
        List<Note> filteredNotes = new ArrayList<>();
        
        // 筛选符合条件的笔记（如果有搜索条件）
        if (currentSearchQuery.isEmpty()) {
            filteredNotes.addAll(noteList);
        } else {
            // 搜索筛选
            for (Note note : noteList) {
                if (matchesSearch(note, currentSearchQuery)) {
                    filteredNotes.add(note);
                }
            }
        }
        
        // 使用适配器的updateNotes方法更新列表，避免直接操作底层集合
        adapter.updateNotes(filteredNotes);
        
        // 更新空视图状态
        updateEmptyView();
    }
    
    /**
     * 检查笔记是否匹配搜索条件
     */
    private boolean matchesSearch(Note note, String query) {
        // 标题匹配
        if (note.getTitle() != null && note.getTitle().toLowerCase().contains(query.toLowerCase())) {
            return true;
        }
        
        // 内容匹配
        if (note.getContent() != null && note.getContent().toLowerCase().contains(query.toLowerCase())) {
            return true;
        }
        
        // 学科匹配
        if (note.getSubject() != null && note.getSubject().toLowerCase().contains(query.toLowerCase())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 显示/隐藏加载状态
     */
    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        if (errorView != null) {
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 隐藏错误信息
     */
    private void hideError() {
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
    }
    
    /**
     * 更新空视图状态
     */
    private void updateEmptyView() {
        if (emptyView != null) {
            if (adapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("暂无笔记，点击 + 按钮创建新笔记");
            } else {
                emptyView.setVisibility(View.GONE);
            }
        }
    }
    
    private Note convertEntityToModel(NoteEntity entity) {
        Note note = new Note(entity.getTitle(), entity.getContent(), entity.getSubject());
        note.setId(String.valueOf(entity.getId()));
        note.setCreatedDate(entity.getCreationDate());
        note.setLastModifiedDate(entity.getLastModifiedDate());
        
        if (entity.getAttachments() != null) {
            for (String attachment : entity.getAttachments()) {
                note.addAttachmentPath(attachment);
            }
        }
        
        return note;
    }
    
    private boolean shouldIncludeNote(Note note) {
        if (!currentSearchQuery.isEmpty()) {
            String title = note.getTitle().toLowerCase();
            String content = note.getContent().toLowerCase();
            String query = currentSearchQuery.toLowerCase();
            return title.contains(query) || content.contains(query);
        }
        
        return true;
    }
    
    private void syncNotesWithServer() {
        swipeRefresh.setRefreshing(true);
        
        if (syncManager != null) {
            syncManager.syncNotesWithServer(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            refreshNotes();
                        });
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            refreshNotes(); // 即使同步失败，也刷新本地数据
                            Toast.makeText(requireContext(), "笔记同步失败: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } else {
            refreshNotes();
        }
    }
    
    private void searchNotes(String query) {
        currentSearchQuery = query;
        loadNotesFromCloud();
    }
    
    private void clearSearch() {
        currentSearchQuery = "";
        loadNotesFromCloud();
    }
    
    @Override
    public void onNoteClick(Note note) {
        // 打开笔记详情页面，而不是直接打开编辑页面
        try {
            Intent intent = new Intent(getActivity(), NoteDetailActivity.class);
            intent.putExtra("note_id", note.getId());
            
            // 使用Fragment中的startActivityForResult
            Log.d(TAG, "点击笔记启动详情活动，笔记ID: " + note.getId());
            startActivityForResult(intent, 1003); // 使用请求码1003表示查看笔记详情
        } catch (Exception e) {
            Log.e(TAG, "启动笔记详情活动时出错", e);
            Toast.makeText(requireContext(), "无法打开笔记: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNoteMenuClick(Note note, View view) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.inflate(R.menu.menu_note_item);
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                Intent intent = new Intent(getActivity(), NoteEditActivity.class);
                intent.putExtra("note_id", note.getId());
                startActivityForResult(intent, 1002); // 使用请求码1002表示编辑现有笔记
                Log.d(TAG, "从菜单启动笔记编辑活动，等待结果返回, 笔记ID: " + note.getId());
                return true;
            } else if (id == R.id.action_delete) {
                confirmDeleteNote(note);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void confirmDeleteNote(Note note) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteNote(note))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteNote(Note note) {
        try {
            if (note == null || note.getId() == null) {
                Toast.makeText(requireContext(), "无效的笔记ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String noteId = note.getId();
            
            // 显示删除中状态
            showLoading(true);
            
            // 从SyncManager获取当前笔记列表
            List<Note> currentServerNotes = syncManager.getServerNotes();
            
            // 从列表中移除要删除的笔记
            List<Note> newNotesList = new ArrayList<>();
            if (currentServerNotes != null) {
                for (Note serverNote : currentServerNotes) {
                    // 检查所有ID形式（包括浮点数形式）
                    String serverNoteId = serverNote.getId();
                    boolean shouldRemove = noteId.equals(serverNoteId);
                    
                    // 处理浮点数形式的ID (如: "38.0")
                    if (!shouldRemove && serverNoteId != null && serverNoteId.contains(".")) {
                        String baseId = serverNoteId.substring(0, serverNoteId.indexOf('.'));
                        shouldRemove = noteId.equals(baseId);
                    }
                    
                    if (!shouldRemove) {
                        newNotesList.add(serverNote);
                    } else {
                        Log.d(TAG, "从显示列表中移除笔记: ID=" + serverNoteId);
                    }
                }
            }
            
            // 直接更新UI
            displayNotes(newNotesList);
            
            // 显示删除成功提示
            Toast.makeText(requireContext(), R.string.note_deleted, Toast.LENGTH_SHORT).show();
            
            // 隐藏加载状态
            showLoading(false);
            
            // 尝试从本地数据库中删除笔记
            try {
                long localId;
                // 处理可能是浮点数形式的ID (如: "38.0")
                if (noteId.contains(".")) {
                    try {
                        double doubleValue = Double.parseDouble(noteId);
                        localId = (long) doubleValue;
                        Log.d(TAG, "将浮点数笔记ID转换为整数: " + localId);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "无法将浮点数笔记ID转换为整数: " + noteId, e);
                        // 尝试去掉小数点后的部分
                        localId = Long.parseLong(noteId.substring(0, noteId.indexOf('.')));
                        Log.d(TAG, "通过截取小数点前部分转换ID: " + localId);
                    }
                } else {
                    localId = Long.parseLong(noteId);
                }
                
                NoteRepository noteRepository = NoteRepository.getInstance(requireContext());
                noteRepository.deleteById(localId);
                Log.d(TAG, "从本地数据库删除笔记: ID=" + localId);
            } catch (NumberFormatException e) {
                Log.e(TAG, "无法将笔记ID转换为数字，无法从本地数据库删除: " + noteId, e);
            }
            
            // 在后台执行服务器删除操作
            syncManager.deleteNote(noteId, new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() == null || !isAdded()) return;
                    
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "笔记删除已成功同步到服务器: " + noteId);
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    if (getActivity() == null || !isAdded()) return;
                    
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "笔记删除同步到服务器时出错: " + errorMessage, 
                            Toast.LENGTH_LONG).show();
                        // 刷新UI确保与服务器保持同步
                        forceRefreshNotes();
                    });
                }
            });
        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(requireContext(), "删除笔记失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            // 如果删除出错，强制刷新以恢复界面状态
            forceRefreshNotes();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "收到活动结果: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        // 处理从笔记编辑页面返回的结果
        if ((requestCode == 1001 || requestCode == 1002) && resultCode == getActivity().RESULT_OK) {
            boolean shouldRefresh = data != null && data.getBooleanExtra("refresh_notes", false);
            Log.d(TAG, "活动返回成功，请求码=" + requestCode + "，需要刷新: " + shouldRefresh);
            
            // 强制刷新笔记列表，不管是否有refresh_notes标志
            forceRefreshNotes();
            Log.d(TAG, "已调用forceRefreshNotes()强制刷新笔记列表");
        }
        // 处理从笔记详情页面返回的结果
        else if (requestCode == 1003) {
            // 无论结果如何，都刷新笔记列表以确保显示最新状态
            Log.d(TAG, "从详情页面返回，刷新笔记列表");
            forceRefreshNotes();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(noteDeletedReceiver, new IntentFilter("com.lianxiangdaimaowang.lumina.NOTE_DELETED"));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(noteDeletedReceiver);
    }
} 