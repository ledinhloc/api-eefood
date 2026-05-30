package com.eefood.reactionservice.livestream.dto.request;

import lombok.Data;

@Data
public class SendGiftRequest {
    private Long giftItemId;
    private Long livestreamId;
    private Integer quantity;
}
