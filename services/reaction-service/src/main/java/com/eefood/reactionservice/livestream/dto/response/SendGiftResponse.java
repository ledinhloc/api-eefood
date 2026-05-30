package com.eefood.reactionservice.livestream.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendGiftResponse {
    private Long giftLogId;

    private Long senderId;
    private String senderName;
    private String senderImageUrl;

    private Long receiverId;
    private Long livestreamId;

    private Long giftItemId;
    private String giftName;
    private String animationUrl;
    private Integer quantity;

    private Long totalDiamondSpent;
    private Long senderNewBalance;
}
