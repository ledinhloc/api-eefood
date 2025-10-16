package com.eefood.reactionservice.controller;


import com.eefood.reactionservice.dto.response.CollectionResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.service.CollectionService;
import com.eefood.reactionservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {
  private final CollectionService service;
  private final SecurityUtil securityUtils;

  @PostMapping
  public ResponseData<CollectionResponse> create(@RequestParam String name) {
    Long userId = securityUtils.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Create Success",service.create(userId, name));
  }

  @PutMapping("/{id}")
  public ResponseData<CollectionResponse> update(
    @PathVariable Long id,
    @RequestParam String name,
    @RequestParam String coverImageUrl) {
    return new ResponseData<>(HttpStatus.OK.value(), "Update Success", service.update(id, name, coverImageUrl));
  }

  @DeleteMapping("/{id}")
  public ResponseData<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return new ResponseData<>(HttpStatus.OK.value(), "Delete Success");
  }

  @GetMapping("/user")
  public ResponseData<List<CollectionResponse>> getByUser() {
    Long userId = securityUtils.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Get By User", service.getByUser(userId));
  }

  @GetMapping("/{id}")
  public ResponseData<CollectionResponse> getById(@PathVariable Long id) {
    return new ResponseData<>(HttpStatus.OK.value(), "Get By Id", service.getById(id));
  }

  @PostMapping("/{collectionId}/posts/{postId}")
  public ResponseData<Void> addPost(@PathVariable Long collectionId, @PathVariable Long postId) {
    service.addPost(collectionId, postId);
    return new ResponseData<>(HttpStatus.OK.value(), "Add Post Success");
  }

  @DeleteMapping("/{collectionId}/posts/{postId}")
  public ResponseData<Void> removePost(@PathVariable Long collectionId, @PathVariable Long postId) {
    service.removePost(collectionId, postId);
    return new ResponseData<>(HttpStatus.OK.value(), "Remove Post Success");
  }
}
