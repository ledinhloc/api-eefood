package com.eefood.reactionservice.service;

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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import com.eefood.reactionservice.dto.response.PostCollectionResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionService {
  private final CollectionRepository collectionRepo;
  private final CollectionPostRepository collectionPostRepo;
  private final PostRepository postRepo;
  private final CollectionMapper mapper;
  private final SecurityUtil securityUtil;
  private final IamClient iamClient;

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

    return mapper.toDto(collectionRepo.save(collection));
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
    List<Collection> collections = collectionRepo.findAllByUserIdAndIsDeletedFalse(userId);
    if (collections.isEmpty()) return List.of();

    //mapper sang dto
    List<CollectionResponse> responses = collections.stream()
      .map(mapper::toDto)
      .toList();

    // SẮP XẾP CÁC POST TRONG MỖI COLLECTION
    for (CollectionResponse col : responses) {
      if (col.getPosts() != null) {
        col.getPosts().sort((a, b) -> {
          // Ưu tiên createdAt (mới nhất trước)
          int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
          if (cmp != 0) return cmp;
          // Nếu trùng -> sắp theo title (A-Z)
          String t1 = Optional.ofNullable(a.getTitle()).orElse("");
          String t2 = Optional.ofNullable(b.getTitle()).orElse("");
          return t1.compareToIgnoreCase(t2);
        });
      }
    }

    // sort theo thoi gian cap nhat
    responses.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));

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
    CollectionResponse response = mapper.toDto(collection);
    //Nếu collection không có post thì return luôn
    if (response.getPosts() == null || response.getPosts().isEmpty()) {
      return response;
    }
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
