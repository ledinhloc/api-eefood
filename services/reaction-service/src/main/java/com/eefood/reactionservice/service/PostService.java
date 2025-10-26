package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
  private final PostRepository postRepo;
  private final PostMapper postMapper;
  private final IamClient iamClient;
  private final PostSearchService postSearchService;
  private final RecipeClient recipeClient;

  public Page<PostResponse> getAllPosts(
    String keyword,
    Long userId,
    String region,
    String difficulty,
    Pageable pageable) {
    List<Long> recipeIds = List.of();

    boolean hasRecipeFilter =
      (keyword != null && !keyword.isBlank()) ||
        (region != null && !region.isBlank()) ||
        (difficulty != null && !difficulty.isBlank());


    // chỉ gọi sang recipe-service khi có tiêu chí lọc
    if (hasRecipeFilter) {
      try {
        ResponseData<List<Long>> response = recipeClient.searchRecipeIds(keyword, region, difficulty);
        recipeIds = response.getData() != null ? response.getData() : List.of();
        log.info("Filtered recipe IDs: {}", recipeIds);
      } catch (Exception e) {
        log.error("Error calling recipe service: {}", e.getMessage());
      }
    }

    // Tìm theo nội dung bài viết (content)
    List<Long> postIdsFromES = List.of();
    if (keyword != null && !keyword.isBlank()) {
      postIdsFromES = postSearchService.searchPostIdsByContent(keyword);
      log.info("Filtered post IDs from Elasticsearch (content): {}", postIdsFromES);
    }

    // Kết hợp điều kiện
    Specification<Post> spec =
      PostSpecification.isNotDeleted()
        .and(PostSpecification.hasUserId(userId))
        .and(PostSpecification.hasRecipeIds(recipeIds))
        .and(PostSpecification.hasPostIds(postIdsFromES));

    Page<Post> posts = postRepo.findAll(spec, pageable);
    return mapToPostResponse(posts);
  }

  private Page<PostResponse> mapToPostResponse(Page<Post> posts) {
    //lay thong tin user
    List<Long> userIds = posts.stream().map(Post::getUserId).distinct().toList();
    List<UserInfo> userInfos = iamClient.getUserInfoBatch(userIds).getData();
    Map<Long, UserInfo> userInfoMap = userInfos.stream().collect(Collectors.toMap(UserInfo::getId, u -> u));

    return posts.map(post ->{
      PostResponse response = postMapper.toResponse(post);
      UserInfo userInfo = userInfoMap.get(post.getUserId());
      if(userInfo != null){
        response.setUsername(userInfo.getUsername());
        response.setEmail(userInfo.getEmail());
        response.setAvatarUrl(userInfo.getAvatarUrl());
      }
      return response;
    });
  }
  public PostResponse getPostById(Long id) {
    Post post = postRepo.findByIdAndIsDeletedFalse(id);
    if (post == null) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    PostResponse response = postMapper.toResponse(post);

    UserInfo userInfo = iamClient.getUserInfo(post.getUserId()).getData();
    response.setUsername(userInfo.getUsername());
    response.setEmail(userInfo.getEmail());
    response.setAvatarUrl(userInfo.getAvatarUrl());

    return response;
  }
}
