package com.eefood.notificationservice.controller;
import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.response.NotificationResponse;
import com.eefood.notificationservice.dto.response.NotificationSettingResponse;
import com.eefood.notificationservice.dto.response.ResponseData;
import com.eefood.notificationservice.enums.NotificationsType;
import com.eefood.notificationservice.enums.SuccessMessage;
import com.eefood.notificationservice.service.FirebaseNotificationService;
import com.eefood.notificationservice.service.NotificationsService;
import com.eefood.notificationservice.service.NotificationsSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationsController {
    private final NotificationsService notificationsService;
    private final NotificationsSettingService notificationsSettingService;
    private final FirebaseNotificationService firebaseNotificationService;

    @PostMapping("/unregister/{userId}")
    public ResponseData<Void> unregisterUserToken(@PathVariable Long userId) {
        firebaseNotificationService.unregisterUserToken(userId);
        return new ResponseData<>(HttpStatus.OK.value(),"Successfully unregistered user token");
    }

    @PostMapping
    public ResponseData<Void> sendNotification(@RequestBody NotificationRequest request) {
        notificationsService.handleNotificationIncome(request);
        return new ResponseData<>(HttpStatus.OK.value(),SuccessMessage.SEND_NOTIFICATION_SUCCESS.getMessage());
    }

    @GetMapping
    public ResponseData<Page<NotificationResponse>> getUserNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit)
    {
        Page<NotificationResponse> notificationResponses = notificationsService.getUserNotifications(page, limit);
        return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.GET_SUCCESS_MESSAGE.getMessage(),notificationResponses);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseData<Void> markAsReadNotification(@PathVariable("notificationId") Long notificationId) {
        notificationsService.markAsRead(notificationId);
        return new ResponseData<>(HttpStatus.OK.value(),SuccessMessage.MARK_AS_READ.getMessage());
    }

    @PutMapping("/read-all")
    public ResponseData<Void> markAsReadAll() {
        notificationsService.markAllAsRead();
        return new ResponseData<>(HttpStatus.OK.value(),SuccessMessage.MARK_AS_READ.getMessage());
    }

    @GetMapping("/unread-count")
    public ResponseData<Long> getUnreadCount() {
        Long count  = notificationsService.getUnreadCount();
        return new ResponseData<>(HttpStatus.OK.value(),SuccessMessage.GET_SUCCESS_MESSAGE.getMessage(), count);
    }

    @GetMapping("/settings")
    public ResponseData<List<NotificationSettingResponse>> getNotificationsSettings() {
        List<NotificationSettingResponse> settings = notificationsSettingService.getUserNotificationSettings();
        return new ResponseData<>(HttpStatus.OK.value(),SuccessMessage.GET_SUCCESS_MESSAGE.getMessage(), settings);
    }

    @PutMapping("/settings/update")
    public ResponseData<List<NotificationSettingResponse>> updateMultiSettings(@RequestBody Map<NotificationsType, Boolean> settings) {
        List<NotificationSettingResponse> updatedSettings = notificationsSettingService.updateMultipleNotificationSettings(settings);
        return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.UPDATE_SETTINGS_SUCCESS.getMessage(), updatedSettings);
    }
    
    @DeleteMapping("/{notificationId}")
    public ResponseData<Void> deleteNotification(@PathVariable("notificationId") Long notificationId) {
        notificationsService.softDeleteNotification(notificationId);
        return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.DELETE_NOTIFICATION_SUCCESS.getMessage());
    }

    @DeleteMapping("/delete-all")
    public ResponseData<Void> deleteAllNotifications() {
        notificationsService.softDeleteAllNotifications();
        return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.DELETE_NOTIFICATION_SUCCESS.getMessage());
    }

}
