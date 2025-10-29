package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.PostCreateRequest;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.RecipeSummaryResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.util.SecurityUtil;
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
  private final SecurityUtil securityUtil;

  public PostResponse createPost(PostCreateRequest request) {
    Long currentUserId = securityUtil.getCurrentUserId();

    // Kiểm tra xem recipe này đã có post chưa bị xóa chưa
    boolean exists = postRepo.existsByRecipeIdAndIsDeletedFalse(request.getRecipeId());
    if (exists) {
      throw ExceptionUtil.conflict(ErrorMessage.ALREADY_EXISTS);
    }

    // Lấy thông tin recipe từ Recipe Service
    ResponseData<RecipeSummaryResponse> recipeResponse = recipeClient.getRecipeSummary(request.getRecipeId());
    RecipeSummaryResponse recipe = recipeResponse.getData();

    if (recipe == null) {
      throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
    }

    Post post = Post.builder()
      .userId(currentUserId)
      .recipeId(request.getRecipeId())
      .content(request.getContent())
      .title(recipe.getTitle())
      .imageUrl(recipe.getImageUrl())
      .build();

    postRepo.save(post);
    return postMapper.toResponse(post);
  }

  public PostResponse updatePost(Long id, String content) {
    Post post = postRepo.findByIdAndIsDeletedFalse(id);
    if (post == null) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    Long currentUserId = securityUtil.getCurrentUserId();
    if (!post.getUserId().equals(currentUserId)) {
      throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
    }

    post.setContent(content);
    postRepo.save(post);
    return postMapper.toResponse(post);
  }

  public void deletePost(Long id) {
    Post post = postRepo.findByIdAndIsDeletedFalse(id);
    if (post == null) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    Long currentUserId = securityUtil.getCurrentUserId();
    boolean isAdmin = securityUtil.hasRole("ADMIN");
    if (!isAdmin && !post.getUserId().equals(currentUserId)) {
      throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
    }

    post.setIsDeleted(true);
    postRepo.save(post);
  }

  public Page<PostResponse> getAllPosts(
    String keyword,
    Long userId,
    String region,
    String difficulty,
    Pageable pageable) {
    List<Long> recipeIds = List.of();
    //kiem tra loc
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
    Specification<Post> spec = PostSpecification.isNotDeleted()
      .and(PostSpecification.hasUserId(userId));

    if (!recipeIds.isEmpty() && !postIdsFromES.isEmpty()) {
      spec = spec.and(
        PostSpecification.hasRecipeIds(recipeIds)
          .or(PostSpecification.hasPostIds(postIdsFromES))
      );
    } else if (!recipeIds.isEmpty()) {
      spec = spec.and(PostSpecification.hasRecipeIds(recipeIds));
    } else if (!postIdsFromES.isEmpty()) {
      spec = spec.and(PostSpecification.hasPostIds(postIdsFromES));
    }

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
