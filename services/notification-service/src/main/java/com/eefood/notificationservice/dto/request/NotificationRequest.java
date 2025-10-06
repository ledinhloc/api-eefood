package com.eefood.notificationservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    String title;
    String body;
    String path;
    String avatarUrl;
    String postImageUrl;
    String type;
    String userId;
}
