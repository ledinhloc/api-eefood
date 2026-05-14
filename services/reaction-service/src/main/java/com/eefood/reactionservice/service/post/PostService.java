package com.eefood.reactionservice.service.post;

import com.eefood.common.avro.NotificationEvent;
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
import com.eefood.reactionservice.service.chatbot.ChromaEmbeddingService;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
  private static final double STRONG_REDUCED_WEIGHT = 0.1d;
  private static final double LIGHT_REDUCED_WEIGHT = 0.5d;
  private static final Set<String> STRONG_REDUCED_INGREDIENTS = Set.of(
    "muối", "đường", "tiêu", "nước", "dầu ăn", "bột ngọt", "hạt nêm", "gia vị"
  );
  private static final Set<String> LIGHT_REDUCED_INGREDIENTS = Set.of(
    "tỏi", "ớt", "hành", "nước mắm", "xì dầu", "dầu hào"
  );

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
  private final ChromaEmbeddingService chromaEmbeddingService;

  private record SimilarCandidateScore(Post post, double score, List<String> matchedIngredients) {}

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
    // Sync post to chromaDB
    chromaEmbeddingService.syncSinglePostToChroma(post.getId());

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
    // Sync post to chromaDB
    chromaEmbeddingService.syncSinglePostToChroma(post.getId());

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
//    Long userId,
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

    //  Lấy userId từ security context (null nếu là guest)
    Long currentUserId = null;
    UserResponse user = null;
    List<Long> newFollowings = List.of();
    List<Long> oldFollowings = List.of();
    try {
      currentUserId = securityUtil.getCurrentUserId();
      log.info("Logged-in user: {}", currentUserId);

      if(currentUserId != null){
        // Lấy thông tin user và followings CHỈ KHI đã login
        ResponseData<UserResponse> userResponse = iamClient.getUserById(currentUserId);
        user = userResponse.getData();
        newFollowings = followService.getNewFollowings(currentUserId);
        oldFollowings = followService.getOldFollowings(currentUserId);
      }
    } catch (Exception e) {
      // Guest user - không có token
      log.info("Guest user - no personalization applied");
    }


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

  @Transactional(readOnly = true)
  // Tìm các bài viết món ăn tương tự từ recipe gốc, có thể lọc thêm theo danh sách nguyên liệu đầu vào.
  public List<SimilarPostResponse> getSimilarRecipes(Long recipeId, List<String> ingredients, Integer limit) {
    // tìm post gốc
    Post targetPost = postRepo.findByRecipeIdAndStatusWithIngredients(recipeId, PostStatus.APPROVED)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND));

    Set<String> targetIngredients = normalizeIngredients(targetPost.getRecipeIngredientKeywords());
    if (targetIngredients.isEmpty()) {
      return List.of();
    }

    // Chuẩn hóa danh sách nguyên liệu lọc do client truyền lên.
    Set<String> filterIngredients = normalizeIngredients(ingredients);

    List<Post> candidates = postRepo.findAllSimilarCandidates(recipeId, PostStatus.APPROVED);

    List<SimilarCandidateScore> topCandidates = candidates.stream()
      .map(candidate -> evaluateSimilarCandidate(targetIngredients, filterIngredients, candidate))
      .filter(Objects::nonNull)
      .sorted(Comparator.comparingDouble(SimilarCandidateScore::score)
        .reversed()
        .thenComparing(result -> result.post().getId(), Comparator.nullsLast(Comparator.reverseOrder())))
      .limit(limit)
      .toList();

    log.info("Similar recipes result for recipeId={}: {}", recipeId,
      topCandidates.stream()
        .map(result -> result.post().getTitle() + " (score=" + result.score() + ")")
        .toList());

    return topCandidates.stream()
      .map(result -> SimilarPostResponse.builder()
        .postId(result.post().getId())
        .recipeId(result.post().getRecipeId())
        .title(result.post().getTitle())
        .imageUrl(result.post().getImageUrl())
        .matchedIngredients(result.matchedIngredients())
        .build())
      .toList();
  }

  private SimilarCandidateScore evaluateSimilarCandidate(
    Set<String> targetIngredients,
    Set<String> filterIngredients,
    Post candidate
  ) {
    Set<String> candidateIngredients = normalizeIngredients(candidate.getRecipeIngredientKeywords());
    //lọc
    if (!matchesFilterIngredients(filterIngredients, candidateIngredients)) {
      return null;
    }

    //Tính điểm tương đồng
    List<String> matchedIngredients = getMatchedIngredients(targetIngredients, candidateIngredients);
    double weightedScore = matchedIngredients.stream()
      .mapToDouble(this::getIngredientWeight)
      .sum();
    if (weightedScore <= 0) {
      return null;
    }

    return new SimilarCandidateScore(candidate, weightedScore, matchedIngredients);
  }

  private List<String> getMatchedIngredients(Set<String> targetIngredients, Set<String> candidateIngredients) {
    return candidateIngredients.stream()
      .filter(candidateIngredient -> targetIngredients.stream()
        .anyMatch(targetIngredient -> isSoftIngredientMatch(targetIngredient, candidateIngredient)))
      .sorted()
      .toList();
  }

  private boolean matchesFilterIngredients(Set<String> filterIngredients, Set<String> candidateIngredients) {
    if (filterIngredients.isEmpty()) {
      return true;
    }

    return filterIngredients.stream()
      .anyMatch(filterIngredient -> candidateIngredients.stream()
        .anyMatch(candidateIngredient -> isSoftIngredientMatch(filterIngredient, candidateIngredient)));
  }

  // Trả về trọng số của từng nguyên liệu 
  private double getIngredientWeight(String ingredient) {
    if (matchesReducedIngredient(ingredient, STRONG_REDUCED_INGREDIENTS)) {
      return STRONG_REDUCED_WEIGHT;
    }
    if (matchesReducedIngredient(ingredient, LIGHT_REDUCED_INGREDIENTS)) {
      return LIGHT_REDUCED_WEIGHT;
    }
    return 1.0d;
  }

  // Kiểm tra nguyên liệu hiện tại có khớp mềm với nhóm nguyên liệu giảm trọng số hay không.
  private boolean matchesReducedIngredient(String ingredient, Set<String> reducedIngredients) {
    String normalizedIngredient = normalizeIngredient(ingredient);
    return reducedIngredients.stream()
      .map(this::normalizeIngredient)
      .anyMatch(reducedIngredient -> isSoftIngredientMatch(normalizedIngredient, reducedIngredient));
  }

  // So khớp mềm hai chuỗi nguyên liệu theo kiểu một bên chứa bên còn lại.
  private boolean isSoftIngredientMatch(String left, String right) {
    return left.contains(right) || right.contains(left);
  }

  private Set<String> normalizeIngredients(Set<String> ingredients) {
    return Optional.ofNullable(ingredients)
      .orElse(Set.of())
      .stream()
      .filter(Objects::nonNull)
      .map(this::normalizeIngredient)
      .filter(value -> !value.isBlank())
      .collect(Collectors.toSet());
  }

  private Set<String> normalizeIngredients(List<String> ingredients) {
    return Optional.ofNullable(ingredients)
      .orElse(List.of())
      .stream()
      .filter(Objects::nonNull)
      .map(this::normalizeIngredient)
      .filter(value -> !value.isBlank())
      .collect(Collectors.toSet());
  }

  // Bỏ khoảng trắng thừa và chuyển về chữ thường để so khớp nhất quán nhưng vẫn giữ dấu tiếng Việt.
  private String normalizeIngredient(String ingredient) {
    //   String withoutVietnameseD = trimmed
    //   .replace('đ', 'd')
    //   .replace('Đ', 'd');
    // String normalized = Normalizer.normalize(withoutVietnameseD, Normalizer.Form.NFD);
    // return normalized.replaceAll("\\p{M}+", "");
    return ingredient.trim().toLowerCase();
  }
}
