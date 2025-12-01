package com.eefood.reactionservice.service.share;

import com.eefood.reactionservice.dto.request.ShareRequest;
import com.eefood.reactionservice.dto.response.ShareResponse;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.exception.ExceptionUtil;
import com.eefood.reactionservice.mapper.ShareMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.Share;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShareService {
    private final ShareRepository shareRepository;
    private final PostRepository postRepository;
    private final ShareMapper shareMapper;

    public ShareResponse sharePost(ShareRequest request) {
        Post post = postRepository.findById(request.getPostId()).orElseThrow(()-> ExceptionUtil.badRequest(ErrorMessage.POST_NOT_FOUND));

        Share share = Share.builder()
                .post(post)
                .platform(request.getPlatform())
                .userId(request.getUserId())
                .createdAt(LocalDateTime.now())
                .build();

        return shareMapper.toResponse(shareRepository.save(share));
    }

    public Long getShareCountByPostId(Long postId) {
        return shareRepository.countByPostId(postId);
    }
}
