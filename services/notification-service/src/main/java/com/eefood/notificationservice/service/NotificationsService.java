package com.eefood.notificationservice.service;
import com.eefood.notificationservice.dto.request.NotificationRequest;
import com.eefood.notificationservice.dto.request.UserNotificationResquest;
import com.eefood.notificationservice.dto.response.NotificationResponse;
import com.eefood.notificationservice.enums.ErrorMessage;
import com.eefood.notificationservice.enums.NotificationsType;
import com.eefood.notificationservice.mapper.NotificationsMapper;
import com.eefood.notificationservice.model.Notifications;
import com.eefood.notificationservice.model.NotificationsRecipient;
import com.eefood.notificationservice.model.NotificationsSetting;
import com.eefood.notificationservice.repository.NotificationsRecipientRepo;
import com.eefood.notificationservice.repository.NotificationsRepository;
import com.eefood.notificationservice.repository.NotificationsSettingRepository;
import com.eefood.notificationservice.repository.UserFcmTokenRepository;
import com.eefood.notificationservice.repository.httpclient.IamClient;
import com.eefood.notificationservice.utils.ExceptionUtil;
import com.eefood.notificationservice.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsService {
    private final NotificationsRepository notificationsRepository;
    private final NotificationsSettingRepository notificationsSettingRepository;
    private final NotificationsRecipientRepo recipientRepo;
    private final NotificationsMapper notificationsMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityUtil securityUtil;
    private final NotificationsRecipientRepo notificationsRecipientRepo;
    private final IamClient iamClient;
    private final FirebaseNotificationService firebaseNotificationService;
    private final UserFcmTokenRepository userFcmTokenRepository;

    private NotificationsSetting getOrCreateSetting(Long userId, NotificationsType type) {

        return notificationsSettingRepository
                .findByUserIdAndType(userId, type)
                .orElseGet(() -> {
                    log.info("Create default notification setting for user {}, type {}", userId, type);

                    NotificationsSetting setting = NotificationsSetting.builder()
                            .userId(userId)
                            .type(type)
                            .enabled(true)
                            .build();

                    return notificationsSettingRepository.save(setting);
                });
    }

    private boolean hasFcmToken(Long userId) {
        return userFcmTokenRepository.findByUserId(userId)
                .map(token -> token.getFcmToken() != null && !token.getFcmToken().isBlank())
                .orElse(false);
    }

    @Transactional
    public void sendNotificationToAdmins(NotificationRequest request) {

        var response = iamClient.getAllUserNotifications();
        List<UserNotificationResquest> users = response.getData();

        // Lọc admin
        List<Long> adminIds = users.stream()
                .filter(u -> "ADMIN".equals(u.getRole()))
                .map(UserNotificationResquest::getId)
                .toList();

        if (adminIds.isEmpty()) {
            log.warn("No admin found to send notification");
            return;
        }

        // Tạo Notifications entity
        Notifications notification = Notifications.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .path(request.getPath())
                .avatarUrl(request.getAvatarUrl())
                .postImageUrl(request.getPostImageUrl())
                .type(NotificationsType.valueOf(request.getType()))
                .build();

        // Tạo bản ghi NotificationsRecipient cho toàn bộ admin
        List<NotificationsRecipient> recipients = adminIds.stream()
                .map(adminId -> NotificationsRecipient.builder()
                        .userId(adminId)
                        .notification(notification)
                        .isRead(false)
                        .isDeleted(false)
                        .build())
                .collect(Collectors.<NotificationsRecipient>toList());

        notification.setRecipients(recipients);

        // Lưu vào DB
        notificationsRepository.save(notification);

        // Tạo object để gửi qua Firebase
        NotificationResponse resp = NotificationResponse.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .path(request.getPath())
                .avatarUrl(request.getAvatarUrl())
                .postImageUrl(request.getPostImageUrl())
                .isRead(false)
                .build();

        // Gửi tb
        firebaseNotificationService.sendNotificationToListUser(adminIds, resp);

        log.info("Saved & sent notification for admins: {}", adminIds);
    }

    // Giống push notification
    @Transactional
    public void handleNotificationIncome(NotificationRequest request) {

        boolean hasUserId = request.getUserId() != null;
        Long userId = null;
        NotificationsType type = NotificationsType.valueOf(request.getType().trim());
        if(hasUserId) {

            userId = request.getUserId();
            NotificationsSetting setting = getOrCreateSetting(userId, type);

            if (Boolean.FALSE.equals(setting.isEnabled())) {
                log.info("User {} disabled notification type {}", userId, type);
                return;
            }
        }

        Notifications notification = Notifications.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .path(request.getPath())
                .avatarUrl(request.getAvatarUrl())
                .postImageUrl(request.getPostImageUrl())
                .type(type)
                .build();

        if(hasUserId) {
            NotificationsRecipient recipient = NotificationsRecipient.builder()
                    .userId(userId)
                    .notification(notification)
                    .isRead(false)
                    .build();
            notification.setRecipients(List.of(recipient));
            notificationsRepository.save(notification);
            if (hasFcmToken(userId)) {
                sendNotificationViaWebSocket(request, userId);
            } else {
                log.info("Skip sending notification, user {} has no FCM token", userId);
            }
        }
        else {
            Long currentUserId = securityUtil.getCurrentUserId();
            var response = iamClient.getAllUserNotifications();

            if (response == null || response.getData() == null) {
                throw ExceptionUtil.badRequest(ErrorMessage.USER_NOT_EXISTED);
            }

            List<UserNotificationResquest> users = response.getData();

            if(users.isEmpty()) {
                throw ExceptionUtil.forbidden(ErrorMessage.USER_NOT_EXISTED);
            }

            List<NotificationsRecipient> recipients = users.stream()
                    .filter(u -> !"ADMIN".equals(u.getRole()))
                    .filter(u -> !u.getId().equals(currentUserId))
                    .filter(u -> hasFcmToken(u.getId()))
                    .map(u -> NotificationsRecipient.builder()
                            .userId(u.getId())
                            .notification(notification)
                            .isRead(false)
                            .isDeleted(false)
                            .build())
                    .collect(Collectors.toList());
            if (recipients.isEmpty()) {
                log.info("No users with FCM token, skip broadcast sending");
                return;
            }
            notification.setRecipients(recipients);
            notificationsRepository.save(notification);
            log.info("Send broadcasting notification");
            sendBroadcastNotification(request);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(int page, int size) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationsRecipient> recipientPage = notificationsRecipientRepo
                .findByUserIdAndIsDeletedFalse(currentUserId, pageable);
        return  recipientPage.map(notificationsMapper::toResponse);
    }

    private void sendNotificationViaWebSocket(NotificationRequest request, Long userId) {
        NotificationResponse response = NotificationResponse.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .path(request.getPath())
                .avatarUrl(request.getAvatarUrl())
                .postImageUrl(request.getPostImageUrl())
                .isRead(false)
                .build();

        /*messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                response
        );*/
        firebaseNotificationService.sendNotificationToUser(
                userId,
                response
        );
        log.info("Notification sent via WebSocket to user: {}", userId);
    }

    private void sendBroadcastNotification(NotificationRequest request) {
        NotificationResponse response = NotificationResponse.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .path(request.getPath())
                .avatarUrl(request.getAvatarUrl())
                .postImageUrl(request.getPostImageUrl())
                .isRead(false)
                .build();

        /*messagingTemplate.convertAndSend(
                "/topic/notifications",
                response
        );*/
        firebaseNotificationService.sendBroadcast(response);
        log.info("Broadcast notification sent via WebSocket (topic /topic/notifications)");
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        log.info("Marking notification {} as read", notificationId);
        log.info("Current user: {}", currentUserId);
        Optional<NotificationsRecipient> recipient = notificationsRecipientRepo
                .findByUserIdAndIdAndIsDeletedFalse(currentUserId, notificationId);
        if (recipient.isPresent() ) {
            NotificationsRecipient notificationsRecipient = recipient.get();
            if(!notificationsRecipient.isRead()) {
                notificationsRecipient.setRead(true);
                notificationsRecipient.setReadAt(LocalDateTime.now());
                notificationsRecipientRepo.save(notificationsRecipient);
            }
        }
        else {
            throw ExceptionUtil.forbidden(ErrorMessage.NOTIFICATION_NOT_FOUND);
        }
    }

    @Transactional
    public void markAllAsRead() {
        Long currentUserId = securityUtil.getCurrentUserId();
        List<NotificationsRecipient> unreadNotifications = notificationsRecipientRepo
                .findByUserIdAndIsReadFalseAndIsDeletedFalse(currentUserId);

        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        });

        notificationsRecipientRepo.saveAll(unreadNotifications);
    }

    public Long getUnreadCount() {
        Long currentUserId = securityUtil.getCurrentUserId();
        return notificationsRecipientRepo.countByUserIdAndIsReadFalseAndIsDeletedFalse(currentUserId);
    }

    @Transactional
    public void softDeleteNotification(Long notificationId) {
        Long currentUserId = securityUtil.getCurrentUserId();

        Optional<NotificationsRecipient> recipientOpt =
                notificationsRecipientRepo.findByUserIdAndIdAndIsDeletedFalse(currentUserId, notificationId);

        if (recipientOpt.isEmpty()) {
            throw ExceptionUtil.notFound(ErrorMessage.NOTIFICATION_NOT_FOUND);
        }

        NotificationsRecipient recipient = recipientOpt.get();

        if (!recipient.getIsDeleted()) {
            recipient.setIsDeleted(true);
            notificationsRecipientRepo.save(recipient);
            log.info("Soft deleted notification {} for user {}", notificationId, currentUserId);
        }
    }

    @Transactional
    public void softDeleteAllNotifications() {
        Long currentUserId = securityUtil.getCurrentUserId();

        List<NotificationsRecipient> recipients =
                notificationsRecipientRepo.findByUserIdAndIsDeletedFalse(currentUserId);

        if (recipients.isEmpty()) {
            log.info("No notifications to delete for user {}", currentUserId);
            return;
        }

        recipients.forEach(recipient -> {
            recipient.setIsDeleted(true);
        });

        notificationsRecipientRepo.saveAll(recipients);
        log.info("Soft deleted {} notifications for user {}", recipients.size(), currentUserId);
    }
}
