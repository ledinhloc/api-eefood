package com.eefood.notificationservice.dto.response;

import com.eefood.notificationservice.enums.NotificationsType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingResponse {
    private Long id;
    private NotificationsType type;
    private boolean enabled;
}
