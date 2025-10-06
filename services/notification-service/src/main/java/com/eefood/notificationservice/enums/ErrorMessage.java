package com.eefood.notificationservice.enums;

public enum ErrorMessage {
    // Message chung chung
    UNCATEGORIZED_EXCEPTION("Uncategorized error. Please check log for more detail"),
    INVALID_KEY(
            "Invalid message key"), // Dùng khi message chứa ErrorCode bị sai chính tả (trong @Size)
    INVALID_PARAMETER_TYPE("Invalid value for parameter"),
    MALFORMED_JSON("The JSON in your request is invalid. Please check the syntax."),
    URL_NOT_FOUND("Requested URL is not mapped"),
    INVALID_MESSAGE_KEY("Invalid message key"),
    CONSTRAINT_VIOLATION("Constraint violation in database"),
    VALIDATION_FAILED("Validation failed"),

    // Message cụ thể
    GENDER_INVALID("Gender must be one of: MALE, FEMALE, OTHER (or male, female, other). Unknown"),
    USER_NOT_EXISTED("User not existed"),
    CANNOT_CREATE_TOKEN("Cannot create token"),
    INVALID_TOKEN_FORMAT("Invalid token format"),
    ROLE_NOT_FOUND("Role not found"),
    ROLES_NOT_FOUND("Roles not found"),
    UNAUTHENTICATED("Unauthenticated"),
    ACCESS_DENIED("You do not have permission"),
    PERMISSION_NOT_FOUND("Permission not found"),
    PERMISSIONS_NOT_FOUND("Permissions not found"),
    USER_EXISTED("User already existed"),
    SEND_EMAIL_FAIL("Failed to send OTP email"),
    OTP_INVALID_OR_EXPIRED("OTP is invalid or has expired"),
    OTP_SEND_TO_MUCH("OTP send to much"),
    OLD_PASSWORD_INCORRECT("Old password is incorrect"),
    FAIL_RESET_PASSWORD("Failed to reset password in Keycloak"),
    FAIL_UPDATE_USER("User cant't update"),
    FAIL_UPDATE_PROFILE_USER("User cant't update profile"),
    FAIL_DELETE_USER("User cant't delete"),
    FAIL_UPDATE_ROLE("User cant't update role"),

    SETTING_NOT_FOUND("Setting not found"),
    NOTIFICATION_NOT_FOUND("Notifications not found"),
    NOTIFICATION_DISABLE_TYPE("Notfication is disabled for type");
    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
