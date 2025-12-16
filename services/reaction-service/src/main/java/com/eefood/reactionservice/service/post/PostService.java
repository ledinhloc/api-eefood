package com.eefood.reactionservice.service.post;

import com.eefood.common.avro.NotificationEvent;
import com.eefood.common.avro.PostApprovalRequest;
import com.eefood.reactionservice.dto.SearchResult;
import com.eefood.reactionservice.dto.request.PostCreateRequest;
import com.eefood.reactionservice.dto.response.*;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.enums.PostStatus;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.kafka.NotificationProducer;
import com.eefood.reactionservice.kafka.PostApprovalProducer;
import com.eefood.reactionservice.mapper.PostMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.httpclient.RecipeClient;
import com.eefood.reactionservice.service.follow.FollowService;
import com.eefood.reactionservice.service.ai.GeminiService;
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
  private final GeminiService geminiService;
  private final FollowService followService;
  private final PostAdminSearchService postAdminSearchService;
  private final NotificationProducer notificationProducer;
  private final PostApprovalProducer postApprovalProducer;

  public List<PostPublishResponse> getPostsPublishByUser() {
    Long userId = securityUtil.getCurrentUserId();
    List<Post> posts = postRepo.findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

    if (posts.isEmpty()) return List.of();
    return posts.stream()
      .map(postMapper::toPublishResponse)
      .collect(Collectors.toList());
  }

  public PostPublishResponse createPost(PostCreateRequest request, Long userId) {
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
      .userId(userId)
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
      .status((request.getStatus()!=null && !request.getStatus().isBlank())
              ? PostStatus.valueOf(request.getStatus())
              : PostStatus.PENDING)
      .build();

    postRepo.save(post);

    //gui thong bao
    NotificationEvent notification = NotificationEvent.newBuilder()
      .setTitle("Bài đăng đang chờ duyệt")
      .setBody("Hệ thống đang xem xét " + recipe.getTitle() + " của bạn.")
      .setPath("/recipe-crud/"+post.getId())
      .setAvatarUrl("")
      .setPostImageUrl("")
      .setType("SYSTEM")
      .setUserId(post.getUserId())
      .build();
    notificationProducer.sendNotification(notification);

    postApprovalProducer.sendApprovalRequest(post);

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
      .status(post.getStatus().name())
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
    post.setStatus(PostStatus.EDITED_PENDING);
    postRepo.save(post);

    NotificationEvent notification = NotificationEvent.newBuilder()
      .setTitle("Bài đăng đang chờ duyệt lại")
      .setBody("Hệ thống đang xem xét lại bài đăng " + post.getTitle() + " của bạn sau khi chỉnh sửa.")
      .setPath("/recipe-crud/" + post.getId())
      .setAvatarUrl("")
      .setPostImageUrl("")
      .setType("SYSTEM")
      .setUserId(post.getUserId())
      .build();
    notificationProducer.sendNotification(notification);

    //gui yeu cau duyet
    postApprovalProducer.sendApprovalRequest(post);

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
      .status(post.getStatus().name())
      .difficulty(recipe != null && recipe.getDifficulty() != null ? recipe.getDifficulty().name() : null)
      .location(recipe != null ? recipe.getRegion() : null)
      .prepTime(recipe != null && recipe.getPrepTime() != null ? recipe.getPrepTime().toString() : null)
      .cookTime(recipe != null && recipe.getCookTime() != null ? recipe.getCookTime().toString() : null)
      .build();
  }

  public PostPublishResponse updatePostByAdmin(Long id, String content, String status) {
    Post post = postRepo.findByIdAndIsDeletedFalse(id);
    if (post == null) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    post.setContent(content);
    if(status!= null && !status.isBlank()) {
      post.setStatus(PostStatus.valueOf(status));
    }
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
            .status(post.getStatus().name())
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
    int page,
    int size
    ) {

    if(keyword!=null && keyword.equals("unknown")) {
      return new PageImpl<>(List.of());
    }

    //lay thong tin user
    Long currentUserId = securityUtil.getCurrentUserId();
    log.info(currentUserId.toString());
    ResponseData<UserResponse> userResponse = iamClient.getUserById(currentUserId);
    UserResponse user = userResponse.getData();
    //debug
    //log.info(user.toString());
    List<Long> newFollowings = followService.getNewFollowings(userId);
    List<Long> oldFollowings = followService.getOldFollowings(userId);


    //Lấy danh sách postIds từ Elasticsearch
    SearchResult esResult = postSearchService.searchPostIds(
      keyword,
      region,
      difficulty,
      category,
      maxCookTime,
      user,
      newFollowings,
      oldFollowings,
      page,
      size
    );

    List<Long> postIds = esResult.getIds();
    long total = esResult.getTotal();
    //debug
    log.info("----------------PostIds : " + postIds.toString());

    if (postIds.isEmpty()) {
      return new PageImpl<>(List.of());
    }

    // 3. Lấy Post từ DB theo postIds
    Specification<Post> spec = PostSpecification.isNotDeleted()
      .and(PostSpecification.hasPostIds(postIds))
      .and(PostSpecification.hasUserId(userId))
      .and(PostSpecification.hasStatus(PostStatus.APPROVED));

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
//    log.info("----------------" + postResponses.toString());
    return new PageImpl<>(postResponses, PageRequest.of(page - 1, size), total);
  }

  public Page<PostResponse> getOwnPosts(
          Long userId,
          Pageable pageable
  ) {

    Specification<Post> spec = PostSpecification.isNotDeleted()
            .and(PostSpecification.hasUserId(userId))
            .and(PostSpecification.hasStatus(PostStatus.APPROVED));

    Page<Post> pageResult = postRepo.findAll(spec, pageable);
    List<Post> posts = pageResult.getContent();

    if (posts.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<PostResponse> responses = mapToPostResponse(posts);

    return new PageImpl<>(responses, pageable, pageResult.getTotalElements());
  }

  public Page<PostResponse> getAllPostsByAdmin(
          String keyword,
          Long userId,
          String region,
          String difficulty,
          String category,
          Integer minPrepTime,
          Integer maxPrepTime,
          Integer minCookTime,
          Integer maxCookTime,
          Integer minReactionCount,
          Integer minTotalShares,
          String status,
          String sortBy,
          Pageable pageable
  ) {
    log.info("PostIds from query param: {}", status);


    SearchResult esResult = postAdminSearchService.searchPostIds(
            keyword,
            userId,
            region,
            difficulty,
            category,
            minPrepTime,
            maxPrepTime,
            minCookTime,
            maxCookTime,
            minReactionCount,
            minTotalShares,
            status,
            sortBy,
            pageable
    );

    List<Long> postIds = esResult.getIds();
    long total = esResult.getTotal();
    log.info("--------------PostIds : " + total);
    log.info("PostIds from ES: {}", postIds);

    if (postIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    Specification<Post> spec = PostSpecification.isNotDeleted()
            .and(PostSpecification.hasPostIds(postIds));

    if (status != null && !status.isBlank()) {
      PostStatus postStatus = PostStatus.valueOf(status);
      log.info("PostStatus from query param: {}", postStatus.name());
      spec = spec.and(PostSpecification.hasStatus(postStatus));
    }

    List<Post> posts = postRepo.findAll(spec);

    Map<Long, Post> postMap = posts.stream()
            .collect(Collectors.toMap(Post::getId, p -> p));

    List<Post> orderedPosts = postIds.stream()
            .map(postMap::get)
            .filter(Objects::nonNull)
            .toList();

    List<PostResponse> responses = mapToPostResponse(orderedPosts);

    return new PageImpl<>(responses, pageable, total);
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
