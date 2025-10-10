package com.eefood.notificationservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long notificationId;
    private String title;
    private String body;
    private String type;
    private String path;
    private String avatarUrl;
    private String postImageUrl;
    boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
