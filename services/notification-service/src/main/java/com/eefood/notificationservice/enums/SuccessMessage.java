package com.eefood.notificationservice.enums;

public enum SuccessMessage {
    MARK_AS_READ("Notification marked as read"),
    UPDATE_SETTINGS_SUCCESS("Notification settings updated"),
    MARK_ALL_AS_READ("Notification marked all as read"),
    SEND_NOTIFICATION_SUCCESS("Sent notification successfully"),
    GET_SUCCESS_MESSAGE("get success message");
    private final String message;

    SuccessMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
