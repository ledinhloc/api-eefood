package com.eefood.reactionservice.service.story;

import com.eefood.reactionservice.dto.request.StoryCollectionRequest;
import com.eefood.reactionservice.dto.response.StoryCollectionResponse;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.mapper.StoryCollectionMapper;
import com.eefood.reactionservice.mapper.StoryMapper;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.StoryCollection;
import com.eefood.reactionservice.model.StoryCollectionItem;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.story.StoryCollectionItemRepository;
import com.eefood.reactionservice.repository.story.StoryCollectionRepository;
import com.eefood.reactionservice.repository.story.StoryRepository;
import com.eefood.reactionservice.repository.story.StoryViewRepository;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryCollectionService {
    private final StoryCollectionRepository storyCollectionRepository;
    private final StoryCollectionItemRepository storyCollectionItemRepository;
    private final StoryViewRepository storyViewRepository;
    private final StoryRepository storyRepository;
    private final SecurityUtil securityUtil;
    private final StoryCollectionMapper storyCollectionMapper;
    private final StoryMapper storyMapper;
    private final StorySettingService storySettingService;

    public StoryCollectionResponse create(StoryCollectionRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        StoryCollection sc = StoryCollection.builder()
                .userId(userId)
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .build();

        storyCollectionRepository.save(sc);

        return storyCollectionMapper.toResponse(sc);
    }

    public StoryCollectionResponse update(Long id, StoryCollectionRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        StoryCollection sc = storyCollectionRepository.findById(id).orElseThrow(()-> new RuntimeException("Story collection not found"));

        if(!sc.getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to update story collection");
        }

        sc.setName(request.getName());
        sc.setImageUrl(request.getImageUrl());
        sc.setDescription(request.getDescription());
        storyCollectionRepository.save(sc);
        return storyCollectionMapper.toResponse(sc);
    }

    public void delete(Long id) {
        Long userId = securityUtil.getCurrentUserId();
        StoryCollection sc = storyCollectionRepository.findById(id).orElseThrow(()-> new RuntimeException("Story collection not found"));
        if(!sc.getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to update story collection");
        }

        sc.setIsDeleted(true);
        List<StoryCollectionItem> items = storyCollectionItemRepository.findByCollectionId(id);
        sc.getItems().removeAll(items);
        storyCollectionRepository.save(sc);
    }

    public Page<StoryCollectionResponse> getUserCollections(Long userId, Pageable pageable) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Page<StoryCollection> page  = storyCollectionRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        List<StoryCollectionResponse> filtered = page.getContent().stream()
                .filter(collection -> canViewCollection(collection, currentUserId))
                .map(storyCollectionMapper::toResponse)
                .toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    private boolean canViewCollection(StoryCollection collection, Long currentUserId) {
        return collection.getItems().stream()
                .map(StoryCollectionItem::getStory)
                .filter(story -> story != null && !Boolean.TRUE.equals(story.getIsDeleted()))
                .allMatch(story ->
                        storySettingService.canViewStory(story.getUserId(), currentUserId)
                );
    }

    public List<StoryResponse> getStoriesInCollection(Long collectionId, Long userId) {
        List<StoryCollectionItem> items = storyCollectionItemRepository
                .findByCollectionIdOrderByCreatedAtDesc(collectionId);
        return items.stream()
                .map(StoryCollectionItem::getStory)
                .filter(s -> storySettingService.canViewStory(s.getUserId(), userId))
                .filter(story -> story != null && Boolean.FALSE.equals(story.getIsDeleted()))
                .map(story -> {
                    StoryResponse dto = storyMapper.toStoryResponse(story);
                    dto.setViewed(storyViewRepository.existsByStoryIdAndUserId(story.getId(), userId));
                    return dto;
                })
                .toList();
    }

    public void addStoryToCollection(Long collectionId, Long storyId) {
        Long userId = securityUtil.getCurrentUserId();
        StoryCollection sc = storyCollectionRepository.findById(collectionId).orElseThrow(()-> new RuntimeException("Story collection not found"));
        Story story = storyRepository.findById(storyId).orElseThrow(()-> new RuntimeException("Story not found"));
        if(!sc.getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to update story collection");
        }

        boolean exists = sc.getItems().stream()
                .anyMatch(i -> i.getStory().getId().equals(storyId));
        if (exists) {
            throw new RuntimeException("Story already in collection");
        }

        StoryCollectionItem item = StoryCollectionItem.builder()
                .collection(sc)
                .story(story)
                .build();

        sc.getItems().add(item);
        storyCollectionRepository.save(sc);
    }

    public void removeStoryFromCollection(Long collectionId, Long storyId) {
        Long userId = securityUtil.getCurrentUserId();
        StoryCollection sc = storyCollectionRepository.findById(collectionId).orElseThrow(()-> new RuntimeException("Story collection not found"));
        if(!sc.getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to update story collection");
        }

        StoryCollectionItem item = sc.getItems().stream()
                .filter(i -> i.getStory().getId().equals(storyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Story not in collection"));

        sc.getItems().remove(item);
        storyCollectionRepository.save(sc);
    }

    public List<StoryCollectionResponse> getCollectionsContainingStory(Long storyId) {
        Long userId = securityUtil.getCurrentUserId();
        List<StoryCollectionItem> items = storyCollectionItemRepository
                .findByCollectionUserIdAndStoryIdAndCollectionIsDeletedFalseOrderByCreatedAtDesc(userId, storyId);

        return items.stream()
                .map(StoryCollectionItem::getCollection)
                .filter(collection -> collection != null && Boolean.FALSE.equals(collection.getIsDeleted()))
                .distinct()
                .map(storyCollectionMapper::toResponse)
                .toList();
    }
}
