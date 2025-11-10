package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.PostCreateRequest;
import com.eefood.reactionservice.dto.response.*;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mapper.PostMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  public List<PostPublishResponse> getPostsPublishByUser() {
    Long userId = securityUtil.getCurrentUserId();
    List<Post> posts = postRepo.findAllByUserIdAndIsDeletedFalse(userId);

    if (posts.isEmpty()) return List.of();
    return posts.stream()
      .map(postMapper::toPublishResponse)
      .collect(Collectors.toList());
  }

  public PostPublishResponse createPost(PostCreateRequest request) {
    Long currentUserId = securityUtil.getCurrentUserId();

    boolean exists = postRepo.existsByRecipeIdAndIsDeletedFalse(request.getRecipeId());
    if (exists) {
      throw ExceptionUtil.conflict(ErrorMessage.ALREADY_EXISTS);
    }

    ResponseData<RecipeSummaryResponse> recipeResponse = recipeClient.getRecipeSummary(request.getRecipeId());
    RecipeSummaryResponse recipe = recipeResponse.getData();

    if (recipe == null) {
      throw ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND);
    }

    Post post = Post.builder()
      .userId(currentUserId)
      .content(request.getContent())
      //thong tin recipe
      .recipeId(request.getRecipeId())
      .title(recipe.getTitle())
      .description(recipe.getDescription())
      .region(recipe.getRegion())
      .imageUrl(recipe.getImageUrl())
      .prepTime(recipe.getPrepTime())
      .cookTime(recipe.getCookTime())
      .difficulty(recipe.getDifficulty())
      .recipeCategories(recipe.getRecipeCategories())
      .recipeIngredientKeywords(recipe.getRecipeIngredientKeywords())
      .build();

    postRepo.save(post);

    // Tạo response giống như trong getPostsPublishByUser()
    return PostPublishResponse.builder()
      .id(post.getId())
      .recipeId(post.getRecipeId())
      .userId(post.getUserId())
      .title(post.getTitle())
      .content(post.getContent())
      .imageUrl(post.getImageUrl())
      .createdAt(post.getCreatedAt())
      .countReaction(0L)
      .countComment(0L)
      .difficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().name() : null)
      .location(recipe.getRegion())
      .prepTime(recipe.getPrepTime() != null ? recipe.getPrepTime().toString() : null)
      .cookTime(recipe.getCookTime() != null ? recipe.getCookTime().toString() : null)
      .build();
  }

  public PostPublishResponse updatePost(Long id, String content) {
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

    ResponseData<RecipeSummaryResponse> recipeResponse = recipeClient.getRecipeSummary(post.getRecipeId());
    RecipeSummaryResponse recipe = recipeResponse.getData();

    long countReaction = post.getReactions() != null ? post.getReactions().size() : 0;
    long countComment = post.getComments() != null ? post.getComments().size() : 0;

    return PostPublishResponse.builder()
      .id(post.getId())
      .recipeId(post.getRecipeId())
      .userId(post.getUserId())
      .title(post.getTitle())
      .content(post.getContent())
      .imageUrl(post.getImageUrl())
      .createdAt(post.getCreatedAt())
      .countReaction(countReaction)
      .countComment(countComment)
      .difficulty(recipe != null && recipe.getDifficulty() != null ? recipe.getDifficulty().name() : null)
      .location(recipe != null ? recipe.getRegion() : null)
      .prepTime(recipe != null && recipe.getPrepTime() != null ? recipe.getPrepTime().toString() : null)
      .cookTime(recipe != null && recipe.getCookTime() != null ? recipe.getCookTime().toString() : null)
      .build();
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
    String category,
    Integer maxCookTime,
    String sortBy,
    int page,
    int size
    ) {

    //lay thong tin user
    Long currentUserId = securityUtil.getCurrentUserId();
    ResponseData<UserResponse> userResponse = iamClient.getUserById(currentUserId);
    UserResponse user = userResponse.getData();
    //debug
    log.info(user.toString());

    //Lấy danh sách postIds từ Elasticsearch
    List<Long> postIds = postSearchService.searchPostIds(
      keyword,
      region,
      difficulty,
      category,
      maxCookTime,
      user,
      page,
      size
    );
    //debug
    log.info("----------------" + postIds.toString());

    if (postIds.isEmpty()) {
      return new PageImpl<>(List.of());
    }

    // 3. Lấy Post từ DB theo postIds
    Specification<Post> spec = PostSpecification.isNotDeleted()
      .and(PostSpecification.hasUserId(userId))
      .and(PostSpecification.hasPostIds(postIds));

    List<Post> posts = postRepo.findAll(spec);
    // 4. Sắp xếp theo thứ tự của postIds (theo ES)
    Map<Long, Post> postMap = posts.stream()
      .collect(Collectors.toMap(Post::getId, p -> p));

    List<Post> orderedPosts = postIds.stream()
      .map(postMap::get)
      .filter(Objects::nonNull)
      .toList();
    List<PostResponse> postResponses = mapToPostResponse(orderedPosts);
    //debug
    log.info("----------------" + postResponses.toString());
    return new PageImpl<>(postResponses, PageRequest.of(page - 1, size), postIds.size());
  }


  private List<PostResponse> mapToPostResponse(List<Post> posts) {
    // Lấy thông tin user
    List<Long> userIds = posts.stream().map(Post::getUserId).distinct().toList();
    List<UserInfo> userInfos = iamClient.getUserInfoBatch(userIds).getData();
    Map<Long, UserInfo> userInfoMap = userInfos.stream()
      .collect(Collectors.toMap(UserInfo::getId, u -> u));

    return posts.stream()
      .map(post -> {
        PostResponse response = postMapper.toResponse(post);
        UserInfo userInfo = userInfoMap.get(post.getUserId());
        if (userInfo != null) {
          response.setUsername(userInfo.getUsername());
          response.setEmail(userInfo.getEmail());
          response.setAvatarUrl(userInfo.getAvatarUrl());
        }
        return response;
      })
      .toList();
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
