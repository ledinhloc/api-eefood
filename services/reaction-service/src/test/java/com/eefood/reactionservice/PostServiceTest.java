package com.eefood.reactionservice;

import com.eefood.reactionservice.dto.SearchResult;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserInfo;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.mapper.PostMapper;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.repository.post.PostRepository;
import com.eefood.reactionservice.service.follow.FollowService;
import com.eefood.reactionservice.service.post.PostSearchService;
import com.eefood.reactionservice.service.post.PostService;
import com.eefood.reactionservice.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @InjectMocks
  private PostService postService;

  @Mock
  private SecurityUtil securityUtil;

  @Mock
  private IamClient iamClient;

  @Mock
  private FollowService followService;

  @Mock
  private PostSearchService postSearchService;

  @Mock
  private PostRepository postRepo;

  @Mock
  private PostMapper postMapper;

  // ====== Test 1: keyword = "unknown" ======
  @Test
  void getAllPosts_keywordUnknown_returnEmptyPage() {
    Page<PostResponse> result = postService.getAllPosts(
      "unknown", null, null, null, null, 1, 10
    );

    assertNotNull(result);
    assertEquals(0, result.getContent().size());

    verifyNoInteractions(
      securityUtil, iamClient, followService, postSearchService, postRepo
    );
  }

  // ====== Test 2: Guest user (securityUtil throw exception) ======
  @Test
  void getAllPosts_guestUser_success() {
    when(securityUtil.getCurrentUserId()).thenThrow(new RuntimeException("No token"));

    List<Long> ids = List.of(2L, 1L);
    SearchResult esResult = new SearchResult(ids, 2L);
    when(postSearchService.searchPostIds(
      any(), any(), any(), any(), any(),
      isNull(), anyList(), anyList(), eq(1), eq(10)
    )).thenReturn(esResult);

    Post p1 = new Post(); p1.setId(1L); p1.setUserId(100L);
    Post p2 = new Post(); p2.setId(2L); p2.setUserId(101L);
    when(postRepo.findAll(any(Specification.class)))
      .thenReturn(List.of(p1, p2));

    when(postMapper.toResponse(p1)).thenReturn(PostResponse.builder().id(1L).build());
    when(postMapper.toResponse(p2)).thenReturn(PostResponse.builder().id(2L).build());

    // mock mapToPostResponse gọi batch user info
    UserInfo u1 = new UserInfo(100L, "u100", "u100@mail.com", "avt1");
    UserInfo u2 = new UserInfo(101L, "u101", "u101@mail.com", "avt2");
    when(iamClient.getUserInfoBatch(anyList()))
      .thenReturn(new ResponseData<>(200, "OK", List.of(u1, u2)));

    Page<PostResponse> result = postService.getAllPosts(
      "bún bò", null, null, null, null, 1, 10
    );

    assertEquals(2, result.getTotalElements());
    // phải theo thứ tự ES: [2,1]
    assertEquals(2L, result.getContent().get(0).getId());
    assertEquals(1L, result.getContent().get(1).getId());

    verify(securityUtil).getCurrentUserId();
    verify(postSearchService).searchPostIds(
      any(), any(), any(), any(), any(),
      isNull(), anyList(), anyList(), eq(1), eq(10)
    );
  }

  // ====== Test 3: Logged-in user ======
  @Test
  void getAllPosts_loggedInUser_success() {
    when(securityUtil.getCurrentUserId()).thenReturn(10L);

    UserResponse user = UserResponse.builder().id(10L).build();
    when(iamClient.getUserById(10L))
      .thenReturn(new ResponseData<>(200, "OK", user));

    when(followService.getNewFollowings(10L)).thenReturn(List.of(1L, 2L));
    when(followService.getOldFollowings(10L)).thenReturn(List.of(3L));

    List<Long> ids = List.of(5L);
    when(postSearchService.searchPostIds(
      any(), any(), any(), any(), any(),
      eq(user), anyList(), anyList(), eq(1), eq(10)
    )).thenReturn(new SearchResult(ids, 1L));

    Post p = new Post(); p.setId(5L); p.setUserId(200L);
    when(postRepo.findAll(any(Specification.class)))
      .thenReturn(List.of(p));

    when(postMapper.toResponse(p))
      .thenReturn(PostResponse.builder().id(5L).build());

    UserInfo u = new UserInfo(200L, "u200", "u200@mail.com", "avt");
    when(iamClient.getUserInfoBatch(anyList()))
      .thenReturn(new ResponseData<>(200, "OK", List.of(u)));

    Page<PostResponse> result = postService.getAllPosts(
      "bún bò", null, null, null, null, 1, 10
    );

    assertEquals(1, result.getTotalElements());
    assertEquals(5L, result.getContent().get(0).getId());

    verify(iamClient).getUserById(10L);
    verify(followService).getNewFollowings(10L);
    verify(followService).getOldFollowings(10L);
  }

  // ====== Test 4: ES trả về rỗng ======
  @Test
  void getAllPosts_esEmpty_returnEmptyPage() {
    when(securityUtil.getCurrentUserId()).thenReturn(null);

    when(postSearchService.searchPostIds(
      any(), any(), any(), any(), any(),
      isNull(), anyList(), anyList(), eq(1), eq(10)
    )).thenReturn(new SearchResult(List.of(), 0L));

    Page<PostResponse> result = postService.getAllPosts(
      "abc", null, null, null, null, 1, 10
    );

    assertTrue(result.getContent().isEmpty());
    verify(postRepo, never()).findAll(any(Specification.class));
  }
}
