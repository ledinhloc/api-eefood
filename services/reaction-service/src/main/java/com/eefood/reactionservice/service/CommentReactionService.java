package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.CommentReactionRequest;
import com.eefood.reactionservice.dto.response.CommentReactionResponse;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.model.*;
import com.eefood.reactionservice.repository.CommentReactionCountRepository;
import com.eefood.reactionservice.repository.CommentReactionRepository;
import com.eefood.reactionservice.repository.CommentRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.NotificationUtils;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentReactionService {
    private final CommentReactionRepository commentReactionRepository;
    private final CommentRepository commentRepository;
    private final CommentReactionCountRepository commentReactionCountRepository;
    private final SecurityUtil securityUtil;
    private final IamClient iamClient;
    private final CommentReactionMapper commentReactionMapper;
    private final NotificationUtils notificationUtils;

    // React hoặc cập nhật reaction của comment
    @Transactional
    public CommentReactionResponse reactToComment(CommentReactionRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        UserInfo userInfo = iamClient.getUserInfo(userId).getData();

        Comment comment = commentRepository.findById(request.getCommentId())
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Kiểm tra reaction hiện tại của user
        CommentReaction existing = commentReactionRepository
                .findByCommentIdAndUserId(request.getCommentId(), userId)
                .orElse(null);

        // Nếu đã có reaction cùng loại => gỡ bỏ
        if (existing != null && existing.getReactionType() == request.getReactionType()) {
            commentReactionRepository.delete(existing);
            decreaseCount(comment, existing.getReactionType());
            return null;
        }

        // Nếu có reaction khác loại => cập nhật lại
        if (existing != null) {
            decreaseCount(comment, existing.getReactionType());
            existing.setReactionType(request.getReactionType());
            CommentReaction updated = commentReactionRepository.save(existing);
            increaseCount(comment, request.getReactionType());

            // Gửi notification (không gửi cho chính mình)
            if (!userId.equals(comment.getUserId())) {
                notificationUtils.sendReactionNotification(
                        comment.getUserId(),
                        userInfo.getUsername(),
                        request.getReactionType(),
                        userInfo.getAvatarUrl(),
                        true,
                        "/posts/" + comment.getPost().getId(),
                        null);
            }

            return commentReactionMapper.toResponse(updated);
        }

        // Nếu chưa từng react => tạo mới
        CommentReaction newReaction = CommentReaction.builder()
                .comment(comment)
                .userId(userId)
                .reactionType(request.getReactionType())
                .build();

        CommentReaction saved = commentReactionRepository.save(newReaction);
        increaseCount(comment, request.getReactionType());

        if (!userId.equals(comment.getUserId())) {
            notificationUtils.sendReactionNotification(
                    comment.getUserId(),
                    userInfo.getUsername(),
                    request.getReactionType(),
                    userInfo.getAvatarUrl(),
                    true,
                    "/posts/" + comment.getPost().getId(),
                    null);
        }

        return commentReactionMapper.toResponse(saved);
    }

    // Gỡ reaction hiện tại của user khỏi comment
    @Transactional
    public void removeReaction(Long commentId) {
        Long userId = securityUtil.getCurrentUserId();
        commentReactionRepository.findByCommentIdAndUserId(commentId, userId)
                .ifPresent(reaction -> {
                    commentReactionRepository.delete(reaction);
                    decreaseCount(reaction.getComment(), reaction.getReactionType());
                });
    }

    // Lấy danh sách reaction theo comment
    public List<CommentReactionResponse> getReactionsByComment(Long commentId) {
        return commentReactionRepository.findAll().stream()
                .filter(r -> r.getComment().getId().equals(commentId))
                .map(commentReactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    // Lấy thống kê reaction theo từng loại
    public Map<ReactionType, Long> getReactionCounts(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        return commentReactionCountRepository.findAllByComment(comment)
                .stream()
                .collect(Collectors.toMap(CommentReactionCount::getReactionType, CommentReactionCount::getCount));
    }

    private void increaseCount(Comment comment, ReactionType type) {
        CommentReactionCountId id = new CommentReactionCountId(comment.getId(), type);
        CommentReactionCount count = commentReactionCountRepository
                .findById(id)
                .orElse(CommentReactionCount.builder().comment(comment).reactionType(type).count(0L).build());
        count.setCount(count.getCount() + 1);
        commentReactionCountRepository.save(count);
    }

    private void decreaseCount(Comment comment, ReactionType type) {
        CommentReactionCountId id = new CommentReactionCountId(comment.getId(), type);
        commentReactionCountRepository.findById(id).ifPresent(count -> {
            count.setCount(Math.max(0, count.getCount() - 1));
            commentReactionCountRepository.save(count);
        });
    }
}
