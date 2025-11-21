package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.StoryReactionRequest;
import com.eefood.reactionservice.dto.response.StoryReactionResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.mapper.StoryReactionMapper;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.StoryReaction;
import com.eefood.reactionservice.model.StoryReactionCount;
import com.eefood.reactionservice.model.StoryReactionCountId;
import com.eefood.reactionservice.repository.StoryReactionCountRepository;
import com.eefood.reactionservice.repository.StoryReactionRepository;
import com.eefood.reactionservice.repository.StoryRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.NotificationUtils;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryReactionService {
    private final StoryReactionRepository storyReactionRepository;
    private final StoryRepository storyRepository;
    private final StoryReactionCountRepository storyReactionCountRepository;
    private final IamClient iamClient;
    private final NotificationUtils notificationUtils;
    private final StoryReactionMapper storyReactionMapper;
    private final SecurityUtil securityUtil;

    @Transactional
    public StoryReactionResponse reactToStory(StoryReactionRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        UserInfo userInfo = iamClient.getUserInfo(userId).getData();

        Story story = storyRepository.findByIdAndIsDeletedFalse(request.getStoryId())
                .orElseThrow(() -> new RuntimeException("Story not found"));

        StoryReaction existing = storyReactionRepository
                .findByStoryIdAndUserId(story.getId(), userId)
                .orElse(null);

        // Nếu react cùng icon => gỡ reaction
        if (existing != null && existing.getReactionType() == request.getReactionType()) {
            storyReactionRepository.delete(existing);
            decreaseCount(story, existing.getReactionType());
            return null;
        }

        // Nếu react khác icon => cập nhật lại
        if (existing != null) {
            decreaseCount(story, existing.getReactionType());
            existing.setReactionType(request.getReactionType());
            StoryReaction updated = storyReactionRepository.save(existing);
            increaseCount(story, request.getReactionType());

            sendNotification(story, userId, userInfo, request.getReactionType());

            return storyReactionMapper.toResponse(updated);
        }

        // Nếu chưa từng react => tạo mới
        StoryReaction newReaction = StoryReaction.builder()
                .story(story)
                .userId(userId)
                .reactionType(request.getReactionType())
                .build();

        StoryReaction saved = storyReactionRepository.save(newReaction);
        increaseCount(story, request.getReactionType());

        sendNotification(story, userId, userInfo, request.getReactionType());

        return storyReactionMapper.toResponse(saved);
    }

    @Transactional
    public void removeReaction(Long storyId) {
        Long userId = securityUtil.getCurrentUserId();
        storyReactionRepository.findByStoryIdAndUserId(storyId, userId)
                .ifPresent(reaction -> {
                    storyReactionRepository.delete(reaction);
                    decreaseCount(reaction.getStory(), reaction.getReactionType());
                });
    }

    private void sendNotification(Story story, Long userId, UserInfo userInfo, ReactionType type) {
        if (!userId.equals(story.getUserId())) {
            notificationUtils.sendReactionNotification(
                    story.getUserId(),
                    userInfo.getUsername(),
                    type,
                    userInfo.getAvatarUrl(),
                    true,
                    "/story/" + story.getId(),
                    story.getContentUrl()
            );
        }
    }

    private void increaseCount(Story story, ReactionType type) {
        StoryReactionCountId id = new StoryReactionCountId(story.getId(), type);

        StoryReactionCount count = storyReactionCountRepository.findById(id)
                .orElse(StoryReactionCount.builder()
                        .story(story)
                        .reactionType(type)
                        .count(0L)
                        .build());

        count.setCount(count.getCount() + 1);
        storyReactionCountRepository.save(count);
    }

    private void decreaseCount(Story story, ReactionType type) {
        StoryReactionCountId id = new StoryReactionCountId(story.getId(), type);
        storyReactionCountRepository.findById(id)
                .ifPresent(c -> {
                    c.setCount(Math.max(0, c.getCount() - 1));
                    storyReactionCountRepository.save(c);
                });
    }

    public Long getTotalReactions(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        return storyReactionCountRepository.findAllByStory(story)
                .stream()
                .mapToLong(StoryReactionCount::getCount)
                .sum();
    }

    public Page<StoryReactionResponse> getUsersReactedStory(Long storyId, Pageable pageable) {
        storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        Page<StoryReaction> page = storyReactionRepository.findByStoryId(storyId, pageable);

        return page.map(r -> {
            UserInfo author = iamClient.getUserInfo(r.getUserId()).getData();

            return StoryReactionResponse.builder()
                    .id(r.getId())
                    .storyId(storyId)
                    .userId(r.getUserId())
                    .reactionType(r.getReactionType())
                    .avatarUrl(author.getAvatarUrl())
                    .username(author.getUsername())
                    .createdAt(r.getCreatedAt())
                    .build();
        });
    }
}
