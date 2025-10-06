package com.eefood.notificationservice.service;
import com.eefood.notificationservice.dto.request.NotificationRequest;
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
import com.eefood.notificationservice.utils.ExceptionUtil;
import com.eefood.notificationservice.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


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

    // Giống push notification
    public void handleNotificationIncome(NotificationRequest request) {
        Long userId = null;
        NotificationsType type = NotificationsType.valueOf(request.getType());;



        if(request.getUserId()!= null && !request.getUserId().isBlank()) {
            userId = Long.parseLong(request.getUserId());
            Optional<NotificationsSetting> settingOpt =
                    notificationsSettingRepository.findByUserIdAndType(userId, type);

            if (settingOpt.isPresent() && !settingOpt.get().isEnabled()) {
                log.info("User {} disabled notification type {}", userId, request.getType());
                throw ExceptionUtil.forbidden(ErrorMessage.NOTIFICATION_DISABLE_TYPE);
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

        if(userId != null) {
            NotificationsRecipient recipient = NotificationsRecipient.builder()
                    .userId(userId)
                    .notification(notification)
                    .isRead(false)
                    .build();
            notification.setRecipients(List.of(recipient));
        }

        notificationsRepository.save(notification);

        // Gửi qua WebSocket
        if (userId != null) {
            sendNotificationViaWebSocket(request, userId);
        } else {
            sendBroadcastNotification(request);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(int page, int size) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationsRecipient> recipientPage = notificationsRecipientRepo
                .findUserNotifications(currentUserId, pageable);
        return recipientPage.map(n -> {

            NotificationResponse resp = notificationsMapper.toResponse(n);
            Optional<NotificationsRecipient> rOpt = notificationsRecipientRepo.findByUserIdAndNotificationId(currentUserId, n.getId());
            boolean isRead = rOpt.map(NotificationsRecipient::isRead).orElse(false);
            resp.setRead(isRead);
            return resp;
        });
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getSystemNotifications(int page, int size) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notifications> notifications = notificationsRepository.findSystemNotificationsForUser(NotificationsType.SYSTEM, currentUserId, pageable);
        return notifications.map(notificationsMapper::toResponse);
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

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
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
                .isRead(false)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/notifications",
                response
        );
        log.info("Broadcast notification sent via WebSocket (topic /topic/notifications)");
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Optional<NotificationsRecipient> recipient = notificationsRecipientRepo
                .findByUserIdAndNotificationId(currentUserId, notificationId);
        if (recipient.isPresent() ) {

            NotificationsRecipient notificationsRecipient = recipient.get();
            if(!notificationsRecipient.isRead()) {
                notificationsRecipient.setRead(true);
                notificationsRecipient.setReadAt(LocalDateTime.now());
                notificationsRecipientRepo.save(notificationsRecipient);
            }
        }
        else {
            // Chưa có bản ghi, tạo mới bản ghi NotificationRecipient
            Notifications notification = notificationsRepository.findById(notificationId)
                    .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.NOTIFICATION_NOT_FOUND));

            NotificationsRecipient newRecipient = NotificationsRecipient.builder()
                    .userId(currentUserId)
                    .notification(notification)
                    .isRead(true)
                    .readAt(LocalDateTime.now())
                    .build();
            notificationsRecipientRepo.save(newRecipient);
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

        List<Notifications> unreadSystemNotifications =
                notificationsRepository.findUnreadSystemNotificationsForUser(NotificationsType.SYSTEM, currentUserId);

        if (!unreadSystemNotifications.isEmpty()) {
            List<NotificationsRecipient> created = unreadSystemNotifications.stream().map(n -> {
                NotificationsRecipient r = NotificationsRecipient.builder()
                        .userId(currentUserId)
                        .notification(n)
                        .isRead(true)
                        .readAt(LocalDateTime.now())
                        .build();
                return r;
            }).toList();

            notificationsRecipientRepo.saveAll(created);
        }
    }

    public Long getUnreadCount() {
        Long currentUserId = securityUtil.getCurrentUserId();
        return notificationsRecipientRepo.countByUserIdAndIsReadFalseAndIsDeletedFalse(currentUserId);
    }
}
