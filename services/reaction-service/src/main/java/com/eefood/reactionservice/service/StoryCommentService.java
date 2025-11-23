package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.StoryCommentRequest;
import com.eefood.reactionservice.dto.response.StoryCommentResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.mapper.StoryCommentMapper;
import com.eefood.reactionservice.model.Story;
import com.eefood.reactionservice.model.StoryComment;
import com.eefood.reactionservice.repository.StoryCommentRepository;
import com.eefood.reactionservice.repository.StoryRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryCommentService {
    private final StoryRepository storyRepository;
    private final StoryCommentRepository storyCommentRepository;
    private final StoryCommentMapper storyCommentMapper;
    private final SecurityUtil securityUtil;
    private final IamClient iamClient;

    @Transactional
    public StoryCommentResponse addComment(StoryCommentRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        Story story = storyRepository
                .findByIdAndIsDeletedFalse(request.getStoryId())
                .orElseThrow(() -> new RuntimeException("Story not found"));

        StoryComment storyComment = StoryComment.builder()
                .story(story)
                .userId(userId)
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();

        // Xử lý reply comment với giới hạn 2 cấp
        if (request.getParentId() != null) {
            StoryComment parentComment = storyCommentRepository
                    .findByIdAndIsDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));

            // Kiểm tra parent comment có thuộc story này không
            if (!parentComment.getStory().getId().equals(request.getStoryId())) {
                throw new RuntimeException("Parent comment does not belong to this story");
            }

            // Nếu parent là comment cấp 2 (đã có parent), thì gán về parent cấp 1
            if (parentComment.getParentComment() != null) {
                storyComment.setParentComment(parentComment.getParentComment());
            } else {
                // Parent là cấp 1, gán bình thường
                storyComment.setParentComment(parentComment);
            }
        }

        StoryComment saved = storyCommentRepository.save(storyComment);
        // Lấy thông tin user từ IAM và gán vào response
        StoryCommentResponse response = storyCommentMapper.toResponse(saved);
        enrichCommentWithUserInfo(response, userId);

        // Thêm parentId nếu có
        if (saved.getParentComment() != null) {
            response.setParentId(saved.getParentComment().getId());
        }

        return response;
    }

    @Transactional
    public StoryCommentResponse updateComment(StoryCommentRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        StoryComment comment = storyCommentRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Chỉ chủ comment được sửa
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You cannot update another user's comment");
        }

        comment.setMessage(request.getMessage());

        StoryComment updated = storyCommentRepository.save(comment);
        // Lấy thông tin user từ IAM và gán vào response
        StoryCommentResponse response = storyCommentMapper.toResponse(updated);
        enrichCommentWithUserInfo(response, userId);

        // Thêm parentId nếu có
        if (updated.getParentComment() != null) {
            response.setParentId(updated.getParentComment().getId());
        }

        return response;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Long userId = securityUtil.getCurrentUserId();
        StoryComment comment = storyCommentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found or deleted"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You cannot delete another user's comment");
        }

        // Xóa mềm comment hiện tại
        comment.setIsDeleted(true);
        storyCommentRepository.save(comment);

        // Nếu là comment cha (không có parent), xóa mềm tất cả reply của nó
        if (comment.getParentComment() == null) {
            List<StoryComment> replies = storyCommentRepository.findByParentCommentIdAndIsDeletedFalse(commentId);
            replies.forEach(reply -> {
                reply.setIsDeleted(true);
                storyCommentRepository.save(reply);
            });
        }
    }

    // Lấy comments gốc (cấp 1) của story
    public Page<StoryCommentResponse> getComments(Long storyId, Pageable pageable) {
        Page<StoryComment> page = storyCommentRepository.findByStoryIdAndParentCommentIsNullAndIsDeletedFalse(storyId, pageable);
        return mapCommentsToResponse(page, pageable);
    }

    // Lấy reply comments (cấp 2) của một comment
    public Page<StoryCommentResponse> getReplyComments(Long parentId, Pageable pageable) {
        // Kiểm tra parent comment có tồn tại không
        storyCommentRepository.findByIdAndIsDeletedFalse(parentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));

        Page<StoryComment> page = storyCommentRepository.findByParentCommentIdAndIsDeletedFalse(parentId, pageable);
        return mapCommentsToResponse(page, pageable);
    }

    private void enrichCommentWithUserInfo(StoryCommentResponse response, Long userId) {
        try {
            List<UserInfo> userInfos = iamClient.getUserInfoBatch(List.of(userId)).getData();
            if (userInfos != null && !userInfos.isEmpty()) {
                UserInfo userInfo = userInfos.get(0);
                response.setUsername(userInfo.getUsername());
                response.setEmail(userInfo.getEmail());
                response.setAvatarUrl(userInfo.getAvatarUrl());
            }
        } catch (Exception e) {
            // Log error nhưng không throw exception để không làm fail request
            System.err.println("Failed to fetch user info: " + e.getMessage());
            // Set default values
            response.setUsername("Unknown");
            response.setEmail("");
            response.setAvatarUrl(null);
        }
    }

    private Page<StoryCommentResponse> mapCommentsToResponse(Page<StoryComment> page, Pageable pageable) {
        if (page.isEmpty()) {
            return Page.empty();
        }

        // Lấy userIds
        List<Long> userIds = page.getContent().stream()
                .map(StoryComment::getUserId)
                .distinct()
                .toList();

        // Lấy thông tin user từ IAM
        List<UserInfo> userInfos = iamClient.getUserInfoBatch(userIds).getData();

        Map<Long, UserInfo> userInfoMap = userInfos.stream()
                .collect(Collectors.toMap(UserInfo::getId, u -> u));

        // Map sang DTO
        List<StoryCommentResponse> dtoList = page.getContent().stream()
                .map(comment -> {
                    StoryCommentResponse dto = storyCommentMapper.toResponse(comment);

                    UserInfo info = userInfoMap.get(comment.getUserId());
                    if (info != null) {
                        dto.setUsername(info.getUsername());
                        dto.setEmail(info.getEmail());
                        dto.setAvatarUrl(info.getAvatarUrl());
                    }

                    // Thêm thông tin parent comment nếu có
                    if (comment.getParentComment() != null) {
                        dto.setParentId(comment.getParentComment().getId());
                    }

                    return dto;
                }).toList();

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }
}
