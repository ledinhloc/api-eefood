package com.eefood.notificationservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    Long userId;
}