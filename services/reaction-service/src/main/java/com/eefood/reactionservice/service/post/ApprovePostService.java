package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.ApprovePostResponse;
import com.eefood.reactionservice.mapper.ApprovePostMapper;
import com.eefood.reactionservice.model.ApprovePost;
import com.eefood.reactionservice.repository.ApprovePostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovePostService {

  private final ApprovePostRepository repository;
  private final ApprovePostMapper mapper;

  public ApprovePostResponse save(ApprovePost approvePost) {
    ApprovePost saved = repository.save(approvePost);
    return mapper.toResponse(saved);
  }

  public List<ApprovePostResponse> getByPostId(Long postId) {
    return repository.findByPostIdOrderByCreatedAtDesc(postId)
      .stream()
      .map(mapper::toResponse)
      .collect(Collectors.toList());
  }
}
