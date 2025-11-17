package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.StoryRequest;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.mapper.StoryMapper;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

    public StoryResponse createStory(StoryRequest storyRequest) {
        Story story = Story.builder()
                .type(storyRequest.getType())
                .userId(storyRequest.getUserId())
                .contentUrl(storyRequest.getContentUrl())
                .expiredAt(LocalDateTime.now().plusHours(24))
                .build();
        storyRepository.save(story);
        return storyMapper.toStoryResponse(story);
    }

    public StoryResponse updateStory(StoryRequest storyRequest) {
        Story story = storyRepository.findByIdAndIsDeletedFalse(storyRequest.getId()).orElseThrow(() -> new RuntimeException("Story not found or deleted"));
        if (story != null) {
            story.setExpiredAt(LocalDateTime.now().plusHours(24));
            story.setContentUrl(storyRequest.getContentUrl());
            story.setType(storyRequest.getType());
            storyRepository.save(story);
        }
        return storyMapper.toStoryResponse(story);
    }

    public void softDeleteStory(Long id) {
        Story story = storyRepository.findByIdAndIsDeletedFalse(id).orElseThrow(() -> new RuntimeException("Story not found or deleted"));
        story.setIsDeleted(true);
        storyRepository.save(story);
    }
}
