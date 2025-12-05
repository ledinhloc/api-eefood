package com.eefood.notificationservice.mapper;

import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.response.NotificationResponse;
import com.eefood.notificationservice.dto.response.NotificationSettingResponse;
import com.eefood.notificationservice.model.Notifications;
import com.eefood.notificationservice.model.NotificationsRecipient;
import com.eefood.notificationservice.model.NotificationsSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import javax.management.Notification;

@Mapper(componentModel = "spring")
public interface NotificationsMapper {

    NotificationResponse toResponse(Notifications notification);

    @Mapping(target = "title", source = "notification.title")
    @Mapping(target = "body", source = "notification.body")
    @Mapping(target = "avatarUrl", source = "notification.avatarUrl")
    @Mapping(target = "path", source = "notification.path")
    @Mapping(target = "postImageUrl", source = "notification.postImageUrl")
    @Mapping(target = "type", source = "notification.type")
    @Mapping(target = "id" ,source = "id")
    @Mapping(target = "notificationId", source = "notification.id")
    @Mapping(target = "isRead", source = "read")
    NotificationResponse toResponse(NotificationsRecipient recipient);

    Notifications toNotification(NotificationRequest notificationRequest);

    NotificationSettingResponse toResponse(NotificationsSetting notificationsSetting);
}
