package com.eefood.notificationservice.exception;

import com.eefood.notificationservice.dto.response.ResponseData;
import com.eefood.notificationservice.enums.ErrorMessage;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // Handle ResponseStatusException (chủ động throw trong service/controller)
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ResponseData<Void>> handleResponseStatusException(
      ResponseStatusException ex) {
    String reason = ex.getReason();
    String message = (reason != null) ? reason : ErrorMessage.UNCATEGORIZED_EXCEPTION.getMessage();

    return ResponseEntity.status(ex.getStatusCode())
        .body(new ResponseData<>(ex.getStatusCode().value(), message));
  }

  // Handle @Valid lỗi DTO
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<String> handleValidationErrors(MethodArgumentNotValidException ex) {
    List<String> messages = new ArrayList<>();

    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      String key = error.getDefaultMessage();
      try {
        String message = ErrorMessage.valueOf(key).getMessage();
        messages.add(message);
      } catch (IllegalArgumentException e) {
        return new ResponseData<>(
            HttpStatus.BAD_REQUEST.value(),
            ErrorMessage.INVALID_MESSAGE_KEY.getMessage() + ": " + key);
      }
    }

    String combinedMessage = String.join("; ", messages) + ";";
    return new ResponseData<>(
        HttpStatus.BAD_REQUEST.value(),
        ErrorMessage.VALIDATION_FAILED.getMessage() + ": [" + combinedMessage + "]");
  }

  // Handle @Valid lỗi ở Entity (ConstraintViolation)
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<String> handleConstraintViolation(ConstraintViolationException ex) {
    List<String> messages = new ArrayList<>();

    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String key = violation.getMessage();
      try {
        String message = ErrorMessage.valueOf(key).getMessage();
        messages.add(message);
      } catch (IllegalArgumentException e) {
        return new ResponseData<>(
            HttpStatus.BAD_REQUEST.value(),
            ErrorMessage.INVALID_MESSAGE_KEY.getMessage() + ": " + key);
      }
    }

    String combinedMessage = String.join("; ", messages) + ";";
    return new ResponseData<>(
        HttpStatus.BAD_REQUEST.value(),
        ErrorMessage.VALIDATION_FAILED.getMessage() + ": [" + combinedMessage + "]");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<Void> handleUniqueConstraintViolation(DataIntegrityViolationException ex) {
    Throwable rootCause = ex.getRootCause();
    String message = rootCause != null ? rootCause.getMessage() : ex.getMessage();
    return new ResponseData<>(
        HttpStatus.BAD_REQUEST.value(), ErrorMessage.CONSTRAINT_VIOLATION.getMessage() + message);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return new ResponseData<>(
        HttpStatus.BAD_REQUEST.value(),
        ErrorMessage.INVALID_PARAMETER_TYPE.getMessage() + ": " + ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<Void> jsonParseErrorHandler(HttpMessageNotReadableException ex) {
    return new ResponseData<>(
        HttpStatus.BAD_REQUEST.value(), ErrorMessage.MALFORMED_JSON.getMessage() + ex.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseData<Void> handleNoResourceFound(NoResourceFoundException ex) {
    return new ResponseData<>(
        HttpStatus.NOT_FOUND.value(),
        ErrorMessage.URL_NOT_FOUND.getMessage() + ": " + ex.getMessage());
  }

  @ExceptionHandler(JOSEException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseData<Void> handleJoseException(JOSEException ex) {
    return new ResponseData<>(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        ErrorMessage.CANNOT_CREATE_TOKEN.getMessage() + ": " + ex.getMessage());
  }

  @ExceptionHandler(ParseException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<Void> handleParseException(ParseException ex) {
    return new ResponseData<>(
        HttpStatus.BAD_REQUEST.value(),
        ErrorMessage.INVALID_TOKEN_FORMAT.getMessage() + ": " + ex.getMessage());
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ResponseData<Void> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
    return new ResponseData<>(
        HttpStatus.FORBIDDEN.value(), ErrorMessage.ACCESS_DENIED.getMessage());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseData<Void> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex) {
    return new ResponseData<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
  }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseData<Void> handleRuntimeException(RuntimeException ex) {
    return new ResponseData<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseData<Void> handleGenericException(Exception ex) {
    log.error("Uncategorized Exception: ", ex);
    return new ResponseData<>(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        ErrorMessage.UNCATEGORIZED_EXCEPTION.getMessage());
  }
}
