package com.eefood.reactionservice.util;

import com.eefood.common.avro.NotificationEvent;
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
      NotificationEvent notification = NotificationEvent.newBuilder()
        .setTitle(commenterName + " đã bình luận bài viết của bạn")
        .setBody(body)
        .setPath(postPath)
        .setAvatarUrl(avatarUrl)
        .setPostImageUrl(postImageUrl)
        .setType("COMMENT")
        .setUserId(receiverId)
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
      NotificationEvent notification = NotificationEvent.newBuilder()
        .setTitle(title)
        .setBody("Nhấn vào đây để xem chi tiết bài viết")
        .setPath(postPath)
        .setAvatarUrl(avatarUrl)
        .setPostImageUrl(postImageUrl)
        .setType("REACTION")
        .setUserId(receiverId)
        .build();

      notificationProducer.sendNotification(notification);
    }
}
