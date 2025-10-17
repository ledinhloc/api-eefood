package com.eefood.reactionservice.service;

import com.eefood.reactionservice.dto.request.CommentRequest;
import com.eefood.reactionservice.dto.response.CommentResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.model.Comment;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.CommentRepository;
import com.eefood.reactionservice.repository.PostRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;
    private final IamClient iamClient;
    private final SecurityUtil securityUtil;

    // Hàm tạo comment
    @Transactional
    public CommentResponse addComment(CommentRequest request) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.POST_NOT_FOUND));

        Comment parent = null;
        // Nếu là tin nhắn reply
        if (request.getParentId() != null) {
            parent = commentRepository.findByIdAndIsDeletedFalse(request.getParentId())
                    .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.PARENT_COMMENT_NOT_FOUND));

            int depth = getCommentDepth(parent);

            // Nếu parent là cấp 3 -> gán parent về cấp 2
            if (depth >= 3) {
                Comment parentLevel2 = parent.getParent(); // cấp 2
                parent = parentLevel2; // giữ comment mới ở cấp 3
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .userId(currentUserId)
                .content(request.getContent())
                .parent(parent)
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .videos(request.getVideos() != null ? request.getVideos() : new ArrayList<>())
                .build();

        Comment savedComment = commentRepository.save(comment);

        UserInfo userInfo = iamClient.getUserInfo(currentUserId).getData();
        CommentResponse resp = commentMapper.toResponse(savedComment);
        resp.setUsername(userInfo.getUsername());
        resp.setEmail(userInfo.getEmail());
        resp.setAvatarUrl(userInfo.getAvatarUrl());
        return resp;
    }

    // Lấy danh sách comment gốc (cấp 1)
    public Page<CommentResponse> getPostComments(Long postId, Pageable pageable) {
        Page<Comment> roots = commentRepository.findByPostIdAndParentIsNullAndIsDeletedFalse(postId, pageable);
        List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();

        // Lấy tất cả replies cấp 2
        List<Comment> level2 = rootIds.isEmpty()
                ? List.of()
                : commentRepository.findByParentIdIn(rootIds);

        // Lấy tất cả replies cấp 3 cho comment cấp 2
        List<Long> level2Ids = level2.stream().map(Comment::getId).toList();
        List<Comment> level3 = level2Ids.isEmpty()
                ? List.of()
                : commentRepository.findByParentIdIn(level2Ids);

        // Gom nhóm theo parentId
        Map<Long, List<Comment>> groupLevel2 = level2.stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        Map<Long, List<Comment>> groupLevel3 = level3.stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // Gom user info
        List<Long> allUserIds = new ArrayList<>();
        roots.getContent().forEach(c -> allUserIds.add(c.getUserId()));
        level2.forEach(c -> allUserIds.add(c.getUserId()));
        level3.forEach(c -> allUserIds.add(c.getUserId()));
        Map<Long, UserInfo> userMap = getUserInfoBatch(allUserIds);

        // Build tree: cấp 1 + cấp 2 (1 cái) + cấp 3 (1 cái)
        List<CommentResponse> responses = roots.getContent().stream()
                .map(root -> {
                    CommentResponse rootResp = buildCommentResponse(root, userMap);
                    List<Comment> level2Children = groupLevel2.get(root.getId());

                    if (level2Children != null && !level2Children.isEmpty()) {
                        // chỉ lấy comment cấp 2 đầu tiên
                        Comment firstLevel2 = level2Children.get(0);
                        CommentResponse firstLevel2Resp = buildCommentResponse(firstLevel2, userMap);

                        // Đếm số phản hồi của cấp 2
                        firstLevel2Resp.setReplyCount(commentRepository.countByParentId(firstLevel2.getId()));

                        // Nếu có comment cấp 3 -> lấy 1 cái đầu tiên
                        List<Comment> level3Children = groupLevel3.get(firstLevel2.getId());
                        if (level3Children != null && !level3Children.isEmpty()) {
                            Comment firstLevel3 = level3Children.get(0);
                            CommentResponse firstLevel3Resp = buildCommentResponse(firstLevel3, userMap);
                            firstLevel3Resp.setReplyCount(commentRepository.countByParentId(firstLevel3.getId()));
                            firstLevel2Resp.setReplies(List.of(firstLevel3Resp));
                        }

                        // gán lại vào root
                        rootResp.setReplies(List.of(firstLevel2Resp));
                    }

                    // Đếm tổng phản hồi của cấp 1
                    rootResp.setReplyCount(commentRepository.countByParentId(root.getId()));

                    return rootResp;
                })
                .toList();

        return new PageImpl<>(responses, pageable, roots.getTotalElements());
    }

    // Lấy danh sách reply comment --> Khi nhấn “xem phản hồi” (các cấp còn lại)
    public Page<CommentResponse> getCommentReplies(Long commentId, Pageable pageable) {
        Page<Comment> replies = commentRepository.findByParentId(commentId, pageable);
        List<Long> replyIds = replies.getContent().stream().map(Comment::getId).toList();

        // Lấy thêm 1 cấp con (cấp 3)
        List<Comment> subReplies = replyIds.isEmpty() ? List.of() : commentRepository.findByParentIdIn(replyIds);
        Map<Long, List<Comment>> groupByParent = subReplies.stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        List<Long> userIds = new ArrayList<>();
        replies.getContent().forEach(c -> userIds.add(c.getUserId()));
        subReplies.forEach(c -> userIds.add(c.getUserId()));
        Map<Long, UserInfo> userMap = getUserInfoBatch(userIds);

        List<CommentResponse> responses = replies.getContent().stream()
                .map(c -> {
                    CommentResponse resp = buildCommentResponse(c, userMap);
                    List<Comment> children = groupByParent.get(c.getId());
                    if (children != null && !children.isEmpty()) {
                        Comment first = children.get(0);
                        CommentResponse firstResp = buildCommentResponse(first, userMap);
                        firstResp.setReplyCount(commentRepository.countByParentId(first.getId())); // <-- thêm dòng này
                        resp.setReplies(List.of(firstResp));
                    }
                    resp.setReplyCount(commentRepository.countByParentId(c.getId()));
                    return resp;
                })
                .toList();

        return new PageImpl<>(responses, pageable, replies.getTotalElements());
    }


    // Lấy thông tin user
    private Map<Long, UserInfo> getUserInfoBatch(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();

        List<Long> distinctIds = userIds.stream().distinct().collect(Collectors.toList());
        ResponseData<List<UserInfo>> response = iamClient.getUserInfoBatch(distinctIds);
        if ( response.getData() != null) {
            return response.getData().stream()
                    .collect(Collectors
                            .toMap(
                                    UserInfo::getId,
                                    Function.identity(),
                                    (existing, duplicate) -> existing)
                            );
        }
        throw new RuntimeException("Failed to get user info batch");
    }

    // Tính độ sâu comment
    private int getCommentDepth(Comment comment) {
        int depth = 1;
        Comment temp = comment;
        while (temp.getParent() != null) {
            depth++;
            temp = temp.getParent();
            if(depth >= 3)
                break;
        }
        return depth;
    }

    // Lấy thông tin comment khi response
    private CommentResponse buildCommentResponse(Comment c, Map<Long, UserInfo> userMap) {
        CommentResponse resp = commentMapper.toResponse(c);
        UserInfo u = userMap.get(c.getUserId());
        if (u != null) {
            resp.setUsername(u.getUsername());
            resp.setEmail(u.getEmail());
            resp.setAvatarUrl(u.getAvatarUrl());
        }
        return resp;
    }
}
