package com.eefood.reactionservice.livestream.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveGiftItemResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private String animationUrl;
    private Long diamondCost;
}
