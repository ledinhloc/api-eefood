package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {
  private final PostRepository postRepo;
  private final PostMapper postMapper;

  public Page<PostResponse> getAllPosts(String title, Long userId, Pageable pageable) {
    Specification<Post> spec = Specification
      .where(PostSpecification.hasTitleLike(title))
      .and(PostSpecification.hasUserId(userId));

    Page<Post> posts = postRepo.findAll(spec, pageable);
    return posts.map(postMapper::toResponse);
  }

  public PostResponse getPostById(Long id) {
    Post post = postRepo.findByIdAndIsDeletedFalse(id);
    if (post == null) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }
    return postMapper.toResponse(post);
  }
}
