package com.eefood.notificationservice.model;

import com.eefood.notificationservice.enums.NotificationsType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notifications_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationsSetting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Người sở hữu cài đặt tin nhắn

    @Enumerated(EnumType.STRING)
    private NotificationsType type;

    private boolean enabled;
}
