package com.eefood.reactionservice.unit.service;

import com.eefood.reactionservice.dto.request.StoryRequest;
import com.eefood.reactionservice.dto.response.StoryResponse;
import com.eefood.reactionservice.mapper.StoryMapper;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.repository.story.StoryRepository;
import com.eefood.reactionservice.service.story.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private StoryService storyService;

    @Mock
    private StoryMapper storyMapper;

    private StoryRequest storyRequest;

    @BeforeEach
    void setUp() {
        storyRequest = StoryRequest.builder()
                .type("IMAGE")
                .userId(1L)
                .contentUrl("http://image.png")
                .build();
    }

    @Test
    void testCaseCreateStory_success() {
        Story savedStory = Story.builder()
                .id(100L)
                .type("IMAGE")
                .userId(1L)
                .contentUrl("http://image.png")
                .expiredAt(LocalDateTime.now().plusHours(24))
                .build();

        StoryResponse response = new StoryResponse();
        response.setId(100L);

        when(storyRepository.save(any(Story.class))).thenReturn(savedStory);
        when(storyMapper.toStoryResponse(any(Story.class))).thenReturn(response);

        // when
        StoryResponse result = storyService.createStory(storyRequest);

        // then
        assertNotNull(result);
        assertEquals(100L, result.getId());

        verify(storyRepository, times(1)).save(any(Story.class));
        verify(storyMapper, times(1)).toStoryResponse(any(Story.class));
    }

    @Test
    void testCaseCreateStory_shouldSetExpiredAtPlus24Hours() {
        ArgumentCaptor<Story> storyCaptor = ArgumentCaptor.forClass(Story.class);

        when(storyRepository.save(any(Story.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storyMapper.toStoryResponse(any(Story.class)))
                .thenReturn(new StoryResponse());

        LocalDateTime beforeCall = LocalDateTime.now();

        // when
        storyService.createStory(storyRequest);

        LocalDateTime afterCall = LocalDateTime.now();

        // then
        verify(storyRepository).save(storyCaptor.capture());
        Story capturedStory = storyCaptor.getValue();

        assertNotNull(capturedStory.getExpiredAt());
        assertTrue(
                capturedStory.getExpiredAt().isAfter(beforeCall.plusHours(23)) &&
                        capturedStory.getExpiredAt().isBefore(afterCall.plusHours(25))
        );
    }

    @Test
    void testCaseCreateStory_mapperReturnsNull_shouldReturnNull() {
        // given

        when(storyRepository.save(any(Story.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storyMapper.toStoryResponse(any(Story.class))).thenReturn(null);

        // when
        StoryResponse result = storyService.createStory(storyRequest);

        // then
        assertNull(result);
        verify(storyRepository).save(any(Story.class));
    }
}
