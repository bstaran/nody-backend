package org.nodystudio.nodybackend.controller.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.log.LogCreateRequest;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.log.LogSearchRequest;
import org.nodystudio.nodybackend.dto.log.LogUpdateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.nodystudio.nodybackend.service.log.LogService;
import org.nodystudio.nodybackend.service.thread.ThreadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/**
 * - 로그 CRUD 기능 테스트 - 위치 기반 검색 테스트 - 권한 검증 테스트 - 공개/비공개 필터링 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogController 통합 테스트")
class LogControllerTest {

  @Mock
  private LogService logService;

  @Mock
  private ThreadService threadService;

  @InjectMocks
  private LogController logController;

  private LogResponse testLogResponse;
  private CustomUserDetails mockUserDetails;

  @BeforeEach
  void setUp() {
    // Mock User와 CustomUserDetails 생성
    User mockUser = mock(User.class);
    lenient().when(mockUser.getId()).thenReturn(1L);
    lenient().when(mockUser.getEmail()).thenReturn("test@example.com");

    mockUserDetails = new CustomUserDetails(mockUser);

    UserSummaryResponse author = UserSummaryResponse.builder()
        .id(1L)
        .nickname("테스트유저")
        .build();

    testLogResponse = LogResponse.builder()
        .id(1L)
        .author(author)
        .content("테스트 로그 내용")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .address("서울특별시 중구")
        .isPublic(true)
        .viewCount(1L)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("POST /api/logs - 로그 생성 API 테스트")
  void createLog_Success() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("새로운 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .address("서울특별시 중구")
        .isPublic(true)
        .build();

    given(logService.createLog(any(LogCreateRequest.class), eq("test@example.com")))
        .willReturn(testLogResponse);

    // when
    ResponseEntity<ApiResponse<LogResponse>> response = logController.createLog(mockUserDetails,
        request);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<LogResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200); // ApiResponse.success()는 200을 반환
    assertThat(body.getData().getId()).isEqualTo(1L);
    assertThat(body.getData().getContent()).isEqualTo("테스트 로그 내용");
    assertThat(body.getMessage()).isEqualTo("로그가 성공적으로 생성되었습니다.");

    verify(logService).createLog(any(LogCreateRequest.class), eq("test@example.com"));
  }

  @Test
  @DisplayName("GET /api/logs/{id} - 로그 단건 조회 API 테스트")
  void getLog_Success() {
    // given
    given(logService.getLog(1L, null)).willReturn(testLogResponse);

    // when
    ResponseEntity<ApiResponse<LogResponse>> response = logController.getLog(1L, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<LogResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getId()).isEqualTo(1L);
    assertThat(body.getData().getContent()).isEqualTo("테스트 로그 내용");
    assertThat(body.getData().getViewCount()).isEqualTo(1L);
    assertThat(body.getMessage()).isEqualTo("로그 조회가 완료되었습니다.");

    verify(logService).getLog(1L, null);
  }

  @Test
  @DisplayName("GET /api/logs/{id} - 비로그인 사용자 공개 로그 조회 테스트")
  void getLog_AnonymousUser_PublicLog() {
    // given
    given(logService.getLog(1L, null)).willReturn(testLogResponse);

    // when
    ResponseEntity<ApiResponse<LogResponse>> response = logController.getLog(1L, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<LogResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getIsPublic()).isTrue();

    verify(logService).getLog(1L, null);
  }

  @Test
  @DisplayName("GET /api/logs - 위치 기반 로그 목록 조회 API 테스트")
  void getLogs_LocationBased_Success() {
    // given
    Page<LogResponse> logPage = new PageImpl<>(Arrays.asList(testLogResponse));
    LogSearchRequest searchRequest = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .radiusKm(new BigDecimal("10.0"))
        .page(0)
        .size(20)
        .build();

    given(logService.searchLogs(any(LogSearchRequest.class), eq(null))).willReturn(
        logPage);

    // when
    Pageable pageable = PageRequest.of(0, 20);
    ResponseEntity<ApiResponse<Page<LogResponse>>> response = logController.getLogs(searchRequest,
        pageable, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Page<LogResponse>> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getContent()).isNotNull();
    assertThat(body.getData().getContent().size()).isEqualTo(1);
    assertThat(body.getData().getContent().get(0).getId()).isEqualTo(1L);
    assertThat(body.getMessage()).isEqualTo("로그 목록 조회가 완료되었습니다.");

    verify(logService).searchLogs(any(LogSearchRequest.class), eq(null));
  }

  @Test
  @DisplayName("PUT /api/logs/{id} - 로그 수정 API 테스트")
  void updateLog_Success() {
    // given
    LogUpdateRequest request = LogUpdateRequest.builder()
        .content("수정된 로그 내용")
        .isPublic(false)
        .build();

    LogResponse updatedResponse = LogResponse.builder()
        .id(1L)
        .author(testLogResponse.getAuthor())
        .content("수정된 로그 내용")
        .latitude(testLogResponse.getLatitude())
        .longitude(testLogResponse.getLongitude())
        .address(testLogResponse.getAddress())
        .isPublic(false)
        .viewCount(testLogResponse.getViewCount())
        .createdAt(testLogResponse.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .build();

    given(logService.updateLog(eq(1L), any(LogUpdateRequest.class), eq("test@example.com")))
        .willReturn(updatedResponse);

    // when
    ResponseEntity<ApiResponse<LogResponse>> response = logController.updateLog(1L, mockUserDetails,
        request);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<LogResponse> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getContent()).isEqualTo("수정된 로그 내용");
    assertThat(body.getData().getIsPublic()).isFalse();
    assertThat(body.getMessage()).isEqualTo("로그가 성공적으로 수정되었습니다.");

    verify(logService).updateLog(eq(1L), any(LogUpdateRequest.class), eq("test@example.com"));
  }

  @Test
  @DisplayName("DELETE /api/logs/{id} - 로그 삭제 API 테스트")
  void deleteLog_Success() {
    // given
    willDoNothing().given(logService).deleteLog(1L, "test@example.com");

    // when
    ResponseEntity<ApiResponse<Void>> response = logController.deleteLog(1L, mockUserDetails);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Void> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData()).isNull();
    assertThat(body.getMessage()).isEqualTo("로그가 성공적으로 삭제되었습니다.");

    verify(logService).deleteLog(1L, "test@example.com");
  }

  @Test
  @DisplayName("GET /api/logs - 페이징 파라미터 검증 테스트")
  void getLogs_PaginationValidation() {
    // given
    Page<LogResponse> emptyPage = new PageImpl<>(Collections.emptyList());
    LogSearchRequest searchRequest = LogSearchRequest.builder()
        .page(0)
        .size(10)
        .build();

    given(logService.searchLogs(any(LogSearchRequest.class), isNull())).willReturn(emptyPage);

    // when
    ResponseEntity<ApiResponse<Page<LogResponse>>> response = logController.getLogs(searchRequest,
        null, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Page<LogResponse>> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getContent()).isEmpty();

    verify(logService).searchLogs(any(LogSearchRequest.class), isNull());
  }

  @Test
  @DisplayName("GET /api/logs/{logId}/threads - 로그별 스레드 목록 조회 API 테스트 (인증된 사용자)")
  void getThreadsByLog_AuthenticatedUser_Success() {
    // given
    Long logId = 1L;
    ThreadResponse threadResponse = ThreadResponse.builder()
        .id(1L)
        .content("테스트 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .user(UserSummaryResponse.builder()
            .id(1L)
            .nickname("테스트유저")
            .build())
        .isLinkedToLog(true)
        .isIndependent(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    Page<ThreadResponse> threadPage = new PageImpl<>(Arrays.asList(threadResponse));
    given(threadService.getThreadsByLog(eq(logId), eq(null), any())).willReturn(threadPage);

    // when
    Pageable pageable = PageRequest.of(0, 20);
    ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = logController.getLogThreads(
        logId, pageable, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getContent().size()).isEqualTo(1);
    assertThat(body.getData().getContent().get(0).getContent()).isEqualTo("테스트 스레드 내용");
    assertThat(body.getMessage()).isEqualTo("로그 스레드 목록 조회가 완료되었습니다.");

    verify(threadService).getThreadsByLog(eq(logId), eq(null), any());
  }

  @Test
  @DisplayName("GET /api/logs/{logId}/threads - 로그별 스레드 목록 조회 API 테스트 (익명 사용자)")
  void getThreadsByLog_AnonymousUser_Success() {
    // given
    Long logId = 1L;
    ThreadResponse threadResponse = ThreadResponse.builder()
        .id(1L)
        .content("공개 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .user(UserSummaryResponse.builder()
            .id(1L)
            .nickname("작성자")
            .build())
        .isLinkedToLog(true)
        .isIndependent(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    Page<ThreadResponse> threadPage = new PageImpl<>(Arrays.asList(threadResponse));
    given(threadService.getThreadsByLog(eq(logId), isNull(), any())).willReturn(threadPage);

    // when
    Pageable pageable = PageRequest.of(0, 20);
    ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = logController.getLogThreads(
        logId, pageable, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getContent().size()).isEqualTo(1);
    assertThat(body.getData().getContent().get(0).getContent()).isEqualTo("공개 스레드 내용");
    assertThat(body.getMessage()).isEqualTo("로그 스레드 목록 조회가 완료되었습니다.");

    verify(threadService).getThreadsByLog(eq(logId), isNull(), any());
  }

  @Test
  @DisplayName("GET /api/logs/{logId}/threads - 로그별 스레드 목록 조회 API 테스트 (빈 결과)")
  void getThreadsByLog_EmptyResult() {
    // given
    Long logId = 999L;
    Page<ThreadResponse> emptyPage = new PageImpl<>(Collections.emptyList());
    given(threadService.getThreadsByLog(eq(logId), eq(null), any())).willReturn(emptyPage);

    // when
    Pageable pageable = PageRequest.of(0, 20);
    ResponseEntity<ApiResponse<Page<ThreadResponse>>> response = logController.getLogThreads(
        logId, pageable, null);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    ApiResponse<Page<ThreadResponse>> body = Objects.requireNonNull(response.getBody());
    assertThat(body.getStatus()).isEqualTo(200);
    assertThat(body.getData().getContent()).isEmpty();
    assertThat(body.getMessage()).isEqualTo("로그 스레드 목록 조회가 완료되었습니다.");

    verify(threadService).getThreadsByLog(eq(logId), eq(null), any());
  }
}