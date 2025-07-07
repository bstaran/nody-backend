package org.nodystudio.nodybackend.controller.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;
import org.nodystudio.nodybackend.service.thread.ThreadService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ThreadControllerTest {

  @Mock
  private ThreadService threadService;

  @InjectMocks
  private ThreadController threadController;

  private ThreadResponse threadResponse;
  private User mockUser;

  @BeforeEach
  void setUp() {
    // Mock User 생성
    mockUser = User.builder()
        .id(1L)
        .provider("google")
        .socialId("123456789")
        .email("test@example.com")
        .nickname("테스트유저")
        .role(RoleType.USER)
        .isActive(true)
        .build();

    UserSummaryResponse userResponse = UserSummaryResponse.builder()
        .id(1L)
        .nickname("testuser")
        .build();

    threadResponse = ThreadResponse.builder()
        .id(1L)
        .content("테스트 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .user(userResponse)
        .log(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .isLinkedToLog(false)
        .isIndependent(true)
        .build();
  }

  @Test
  @DisplayName("POST /api/threads - 스레드 생성 API 테스트")
  void createThread_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("새 스레드 내용")
        .isPublic(true)
        .build();

    given(threadService.createThread(any(ThreadCreateRequest.class), eq("test@example.com")))
        .willReturn(threadResponse);

    // when
    ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.createThread(request, mockUser);

    // then
    assertEquals(201, response.getStatusCode().value());
    assertNotNull(response.getBody());
    ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
    assertEquals(200, body.getStatus());
    assertEquals(1L, body.getData().getId());
    assertEquals("테스트 스레드 내용", body.getData().getContent());
    assertEquals("스레드가 성공적으로 생성되었습니다.", body.getMessage());

    verify(threadService).createThread(any(ThreadCreateRequest.class), eq("test@example.com"));
  }

  @Test
  @DisplayName("GET /api/threads/{id} - 스레드 단건 조회 API 테스트")
  void getThread_Success() {
    // given
    given(threadService.getThread(1L, "test@example.com")).willReturn(threadResponse);

    // when
    ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.getThread(1L, mockUser);

    // then
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
    assertEquals(200, body.getStatus());
    assertEquals(1L, body.getData().getId());
    assertEquals("테스트 스레드 내용", body.getData().getContent());
    assertEquals("스레드 조회가 완료되었습니다.", body.getMessage());

    verify(threadService).getThread(1L, "test@example.com");
  }

  @Test
  @DisplayName("GET /api/threads/{id} - 비로그인 사용자 공개 스레드 조회 테스트")
  void getThread_AnonymousUser_PublicThread() {
    // given
    given(threadService.getThread(1L, null)).willReturn(threadResponse);

    // when
    ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.getThread(1L, null);

    // then
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
    assertEquals(200, body.getStatus());
    assertTrue(body.getData().getIsPublic());

    verify(threadService).getThread(1L, null);
  }

  @Test
  @DisplayName("DELETE /api/threads/{id} - 스레드 삭제 API 테스트")
  void deleteThread_Success() {
    // given
    willDoNothing().given(threadService).deleteThread(1L, "test@example.com");

    // when
    ResponseEntity<ApiResponse<Void>> response = threadController.deleteThread(1L, mockUser);

    // then
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    ApiResponse<Void> body = Objects.requireNonNull(response.getBody());
    assertEquals(200, body.getStatus());
    assertNull(body.getData());
    assertEquals("스레드가 성공적으로 삭제되었습니다.", body.getMessage());

    verify(threadService).deleteThread(1L, "test@example.com");
  }
}