package com.eefood.reactionservice.util;

import com.eefood.reactionservice.dto.request.NotificationRequest;
import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.kafka.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationUtils {
    private final NotificationProducer notificationProducer;

    /**
     * Gửi thông báo khi người dùng bình luận bài viết.
     */
    public void sendCommentNotification(Long receiverId,
                                        String commenterName,
                                        String body,
                                        String avatarUrl,
                                        String postPath,
                                        String postImageUrl) {
        NotificationRequest notification = NotificationRequest.builder()
                .title(commenterName + " đã bình luận bài viết của bạn")
                .body(body)
                .path(postPath)
                .avatarUrl(avatarUrl)
                .postImageUrl(postImageUrl)
                .type("COMMENT")
                .userId(receiverId)
                .build();

        notificationProducer.sendNotification(notification);
    }

    /**
     * Gửi thông báo khi người dùng thả cảm xúc vào bài viết.
     */
    public void sendReactionNotification(Long receiverId,
                                         String reactorName,
                                         ReactionType reactionType,
                                         String avatarUrl,
                                         boolean isPost,
                                         String postPath,
                                         String postImageUrl) {
        String reactionText = switch (reactionType) {
            case LIKE -> "thích";
            case LOVE -> "thả tim";
            case WOW -> "ngạc nhiên";
            case SAD -> "buồn";
            case ANGRY -> "phẫn nộ";
        };

        String type = isPost ? "bài viết" : "bình luận";
        String title = String.format(reactorName + " đã " + reactionText + " %s của bạn", type);
        NotificationRequest notification = NotificationRequest.builder()
                .title(title)
                .body("Nhấn vào đây để xem chi tiết bài viết")
                .path(postPath)
                .avatarUrl(avatarUrl)
                .postImageUrl(postImageUrl)
                .type("REACTION")
                .userId(receiverId)
                .build();

        notificationProducer.sendNotification(notification);
    }
}
