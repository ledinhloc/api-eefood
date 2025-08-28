package com.eefood.iamservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class ResponseData<T> {

  private int status;
  private String message;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private T data;

  // DELETE, ERROR
  public ResponseData(int status, String message) {
    this.status = status;
    this.message = message;
  }

  //GET, PUT, POST,
  public ResponseData(int status, String message, T data) {
    this.status = status;
    this.message = message;
    this.data = data;
  }
}
