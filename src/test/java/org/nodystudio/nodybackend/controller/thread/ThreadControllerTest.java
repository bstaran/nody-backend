package org.nodystudio.nodybackend.controller.thread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UnauthorizedException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.code.ErrorCode;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadSearchRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadUpdateRequest;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.nodystudio.nodybackend.service.thread.ThreadService;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@ExtendWith(MockitoExtension.class)
class ThreadControllerTest {

  @Mock
  private ThreadService threadService;

  @InjectMocks
  private ThreadController threadController;

  private ThreadResponse threadResponse;
  private Validator validator;

  @BeforeEach
  void setUp() {
    // Validator 설정
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

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

  /**
   * 테스트용 CustomUserDetails 객체를 생성합니다.
   *
   * @return 설정된 CustomUserDetails 객체
   */
  private CustomUserDetails createMockUserDetails() {
    User mockUser = mock(User.class);
    lenient().when(mockUser.getId()).thenReturn(1L);
    lenient().when(mockUser.getEmail()).thenReturn("test@example.com");
    lenient().when(mockUser.getRoles())
        .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));
    lenient().when(mockUser.getIsActive()).thenReturn(true);

    return new CustomUserDetails(mockUser);
  }

  @Test
  @DisplayName("POST /api/threads - 스레드 생성 API 테스트")
  void createThread_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("새 스레드 내용")
        .isPublic(true)
        .build();

    ThreadResponse createResponse = ThreadResponse.builder()
        .id(1L)
        .content("새 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .user(UserSummaryResponse.builder()
            .id(1L)
            .nickname("testuser")
            .build())
        .log(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .isLinkedToLog(false)
        .isIndependent(true)
        .build();

    given(threadService.createThread(any(ThreadCreateRequest.class), eq("test@example.com")))
        .willReturn(createResponse);

    // when
    CustomUserDetails userDetails = createMockUserDetails();
    ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.createThread(
        userDetails, request);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getId()).isEqualTo(1L);
    assertThat(body.getData().getContent()).isEqualTo("새 스레드 내용");
    assertThat(body.getMessage()).isEqualTo("스레드가 성공적으로 생성되었습니다.");

    verify(threadService).createThread(any(ThreadCreateRequest.class), eq("test@example.com"));
  }

  @Test
  @DisplayName("GET /api/threads/{id} - 스레드 단건 조회 API 테스트")
  void getThread_Success() {
    // given
    given(threadService.getThread(1L, null)).willReturn(threadResponse);

    // when
    ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.getThread(1L, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getId()).isEqualTo(1L);
    assertThat(body.getData().getContent()).isEqualTo("테스트 스레드 내용");
    assertThat(body.getMessage()).isEqualTo("스레드 조회가 완료되었습니다.");

    verify(threadService).getThread(1L, null);
  }

  @Test
  @DisplayName("GET /api/threads/{id} - 비로그인 사용자 공개 스레드 조회 테스트")
  void getThread_AnonymousUser_PublicThread() {
    // given
    given(threadService.getThread(1L, null)).willReturn(threadResponse);

    // when
    ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.getThread(1L, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getIsPublic()).isTrue();

    verify(threadService).getThread(1L, null);
  }

  @Test
  @DisplayName("DELETE /api/threads/{id} - 스레드 삭제 API 테스트")
  void deleteThread_Success() {
    // given
    willDoNothing().given(threadService).deleteThread(1L, "test@example.com");

    // when
    CustomUserDetails userDetails = createMockUserDetails();
    ResponseEntity<ApiResponse<Void>> response = threadController.deleteThread(1L, userDetails);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Void> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData()).isNull();
    assertThat(body.getMessage()).isEqualTo("스레드가 성공적으로 삭제되었습니다.");

    verify(threadService).deleteThread(1L, "test@example.com");
  }

  @Test
  @DisplayName("ThreadSearchRequest - 페이지 크기 최대값 초과 검증 테스트")
  void threadSearchRequest_PageSizeExceedsMax_ValidationError() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(0)
        .size(101)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getMessage().contains("100 이하여야 합니다"));
  }

  @Test
  @DisplayName("ThreadSearchRequest - 페이지 크기 최소값 미만 검증 테스트")
  void threadSearchRequest_PageSizeBelowMin_ValidationError() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(0)
        .size(0)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getMessage().contains("1 이상이어야 합니다"));
  }

  @Test
  @DisplayName("ThreadSearchRequest - 페이지 크기 정상 범위 검증 테스트")
  void threadSearchRequest_PageSizeValid_NoViolation() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(0)
        .size(100)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Nested
  @DisplayName("PUT /api/threads/{id} - 스레드 수정")
  class UpdateThreadTests {

    @Test
    @DisplayName("유효한 요청으로 스레드 수정 시 성공한다")
    void updateThread_WithValidRequest_ShouldReturnUpdatedThread() {
      // given
      Long threadId = 1L;
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content("수정된 스레드 내용")
          .isPublic(false)
          .build();

      ThreadResponse updatedResponse = ThreadResponse.builder()
          .id(threadId)
          .content("수정된 스레드 내용")
          .isPublic(false)
          .viewCount(0L)
          .user(UserSummaryResponse.builder()
              .id(1L)
              .nickname("testuser")
              .build())
          .log(null)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isLinkedToLog(false)
          .isIndependent(true)
          .build();

      given(threadService.updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com")))
          .willReturn(updatedResponse);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.updateThread(
          threadId, userDetails, request);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getStatus()).isEqualTo(200);
      assertThat(body.getData().getId()).isEqualTo(threadId);
      assertThat(body.getData().getContent()).isEqualTo("수정된 스레드 내용");
      assertThat(body.getData().getIsPublic()).isFalse();
      assertThat(body.getMessage()).isEqualTo("스레드가 성공적으로 수정되었습니다.");

      verify(threadService).updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("내용만 수정하는 경우 성공한다")
    void updateThread_ContentOnly_ShouldUpdateContent() {
      // given
      Long threadId = 1L;
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content("내용만 수정")
          .build();

      ThreadResponse updatedResponse = ThreadResponse.builder()
          .id(threadId)
          .content("내용만 수정")
          .isPublic(true)
          .viewCount(0L)
          .user(UserSummaryResponse.builder()
              .id(1L)
              .nickname("testuser")
              .build())
          .log(null)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isLinkedToLog(false)
          .isIndependent(true)
          .build();

      given(threadService.updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com")))
          .willReturn(updatedResponse);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.updateThread(
          threadId, userDetails, request);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getContent()).isEqualTo("내용만 수정");

      verify(threadService).updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("공개 설정만 수정하는 경우 성공한다")
    void updateThread_PublicSettingOnly_ShouldUpdatePublicSetting() {
      // given
      Long threadId = 1L;
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .isPublic(false)
          .build();

      ThreadResponse updatedResponse = ThreadResponse.builder()
          .id(threadId)
          .content("테스트 스레드 내용")
          .isPublic(false)
          .viewCount(0L)
          .user(UserSummaryResponse.builder()
              .id(1L)
              .nickname("testuser")
              .build())
          .log(null)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isLinkedToLog(false)
          .isIndependent(true)
          .build();

      given(threadService.updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com")))
          .willReturn(updatedResponse);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.updateThread(
          threadId, userDetails, request);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getIsPublic()).isFalse();

      verify(threadService).updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("로그 연결을 해제하는 경우 성공한다")
    void updateThread_DisconnectLog_ShouldDisconnectFromLog() {
      // given
      Long threadId = 1L;
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .disconnectLog(true)
          .build();

      ThreadResponse updatedResponse = ThreadResponse.builder()
          .id(threadId)
          .content("테스트 스레드 내용")
          .isPublic(true)
          .viewCount(0L)
          .user(UserSummaryResponse.builder()
              .id(1L)
              .nickname("testuser")
              .build())
          .log(null)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isLinkedToLog(false)
          .isIndependent(true)
          .build();

      given(threadService.updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com")))
          .willReturn(updatedResponse);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<ThreadResponse>> response = threadController.updateThread(
          threadId, userDetails, request);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<ThreadResponse> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getIsLinkedToLog()).isFalse();
      assertThat(body.getData().getIsIndependent()).isTrue();

      verify(threadService).updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com"));
    }
  }

  @Nested
  @DisplayName("GET /api/threads - 스레드 목록 조회")
  class GetThreadsTests {

    @Test
    @DisplayName("검색 조건 없이 전체 목록 조회 시 성공한다")
    void getThreads_WithoutSearch_ShouldReturnAllThreads() {
      // given
      ThreadSearchRequest searchRequest = ThreadSearchRequest.builder().build();
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> threadList = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("첫 번째 스레드")
              .isPublic(true)
              .viewCount(5L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build(),
          ThreadResponse.builder()
              .id(2L)
              .content("두 번째 스레드")
              .isPublic(true)
              .viewCount(3L)
              .user(UserSummaryResponse.builder().id(2L).nickname("user2").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(threadList, pageable, threadList.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq("test@example.com")))
          .willReturn(threadPage);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getThreads(
          searchRequest, pageable, userDetails);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getStatus()).isEqualTo(200);
      assertThat(body.getData().getContent()).hasSize(2);
      assertThat(body.getMessage()).isEqualTo("스레드 목록 조회가 완료되었습니다.");

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("키워드 검색으로 스레드 목록 조회 시 성공한다")
    void getThreads_WithKeyword_ShouldReturnFilteredThreads() {
      // given
      ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
          .keyword("테스트")
          .build();
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> filteredList = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("테스트 키워드가 포함된 스레드")
              .isPublic(true)
              .viewCount(2L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(filteredList, pageable, filteredList.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq("test@example.com")))
          .willReturn(threadPage);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getThreads(
          searchRequest, pageable, userDetails);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getContent()).hasSize(1);
      assertThat(body.getData().getContent().get(0).getContent()).contains("테스트");

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("특정 로그의 스레드 목록 조회 시 성공한다")
    void getThreads_WithLogId_ShouldReturnLogThreads() {
      // given
      ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
          .logId(10L)
          .build();
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> logThreadList = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("로그에 연결된 스레드")
              .isPublic(true)
              .viewCount(1L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(true)
              .isIndependent(false)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(logThreadList, pageable, logThreadList.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq("test@example.com")))
          .willReturn(threadPage);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getThreads(
          searchRequest, pageable, userDetails);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getContent()).hasSize(1);
      assertThat(body.getData().getContent().get(0).getIsLinkedToLog()).isTrue();

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("페이징 정보와 함께 목록 조회 시 성공한다")
    void getThreads_WithPagination_ShouldReturnPagedResults() {
      // given
      ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
          .page(1)
          .size(10)
          .build();
      Pageable pageable = PageRequest.of(1, 10);

      List<ThreadResponse> pagedList = List.of(
          ThreadResponse.builder()
              .id(11L)
              .content("두 번째 페이지의 첫 번째 스레드")
              .isPublic(true)
              .viewCount(0L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(pagedList, pageable, 25);

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq("test@example.com")))
          .willReturn(threadPage);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getThreads(
          searchRequest, pageable, userDetails);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getNumber()).isEqualTo(1);
      assertThat(body.getData().getSize()).isEqualTo(10);
      assertThat(body.getData().getTotalElements()).isEqualTo(25);

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("비로그인 사용자도 공개 스레드 목록을 조회할 수 있다")
    void getThreads_AnonymousUser_ShouldReturnPublicThreads() {
      // given
      ThreadSearchRequest searchRequest = ThreadSearchRequest.builder().build();
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> publicThreadList = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("공개 스레드")
              .isPublic(true)
              .viewCount(10L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(publicThreadList, pageable, publicThreadList.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq(null)))
          .willReturn(threadPage);

      // when
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getThreads(
          searchRequest, pageable, null);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getContent()).hasSize(1);
      assertThat(body.getData().getContent().get(0).getIsPublic()).isTrue();

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq(null));
    }
  }

  @Nested
  @DisplayName("GET /api/threads/independent - 독립 스레드 목록 조회")
  class GetIndependentThreadsTests {

    @Test
    @DisplayName("독립 스레드 목록 조회 시 성공한다")
    void getIndependentThreads_ShouldReturnIndependentThreads() {
      // given
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> independentThreads = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("독립 스레드 1")
              .isPublic(true)
              .viewCount(5L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .log(null)
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build(),
          ThreadResponse.builder()
              .id(2L)
              .content("독립 스레드 2")
              .isPublic(true)
              .viewCount(3L)
              .user(UserSummaryResponse.builder().id(2L).nickname("user2").build())
              .log(null)
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(independentThreads, pageable, independentThreads.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq("test@example.com")))
          .willReturn(threadPage);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getIndependentThreads(
          pageable, userDetails);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getStatus()).isEqualTo(200);
      assertThat(body.getData().getContent()).hasSize(2);
      assertThat(body.getData().getContent().get(0).getIsIndependent()).isTrue();
      assertThat(body.getData().getContent().get(1).getIsIndependent()).isTrue();
      assertThat(body.getMessage()).isEqualTo("독립 스레드 목록 조회가 완료되었습니다.");

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("비로그인 사용자도 공개 독립 스레드 목록을 조회할 수 있다")
    void getIndependentThreads_AnonymousUser_ShouldReturnPublicIndependentThreads() {
      // given
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> publicIndependentThreads = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("공개 독립 스레드")
              .isPublic(true)
              .viewCount(10L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .log(null)
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(false)
              .isIndependent(true)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(publicIndependentThreads, pageable, publicIndependentThreads.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq(null)))
          .willReturn(threadPage);

      // when
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getIndependentThreads(
          pageable, null);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getContent()).hasSize(1);
      assertThat(body.getData().getContent().get(0).getIsPublic()).isTrue();
      assertThat(body.getData().getContent().get(0).getIsIndependent()).isTrue();

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq(null));
    }
  }

  @Nested
  @DisplayName("GET /api/threads/linked - 로그 연결 스레드 목록 조회")
  class GetLinkedThreadsTests {

    @Test
    @DisplayName("로그 연결 스레드 목록 조회 시 성공한다")
    void getLinkedThreads_ShouldReturnLinkedThreads() {
      // given
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> linkedThreads = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("로그 연결 스레드 1")
              .isPublic(true)
              .viewCount(8L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(true)
              .isIndependent(false)
              .build(),
          ThreadResponse.builder()
              .id(2L)
              .content("로그 연결 스레드 2")
              .isPublic(true)
              .viewCount(6L)
              .user(UserSummaryResponse.builder().id(2L).nickname("user2").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(true)
              .isIndependent(false)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(linkedThreads, pageable, linkedThreads.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq("test@example.com")))
          .willReturn(threadPage);

      // when
      CustomUserDetails userDetails = createMockUserDetails();
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getLinkedThreads(
          pageable, userDetails);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getStatus()).isEqualTo(200);
      assertThat(body.getData().getContent()).hasSize(2);
      assertThat(body.getData().getContent().get(0).getIsLinkedToLog()).isTrue();
      assertThat(body.getData().getContent().get(1).getIsLinkedToLog()).isTrue();
      assertThat(body.getMessage()).isEqualTo("로그 연결 스레드 목록 조회가 완료되었습니다.");

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("비로그인 사용자도 공개 로그 연결 스레드 목록을 조회할 수 있다")
    void getLinkedThreads_AnonymousUser_ShouldReturnPublicLinkedThreads() {
      // given
      Pageable pageable = PageRequest.of(0, 20);

      List<ThreadResponse> publicLinkedThreads = List.of(
          ThreadResponse.builder()
              .id(1L)
              .content("공개 로그 연결 스레드")
              .isPublic(true)
              .viewCount(15L)
              .user(UserSummaryResponse.builder().id(1L).nickname("user1").build())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .isLinkedToLog(true)
              .isIndependent(false)
              .build()
      );

      Page<ThreadResponse> threadPage = new PageImpl<>(publicLinkedThreads, pageable, publicLinkedThreads.size());

      given(threadService.searchThreads(any(ThreadSearchRequest.class), eq(null)))
          .willReturn(threadPage);

      // when
      ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = threadController.getLinkedThreads(
          pageable, null);

      // then
      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
      assertThat(body.getData().getContent()).hasSize(1);
      assertThat(body.getData().getContent().get(0).getIsPublic()).isTrue();
      assertThat(body.getData().getContent().get(0).getIsLinkedToLog()).isTrue();

      verify(threadService).searchThreads(any(ThreadSearchRequest.class), eq(null));
    }
  }

  @Nested
  @DisplayName("예외 처리 테스트")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("존재하지 않는 스레드 조회 시 예외가 발생한다")
    void getThread_WithNonExistentId_ShouldThrowException() {
      // given
      Long nonExistentId = 999L;
      given(threadService.getThread(eq(nonExistentId), eq(null)))
          .willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다."));

      // when & then
      assertThatThrownBy(() -> threadController.getThread(nonExistentId, null))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("스레드를 찾을 수 없습니다.");

      verify(threadService).getThread(eq(nonExistentId), eq(null));
    }

    @Test
    @DisplayName("존재하지 않는 스레드 수정 시 예외가 발생한다")
    void updateThread_WithNonExistentThread_ShouldThrowException() {
      // given
      Long nonExistentId = 999L;
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content("수정 내용")
          .build();

      willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다."))
          .given(threadService).updateThread(eq(nonExistentId), any(ThreadUpdateRequest.class), eq("test@example.com"));

      // when & then
      CustomUserDetails userDetails = createMockUserDetails();
      assertThatThrownBy(() -> threadController.updateThread(nonExistentId, userDetails, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("스레드를 찾을 수 없습니다.");

      verify(threadService).updateThread(eq(nonExistentId), any(ThreadUpdateRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("권한이 없는 사용자가 스레드 수정 시 예외가 발생한다")
    void updateThread_WithUnauthorizedUser_ShouldThrowException() {
      // given
      Long threadId = 1L;
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content("수정 내용")
          .build();

      willThrow(new UnauthorizedException("스레드 수정 권한이 없습니다."))
          .given(threadService).updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com"));

      // when & then
      CustomUserDetails userDetails = createMockUserDetails();
      assertThatThrownBy(() -> threadController.updateThread(threadId, userDetails, request))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("스레드 수정 권한이 없습니다.");

      verify(threadService).updateThread(eq(threadId), any(ThreadUpdateRequest.class), eq("test@example.com"));
    }

    @Test
    @DisplayName("권한이 없는 사용자가 스레드 삭제 시 예외가 발생한다")
    void deleteThread_WithUnauthorizedUser_ShouldThrowException() {
      // given
      Long threadId = 1L;

      willThrow(new UnauthorizedException("스레드 삭제 권한이 없습니다."))
          .given(threadService).deleteThread(eq(threadId), eq("test@example.com"));

      // when & then
      CustomUserDetails userDetails = createMockUserDetails();
      assertThatThrownBy(() -> threadController.deleteThread(threadId, userDetails))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("스레드 삭제 권한이 없습니다.");

      verify(threadService).deleteThread(eq(threadId), eq("test@example.com"));
    }

    @Test
    @DisplayName("존재하지 않는 스레드 삭제 시 예외가 발생한다")
    void deleteThread_WithNonExistentThread_ShouldThrowException() {
      // given
      Long nonExistentId = 999L;

      willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다."))
          .given(threadService).deleteThread(eq(nonExistentId), eq("test@example.com"));

      // when & then
      CustomUserDetails userDetails = createMockUserDetails();
      assertThatThrownBy(() -> threadController.deleteThread(nonExistentId, userDetails))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("스레드를 찾을 수 없습니다.");

      verify(threadService).deleteThread(eq(nonExistentId), eq("test@example.com"));
    }

    @Test
    @DisplayName("스레드 생성 시 사용자를 찾을 수 없는 경우 예외가 발생한다")
    void createThread_WithNonExistentUser_ShouldThrowException() {
      // given
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content("새 스레드 내용")
          .isPublic(true)
          .build();

      given(threadService.createThread(any(ThreadCreateRequest.class), eq("nonexistent@example.com")))
          .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

      // when & then
      User mockUser = mock(User.class);
      lenient().when(mockUser.getId()).thenReturn(999L);
      lenient().when(mockUser.getEmail()).thenReturn("nonexistent@example.com");
      lenient().when(mockUser.getIsActive()).thenReturn(true);
      CustomUserDetails userDetails = new CustomUserDetails(mockUser);

      assertThatThrownBy(() -> threadController.createThread(userDetails, request))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessage("사용자를 찾을 수 없습니다.");

      verify(threadService).createThread(any(ThreadCreateRequest.class), eq("nonexistent@example.com"));
    }

    @Test
    @DisplayName("비공개 스레드를 권한 없는 사용자가 조회 시 예외가 발생한다")
    void getThread_PrivateThreadByUnauthorizedUser_ShouldThrowException() {
      // given
      Long privateThreadId = 1L;

      given(threadService.getThread(eq(privateThreadId), eq("other@example.com")))
          .willThrow(new UnauthorizedException("비공개 스레드에 접근할 권한이 없습니다."));

      // when & then
      User otherUser = mock(User.class);
      lenient().when(otherUser.getId()).thenReturn(2L);
      lenient().when(otherUser.getEmail()).thenReturn("other@example.com");
      lenient().when(otherUser.getIsActive()).thenReturn(true);
      CustomUserDetails otherUserDetails = new CustomUserDetails(otherUser);

      assertThatThrownBy(() -> threadController.getThread(privateThreadId, otherUserDetails))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("비공개 스레드에 접근할 권한이 없습니다.");

      verify(threadService).getThread(eq(privateThreadId), eq("other@example.com"));
    }
  }

  @Nested
  @DisplayName("유효성 검증 실패 테스트")
  class ValidationFailureTests {

    @Test
    @DisplayName("ThreadCreateRequest - 빈 내용으로 스레드 생성 시 검증 오류가 발생한다")
    void createThreadRequest_WithBlankContent_ValidationError() {
      // given
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content("")
          .isPublic(true)
          .build();

      // when
      Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

      // then
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getMessage().contains("필수입니다"));
    }

    @Test
    @DisplayName("ThreadCreateRequest - 내용이 5000자를 초과할 때 검증 오류가 발생한다")
    void createThreadRequest_WithContentTooLong_ValidationError() {
      // given
      String longContent = "a".repeat(5001);
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(longContent)
          .isPublic(true)
          .build();

      // when
      Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

      // then
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getMessage().contains("5000자 이하여야 합니다"));
    }

    @Test
    @DisplayName("ThreadUpdateRequest - 빈 내용으로 수정 시 검증 오류가 발생한다")
    void updateThreadRequest_WithBlankContent_ValidationError() {
      // given
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content("")
          .build();

      // when
      Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

      // then
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getMessage().contains("공백일 수 없습니다"));
    }

    @Test
    @DisplayName("ThreadUpdateRequest - 내용이 5000자를 초과할 때 검증 오류가 발생한다")
    void updateThreadRequest_WithContentTooLong_ValidationError() {
      // given
      String longContent = "a".repeat(5001);
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content(longContent)
          .build();

      // when
      Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

      // then
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getMessage().contains("5000자 이하여야 합니다"));
    }

    @Test
    @DisplayName("ThreadUpdateRequest - logId와 disconnectLog가 동시에 설정된 경우 검증 오류가 발생한다")
    void updateThreadRequest_WithBothLogIdAndDisconnectLog_ValidationError() {
      // given
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .logId(10L)
          .disconnectLog(true)
          .build();

      // when
      Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

      // then
      assertThat(violations).isNotEmpty();
      assertThat(violations).anyMatch(v -> v.getMessage().contains("동시에 요청할 수 없습니다"));
    }

    @Test
    @DisplayName("ThreadUpdateRequest - 유효한 값들로 검증 시 오류가 발생하지 않는다")
    void updateThreadRequest_WithValidValues_NoViolation() {
      // given
      ThreadUpdateRequest request1 = ThreadUpdateRequest.builder()
          .content("유효한 내용")
          .isPublic(false)
          .logId(10L)
          .build();

      ThreadUpdateRequest request2 = ThreadUpdateRequest.builder()
          .content("다른 유효한 내용")
          .isPublic(true)
          .disconnectLog(true)
          .build();

      // when
      Set<ConstraintViolation<ThreadUpdateRequest>> violations1 = validator.validate(request1);
      Set<ConstraintViolation<ThreadUpdateRequest>> violations2 = validator.validate(request2);

      // then
      assertThat(violations1).isEmpty();
      assertThat(violations2).isEmpty();
    }
  }

  @RestControllerAdvice
  static class TestValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Object>> handleValidationException(
        MethodArgumentNotValidException ex) {
      String errorMessage = ex.getBindingResult().getFieldErrors().stream()
          .map(DefaultMessageSourceResolvable::getDefaultMessage)
          .collect(Collectors.joining(", "));

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(org.nodystudio.nodybackend.dto.ApiResponse.error(ErrorCode.VALIDATION_ERROR,
              errorMessage));
    }
  }
}