package com.lianxiangdaimaowang.lumina.note;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.database.NoteEntity;
import com.lianxiangdaimaowang.lumina.database.NoteRepository;
import com.lianxiangdaimaowang.lumina.model.Note;

import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment implements NotesAdapter.OnNoteClickListener {
    private static final String TAG = "NotesFragment";
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView;
    private NotesAdapter adapter;
    private List<Note> notes;
    private LocalDataManager localDataManager;
    private NoteRepository noteRepository;
    private String currentSearchQuery = "";
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        notes = new ArrayList<>();
        localDataManager = LocalDataManager.getInstance(requireContext());
        
        try {
            noteRepository = NoteRepository.getInstance(requireContext());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "数据库初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        emptyView = view.findViewById(R.id.empty_view);
        
        setupRecyclerView();
        setupSwipeRefresh();
        
        loadNotes();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotes();
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
    
    private void setupRecyclerView() {
        adapter = new NotesAdapter(notes, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::refreshNotes);
    }
    
    private void loadNotes() {
        notes.clear();
        
        if (noteRepository == null) {
            List<Note> noteList = localDataManager.getAllNotes();
            
            for (Note note : noteList) {
                if (shouldIncludeNote(note)) {
                    notes.add(note);
                }
            }
            
            adapter.notifyDataSetChanged();
            updateEmptyView();
            swipeRefresh.setRefreshing(false);
            return;
        }
        
        if (currentSearchQuery.isEmpty()) {
            noteRepository.getAllNotes().observe(getViewLifecycleOwner(), noteEntities -> {
                notes.clear();
                for (NoteEntity entity : noteEntities) {
                    notes.add(convertEntityToModel(entity));
                }
                adapter.notifyDataSetChanged();
                updateEmptyView();
                swipeRefresh.setRefreshing(false);
            });
        } else {
            noteRepository.searchNotes("%" + currentSearchQuery + "%").observe(getViewLifecycleOwner(), noteEntities -> {
                notes.clear();
                for (NoteEntity entity : noteEntities) {
                    notes.add(convertEntityToModel(entity));
                }
                adapter.notifyDataSetChanged();
                updateEmptyView();
                swipeRefresh.setRefreshing(false);
            });
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
    
    private void refreshNotes() {
        clearSearch();
        loadNotes();
    }
    
    private void updateEmptyView() {
        if (notes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void searchNotes(String query) {
        currentSearchQuery = query;
        loadNotes();
    }
    
    private void clearSearch() {
        currentSearchQuery = "";
        loadNotes();
    }
    
    @Override
    public void onNoteClick(Note note) {
        try {
            Intent intent = new Intent(getActivity(), NoteDetailActivity.class);
            intent.putExtra("note_id", Long.parseLong(note.getId()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "打开笔记详情失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                intent.putExtra("note_id", Long.parseLong(note.getId()));
                startActivity(intent);
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
            long noteId = Long.parseLong(note.getId());
            if (noteRepository != null) {
                noteRepository.deleteById(noteId);
                
                // 同时从本地数据管理器中删除
                if (localDataManager != null) {
                    localDataManager.deleteNote(note.getId());
                }
                
                Toast.makeText(requireContext(), R.string.note_deleted, Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "删除笔记失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 