package com.eefood.reactionservice.enums;

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
  TOKEN_REQUIRED("Token is required"),
  // Message in dto - End



  //Recipe service
  RECIPE_NOT_FOUND("Recipe not found or already deleted"),

  //ingre
  INGREDIENT_SHOPPING_NOT_FOUND("Ingredient not found"),
  SHOPPING_ITEM_NOT_FOUND("Shopping Item not found"),
  SHOPPING_ITEM_MORE("Expected 1 ShoppingItem but found more than one item"),

  //post
  POST_NOT_FOUND("Post not found"),
  //Comment
  LIMIT_REPLIES("Replies are limited to 3 levels"),
  PARENT_COMMENT_NOT_FOUND("Parent comment not found"),

  //post - collection
  COLLECTION_NOT_FOUND("Collection not found"),
  ALREADY_EXISTS("Post already exists"),
  DUPLICATE_COLLECTION_NAME("Collection name already exists for this user"),
  ALREADY_DELETED("Already deleted"),
  INVALID_REQUEST("Invalid request data"),
  LIVE_STREAM_NOT_FOUND("Livestream not found"),

  //block live
  USER_ALREADY_BLOCKED("This user is already blocked by you"),
  USER_NOT_BLOCKED("This user is not blocked"),
  USER_INFO_NOT_FOUND("User info not found"),
  USER_BATCH_INFO_FAILED("Failed to fetch user info batch"),

  //poll
  POLL_NOT_FOUND("Poll not found"),
  POLL_SETTING_NOT_FOUND("Poll setting not found"),
  POLL_NOT_OPEN("Poll is not open"),
  POLL_OPTION_INVALID("Invalid poll option"),
  POLL_ALREADY_VOTED("User already voted"),
  POLL_MULTIPLE_CHOICE_NOT_SUPPORTED("Multiple choice is enabled but not supported in vote schema v1"),
  INVALID_LIVESTREAM_ID("Invalid livestream id"),
  INVALID_POLL_QUESTION("Invalid poll question"),
  INVALID_POLL_OPTIONS("Invalid poll options"),
  POLL_OPTION_ALREADY_SELECTED("Option already selected"),
  POLL_MAX_CHOICES_EXCEEDED("Maximum number of choices exceeded"),
  POLL_SINGLE_CHOICE_ONLY("This poll only allows one choice"),
  INVALID_POLL_STATUS_TRANSITION("Invalid poll status transition"),
  POLL_OPTION_PROPOSAL_NOT_ALLOWED("Poll option proposal is not allowed"),
  POLL_OPTION_PROPOSAL_DUPLICATED("Poll option proposal already exists"),
  POLL_OPTION_PROPOSAL_NOT_FOUND("Poll option proposal not found"),
  INVALID_POLL_OPTION_PROPOSAL_STATUS_TRANSITION("Invalid poll option proposal status transition"),

  // chatbot
  AI_FREE_QUOTA_EXCEEDED("Xin lỗi. Bạn đã hết lượt miễn phí !!!"),
  AI_OVERLOADED("Hệ thống đang quá tải. Vui lòng thử lại sau vài giây."),
  AI_NOT_EXCUTED("Không thể thực hiện tác vụ. Vui lòng thử lại!"),
  AI_INTERNAL_ERROR("Hệ thống đang gặp sự cố. Vui lòng thử lại.");


  private final String message;

  ErrorMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
