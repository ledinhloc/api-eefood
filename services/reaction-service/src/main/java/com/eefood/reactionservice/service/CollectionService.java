package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.PostCollectionsRequest;
import com.eefood.reactionservice.dto.response.CollectionResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.model.Collection;
import com.eefood.reactionservice.model.CollectionPost;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.CollectionPostRepository;
import com.eefood.reactionservice.repository.CollectionRepository;
import com.eefood.reactionservice.repository.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import com.eefood.reactionservice.dto.response.PostCollectionResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {
  private final CollectionRepository collectionRepo;
  private final CollectionPostRepository collectionPostRepo;
  private final PostRepository postRepo;
  private final CollectionMapper mapper;
  private final SecurityUtil securityUtil;
  private final IamClient iamClient;

  @Transactional
  public List<CollectionResponse> updatePostCollections(PostCollectionsRequest request) {
    Long postId = request.getPostId();
    List<Long> collectionIds = request.getCollectionIds();

    //validate
    if (postId == null || collectionIds == null)
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);

    Post post = postRepo.findByIdAndIsDeletedFalse(postId);
    if (post == null) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    //Lấy userId
    Long currentUserId = securityUtil.getCurrentUserId();

    Set<Long> oldIds = collectionPostRepo.findCollectionIdsByPostIdAndUserId(postId, currentUserId);
    Set<Long> newIds = new HashSet<>(request.getCollectionIds());

    // Tìm các collection cần thêm hoặc xóa
    Set<Long> toAdd = newIds.stream()
      .filter(id -> !oldIds.contains(id))
        .collect(Collectors.toSet());
    Set<Long> toRemove = oldIds.stream()
      .filter(id -> !newIds.contains(id))
        .collect(Collectors.toSet());

    //Kiểm tra toàn bộ quyền trước khi xóa
    if (!toRemove.isEmpty()) {
      List<Collection> toRemoveCollections = collectionRepo.findAllByIdInAndIsDeletedFalse(toRemove);

      // Nếu thiếu collection nào => lỗi
      if (toRemoveCollections.size() != toRemove.size()) {
        throw ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND);
      }

      // Nếu có bất kỳ collection nào không thuộc user hiện tại => lỗi -> rollback
      boolean hasUnauthorized = toRemoveCollections.stream()
        .anyMatch(c -> !c.getUserId().equals(currentUserId));

      if (hasUnauthorized) {
        throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
      }
      collectionPostRepo.deleteByPostIdAndCollectionIdIn(postId, toRemove);
      // Cập nhật lại cover cho từng collection bị xóa post
      for (Collection col : toRemoveCollections) {
        Post lastPost = collectionPostRepo.findLastPostByCollectionId(col.getId()).orElse(null);
        col.setCoverImageUrl(lastPost != null ? lastPost.getImageUrl() : null);
        collectionRepo.save(col);
      }
    }
    //xu ly them
    for(Long id: toAdd){
      Collection collection = collectionRepo.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));

      //kiem tra quyen
      if(!collection.getUserId().equals(currentUserId)){
        throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
      }
      //luu post collection
      collectionPostRepo.save(CollectionPost.builder()
          .post(post)
          .collection(collection)
        .build());

      // Cập nhật cover = ảnh bài mới nhất
      collection.setCoverImageUrl(post.getImageUrl());
      collectionRepo.save(collection);
    }
    // Trả về toàn bộ collection của user
    return getByUser(currentUserId);
  }


  public CollectionResponse  create(Long userId, String name){
    if (userId == null || name == null || name.isBlank()) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    //collection cùng tên đã tồn tại cho user này chưa
    boolean exists = collectionRepo.existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(userId, name);
    if (exists) {
      throw ExceptionUtil.badRequest(ErrorMessage.DUPLICATE_COLLECTION_NAME);
    }

    Collection collection = Collection.builder()
      .userId(userId)
      .name(name.trim())
      .coverImageUrl(null)
      .isDeleted(false)
      .build();

    return mapper.toDto(collectionRepo.save(collection));
  }

  /** Update collection */
  public CollectionResponse update(Long id, String name, String coverImageUrl) {
    Collection collection = collectionRepo.findById(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));

    validateOwner(collection);

    if (name != null && !name.isBlank()){
      // check duplicate name
      boolean duplicate = collectionRepo.existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(securityUtil.getCurrentUserId(), name)
        && !Objects.equals(collection.getName(), name);
      if (duplicate) {
        throw ExceptionUtil.badRequest(ErrorMessage.DUPLICATE_COLLECTION_NAME);
      }
      collection.setName(name);
    }

    if (coverImageUrl != null && !coverImageUrl.isBlank())
      collection.setCoverImageUrl(coverImageUrl.trim());

    collectionRepo.save(collection);
    return getById(id);
  }

  public void delete(Long id) {
    Collection collection = collectionRepo.findById(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));
    validateOwner(collection);

    if (collection.getIsDeleted()) {
      throw ExceptionUtil.badRequest(ErrorMessage.ALREADY_DELETED);
    }

    collection.setIsDeleted(true);
    collectionRepo.save(collection);
  }

  /** Get all collections by user */
  public List<CollectionResponse> getByUser(Long userId) {
    if (userId == null) throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    List<Collection> collections = collectionRepo.findAllByUserIdAndIsDeletedFalseOrderByIdDesc(userId);
    if (collections.isEmpty()) return List.of();

    //sort col post giam dan
    for(Collection collection: collections){
      if (collection.getCollectionPosts() != null && !collection.getCollectionPosts().isEmpty()) {
        collection.getCollectionPosts().sort(
          Comparator.comparing(CollectionPost::getId).reversed()
        );
      }
    }

    //mapper sang dto
    List<CollectionResponse> responses = collections.stream()
      .map(mapper::toDto)
      .toList();

    //lay danh sach userId trong post
    Set<Long> userIds = responses.stream()
      .flatMap(c -> c.getPosts().stream())
      .map(PostCollectionResponse::getUserId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    if (userIds.isEmpty()) return responses;
    //gọi iam lay thong tin user
    ResponseData<List<UserInfo>> iamResponse = iamClient.getUserInfoBatch(new ArrayList<>(userIds));
    if (iamResponse == null || iamResponse.getData() == null)
      return responses;

    //map user theo Id
    Map<Long, UserInfo> userInfoMap = iamResponse.getData().stream()
      .collect(Collectors.toMap(UserInfo::getId, u -> u));

    //merge thong tin user vao response
    for (CollectionResponse col : responses) {
      for (PostCollectionResponse post : col.getPosts()) {
        UserInfo info = userInfoMap.get(post.getUserId());
        if (info != null) {
          post.setUsername(info.getUsername());
          post.setEmail(info.getEmail());
          post.setAvatarUrl(info.getAvatarUrl());
        }
      }
    }

    return responses;
  }

  /** Get single collection by ID */
  public CollectionResponse getById(Long id){
    if (id == null)
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    //tim collection
    Collection collection = collectionRepo.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));

    validateOwner(collection);
    // Sắp xếp ngay trong entity trước khi map
    if (collection.getCollectionPosts() != null && !collection.getCollectionPosts().isEmpty()) {
      collection.getCollectionPosts().sort(
        Comparator.comparing(CollectionPost::getId).reversed()
      );
    }

    CollectionResponse response = mapper.toDto(collection);

    // Lấy danh sách userId trong các bài post
    Set<Long> userIds = response.getPosts().stream()
      .map(PostCollectionResponse::getUserId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    if (userIds.isEmpty()) {
      return response;
    }
    //Gọi IAM để lấy thông tin user
    ResponseData<List<UserInfo>> iamResponse =
      iamClient.getUserInfoBatch(new ArrayList<>(userIds));

    if (iamResponse == null || iamResponse.getData() == null) {
      return response;
    }
    // Map userId → UserInfo
    Map<Long, UserInfo> userInfoMap = iamResponse.getData().stream()
      .collect(Collectors.toMap(UserInfo::getId, u -> u));

    // Merge thông tin user vào từng post
    for (PostCollectionResponse post : response.getPosts()) {
      UserInfo info = userInfoMap.get(post.getUserId());
      if (info != null) {
        post.setUsername(info.getUsername());
        post.setEmail(info.getEmail());
        post.setAvatarUrl(info.getAvatarUrl());
      }
    }

    return response;
  }

  public void addPost(Long collectionId, Long postId){
    Collection collection = collectionRepo.findById(collectionId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));
    validateOwner(collection);

    if (collectionPostRepo.existsByCollectionIdAndPostId(collectionId, postId)) {
      throw ExceptionUtil.badRequest(ErrorMessage.ALREADY_EXISTS);
    }

    Post post = postRepo.findById(postId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND));

    // Lưu record
    CollectionPost collectionPost = CollectionPost.builder()
      .collection(collection)
      .post(post)
      .build();
    collectionPostRepo.save(collectionPost);

    // Cập nhật coverImageUrl = ảnh của post mới nhất
    collection.setCoverImageUrl(post.getImageUrl());
    collectionRepo.save(collection);
  }

  /**
   * Remove post khỏi collection
   * => Nếu collection còn post khác, dùng ảnh bài cuối cùng làm cover.
   * => Nếu không còn post, cover = null.
   */
  public void removePost(Long collectionId, Long postId) {
    Collection collection = collectionRepo.findByIdAndIsDeletedFalse(collectionId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));
    validateOwner(collection);

    if (!collectionPostRepo.existsByCollectionIdAndPostId(collectionId, postId)) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    // Xóa mối quan hệ
    collectionPostRepo.deleteByCollectionIdAndPostId(collectionId, postId);

    // Tìm bài post cuối cùng còn lại (nếu có)
    Post lastPost = collectionPostRepo.findLastPostByCollectionId(collectionId)
      .orElse(null);
    collection.setCoverImageUrl(lastPost != null ? lastPost.getImageUrl() : null);

    collectionRepo.save(collection);
  }

  private void validateOwner(Collection collection) {
    if (!Objects.equals(collection.getUserId(), securityUtil.getCurrentUserId())) {
      throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
    }
  }
}
