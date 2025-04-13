package com.lianxiangdaimaowang.lumina.community;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.community.model.LocalPost;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 社区Activity
 */
public class CommunityActivity extends AppCompatActivity {
    private LocalDataManager localDataManager;
    private ListView lvPosts;
    private EditText etPostContent;
    private Button btnPost;
    private PostAdapter postAdapter;
    private List<LocalPost> posts;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // 初始化LocalDataManager
        localDataManager = LocalDataManager.getInstance(this);

        // 初始化视图
        initViews();
        // 加载帖子数据
        loadPosts();
    }

    private void initViews() {
        lvPosts = findViewById(R.id.lv_posts);
        etPostContent = findViewById(R.id.et_post_content);
        btnPost = findViewById(R.id.btn_post);

        // 初始化帖子列表
        posts = new ArrayList<>();
        postAdapter = new PostAdapter(this, posts);
        lvPosts.setAdapter(postAdapter);

        // 设置发帖按钮点击事件
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post();
            }
        });
    }

    private void loadPosts() {
        // 从本地加载帖子数据
        posts.clear();
        posts.addAll(localDataManager.getAllLocalPosts());
        postAdapter.notifyDataSetChanged();
    }

    private void post() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入帖子内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建新帖子
        LocalPost post = new LocalPost();
        post.setId(UUID.randomUUID().toString());
        post.setUserId(localDataManager.getCurrentUserId());
        post.setUsername(localDataManager.getCurrentUsername());
        post.setContent(content);
        post.setCreateTime(new Date());

        // 保存帖子
        localDataManager.saveLocalPost(post);
        Toast.makeText(this, "发帖成功", Toast.LENGTH_SHORT).show();

        // 清空输入框
        etPostContent.setText("");
        // 刷新帖子列表
        loadPosts();
    }
} 