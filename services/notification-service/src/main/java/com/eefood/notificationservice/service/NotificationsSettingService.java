package com.eefood.notificationservice.service;
import com.eefood.notificationservice.dto.response.NotificationSettingResponse;
import com.eefood.notificationservice.enums.NotificationsType;
import com.eefood.notificationservice.mapper.NotificationsMapper;
import com.eefood.notificationservice.model.NotificationsSetting;
import com.eefood.notificationservice.repository.NotificationsSettingRepository;
import com.eefood.notificationservice.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsSettingService {
    private final NotificationsSettingRepository notificationsSettingRepository;
    private final SecurityUtil securityUtil;
    private final NotificationsMapper notificationsMapper;

    public List<NotificationSettingResponse> getUserNotificationSettings() {
        Long userId = securityUtil.getCurrentUserId();
        List<NotificationsSetting> settings = notificationsSettingRepository.findByUserId(userId);

        // Nếu chưa có cài đặt, tạo mặc định
        if (settings.isEmpty()) {
            settings = createDefaultSettings(userId);
        }

        return settings.stream().map(notificationsMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public NotificationsSetting updateNotificationSetting(NotificationsType type, boolean enabled) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Optional<NotificationsSetting> existingSetting = notificationsSettingRepository
                .findByUserIdAndType(currentUserId, type);

        NotificationsSetting setting;
        if (existingSetting.isPresent()) {
            setting = existingSetting.get();
            setting.setEnabled(enabled);
        } else {
            setting = NotificationsSetting.builder()
                    .userId(currentUserId)
                    .type(type)
                    .enabled(enabled)
                    .build();
        }
        return notificationsSettingRepository.save(setting);
    }

    @Transactional
    public List<NotificationSettingResponse> updateMultipleNotificationSettings(Map<NotificationsType, Boolean> settings) {
        List<NotificationsSetting> updatedSettings = new ArrayList<>();

        for (Map.Entry<NotificationsType, Boolean> entry : settings.entrySet()) {
            NotificationsSetting updatedSetting = updateNotificationSetting(entry.getKey(), entry.getValue());
            updatedSettings.add(updatedSetting);
        }

        return updatedSettings.stream().map(notificationsMapper::toResponse).collect(Collectors.toList());
    }

    private List<NotificationsSetting> createDefaultSettings(Long userId) {
        List<NotificationsSetting> defaultSettings = new ArrayList<>();

        for (NotificationsType type : NotificationsType.values()) {
            NotificationsSetting setting = NotificationsSetting.builder()
                    .userId(userId)
                    .type(type)
                    .enabled(true) // Mặc định bật tất cả
                    .build();
            defaultSettings.add(setting);
        }

        return notificationsSettingRepository.saveAll(defaultSettings);
    }
}
