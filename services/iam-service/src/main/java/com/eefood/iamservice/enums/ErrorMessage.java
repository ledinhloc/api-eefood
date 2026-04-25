package com.eefood.iamservice.enums;

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
  USER_HEIGHT_NOT_FOUND("User height not found"),
  USER_WEIGHT_NOT_FOUND("User weight not found"),

  // Message in dto - Start
  FULL_NAME_REQUIRED("Full name is required"),
  DOB_INVALID("Your age must be at least 6"),
  PHONE_NUMBER_INVALID("Invalid phone number (must be in format 0xxxxxxxxx or +84xxxxxxxxx)"),
  CCCD_INVALID("CCCD must include exactly 12 digits"),
  DUPLICATE_CCCD("CCCD already exists in the system"),
  BHYT_INVALID("BHYT must be 10 digits (new) or 15 characters (old format)"),
  BHYT_DUPLICATE("BHYT already exists in the system"),
  PATIENT_ID_LIST_CANNOT_EMPTY("Patient ID list cannot be empty"),
  PASSWORD_REQUIRED("Password is required"),
  PASSWORD_INVALID("Password must be at least 8 characters"),
  EMAIL_REQUIRED("Email is required"),
  EMAIL_INVALID("Invalid email address"),
  USER_NOT_FOUND("User not found"),
  ROLE_NAME_REQUIRED("Role name is required"),
  PERMISSION_NAME_REQUIRED("Permission name is required"),
  LIST_ROLE_NAMES_EMPTY("List roleNames cannot be empty"),
  LIST_PERMISSION_NAMES_EMPTY("List permissionNames cannot be empty"),
  TOKEN_REQUIRED("Token is required");
  // Message in dto - End

  private final String message;

  ErrorMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
