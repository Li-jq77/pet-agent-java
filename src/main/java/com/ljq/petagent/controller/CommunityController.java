package com.ljq.petagent.controller;

import com.ljq.petagent.entity.AppUser;
import com.ljq.petagent.entity.Comment;
import com.ljq.petagent.entity.Post;
import com.ljq.petagent.repository.CommentRepository;
import com.ljq.petagent.repository.PostRepository;
import com.ljq.petagent.service.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CommunityController {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserService currentUserService;

    public CommunityController(
        PostRepository postRepository,
        CommentRepository commentRepository,
        CurrentUserService currentUserService
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/posts")
    public String list(@RequestParam(defaultValue = "1") int page, Model model) {
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(Math.max(0, page - 1), 12)
        );
        Map<Long, Integer> commentCounts = new HashMap<Long, Integer>();
        for (Post post : postPage.getContent()) {
            commentCounts.put(post.getId(), commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).size());
        }
        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("postPage", postPage);
        model.addAttribute("commentCounts", commentCounts);
        return "pets/post_list";
    }

    @GetMapping("/posts/new")
    public String createForm() {
        return "pets/post_form";
    }

    @PostMapping("/posts/new")
    public String create(
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam(defaultValue = "share") String category,
        @RequestParam(required = false) String image_url,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        if (title != null && !title.trim().isEmpty() && content != null && !content.trim().isEmpty()) {
            Post post = new Post();
            post.setAuthor(user);
            post.setTitle(title);
            post.setContent(content);
            post.setCategory(category);
            post.setImageUrl(image_url == null ? "" : image_url);
            postRepository.save(post);
        }
        return "redirect:/posts/";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        return "pets/post_detail";
    }

    @PostMapping("/posts/{id}")
    public String comment(
        @PathVariable Long id,
        @RequestParam String content,
        Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (content != null && !content.trim().isEmpty()) {
                Comment comment = new Comment();
                comment.setPost(post);
                comment.setAuthor(currentUserService.require(authentication));
                comment.setContent(content);
                commentRepository.save(comment);
            }
        }
        return "redirect:/posts/" + id + "/";
    }
}
