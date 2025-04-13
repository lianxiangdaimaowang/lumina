package com.lianxiangdaimaowang.lumina.service;

import com.lianxiangdaimaowang.lumina.entity.Post;
import com.lianxiangdaimaowang.lumina.entity.User;
import com.lianxiangdaimaowang.lumina.repository.PostRepository;
import com.lianxiangdaimaowang.lumina.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    public Post createPost(String username, String title, String content) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);

        return postRepository.save(post);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
} 