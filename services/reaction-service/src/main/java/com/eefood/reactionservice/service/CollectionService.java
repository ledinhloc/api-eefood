package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.response.CollectionResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.model.Collection;
import com.eefood.reactionservice.model.CollectionPost;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.CollectionPostRepository;
import com.eefood.reactionservice.repository.CollectionRepository;
import com.eefood.reactionservice.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CollectionService {
  private final CollectionRepository collectionRepo;
  private final CollectionPostRepository collectionPostRepo;
  private final PostRepository postRepo;
  private final CollectionMapper mapper;

  public CollectionResponse  create(Long userId, String name){
    if (name == null || name.isBlank()) {
      throw ExceptionUtil.badRequest(ErrorMessage.INVALID_REQUEST);
    }

    Collection collection = Collection.builder()
      .userId(userId)
      .name(name)
      .coverImageUrl(null)
      .isDeleted(false)
      .build();

    return mapper.toDto(collectionRepo.save(collection));
  }

  /** Update collection */
  public CollectionResponse update(Long id, String name, String coverImageUrl) {
    Collection collection = collectionRepo.findById(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));

    if (name != null && !name.isBlank())
      collection.setName(name);
    if (coverImageUrl != null)
      collection.setCoverImageUrl(coverImageUrl);

    return mapper.toDto(collectionRepo.save(collection));
  }

  public void delete(Long id) {
    Collection collection = collectionRepo.findById(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));
    collection.setIsDeleted(true);
    collectionRepo.save(collection);
  }

  public List<CollectionResponse> getByUser(Long userId) {
    List<Collection> collections = collectionRepo.findAllByUserIdAndIsDeletedFalse(userId);
    return collections.stream()
      .map(mapper::toDto)
      .toList();
  }

  public CollectionResponse getById(Long id){
    Collection collection = collectionRepo.findById(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));
    return mapper.toDto(collection);
  }

  public void addPost(Long collectionId, Long postId){
    if (collectionPostRepo.existsByCollectionIdAndPostId(collectionId, postId)) {
      throw ExceptionUtil.badRequest(ErrorMessage.ALREADY_EXISTS);
    }

    Collection collection = collectionRepo.findById(collectionId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));

    Post post = postRepo.findById(postId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND));

    // Lưu record trung gian
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
    Collection collection = collectionRepo.findById(collectionId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.COLLECTION_NOT_FOUND));

    if (!collectionPostRepo.existsByCollectionIdAndPostId(collectionId, postId)) {
      throw ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND);
    }

    // Xóa mối quan hệ
    collectionPostRepo.deleteByCollectionIdAndPostId(collectionId, postId);

    // Tìm bài post cuối cùng còn lại (nếu có)
    Post lastPost = collectionPostRepo.findLastPostByCollectionId(collectionId)
      .orElse(null);

    if (lastPost != null) {
      collection.setCoverImageUrl(lastPost.getImageUrl());
    } else {
      collection.setCoverImageUrl(null);
    }

    collectionRepository.save(collection);
  }



}
