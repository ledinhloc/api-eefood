package com.eefood.iamservice.controller;

import com.eefood.iamservice.dto.request.UserHeightRequest;
import com.eefood.iamservice.dto.request.UserWeightRequest;
import com.eefood.iamservice.dto.response.ResponseData;
import com.eefood.iamservice.dto.response.UserHeightResponse;
import com.eefood.iamservice.dto.response.UserWeightResponse;
import com.eefood.iamservice.service.UserHeightService;
import com.eefood.iamservice.service.UserWeightService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserBodyController {
  private final UserHeightService userHeightService;
  private final UserWeightService userWeightService;

  @GetMapping("/heights")
  public ResponseData<List<UserHeightResponse>> getMyHeights() {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", userHeightService.getMyHeights());
  }

  @PostMapping("/heights")
  public ResponseData<UserHeightResponse> createMyHeight(
      @RequestBody @Valid UserHeightRequest request) {
    return new ResponseData<>(
        HttpStatus.OK.value(), "Create Success", userHeightService.createMyHeight(request));
  }

  @PutMapping("/heights/{heightId}")
  public ResponseData<UserHeightResponse> updateMyHeight(
      @PathVariable Long heightId, @RequestBody @Valid UserHeightRequest request) {
    return new ResponseData<>(
        HttpStatus.OK.value(), "Update Success", userHeightService.updateMyHeight(heightId, request));
  }

  @DeleteMapping("/heights/{heightId}")
  public ResponseData<Void> deleteMyHeight(@PathVariable Long heightId) {
    userHeightService.deleteMyHeight(heightId);
    return new ResponseData<>(HttpStatus.OK.value(), "Delete Success");
  }

  @GetMapping("/weights")
  public ResponseData<List<UserWeightResponse>> getMyWeights() {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", userWeightService.getMyWeights());
  }

  @PostMapping("/weights")
  public ResponseData<UserWeightResponse> createMyWeight(
      @RequestBody @Valid UserWeightRequest request) {
    return new ResponseData<>(
        HttpStatus.OK.value(), "Create Success", userWeightService.createMyWeight(request));
  }

  @PutMapping("/weights/{weightId}")
  public ResponseData<UserWeightResponse> updateMyWeight(
      @PathVariable Long weightId, @RequestBody @Valid UserWeightRequest request) {
    return new ResponseData<>(
        HttpStatus.OK.value(), "Update Success", userWeightService.updateMyWeight(weightId, request));
  }

  @DeleteMapping("/weights/{weightId}")
  public ResponseData<Void> deleteMyWeight(@PathVariable Long weightId) {
    userWeightService.deleteMyWeight(weightId);
    return new ResponseData<>(HttpStatus.OK.value(), "Delete Success");
  }
}
