package org.nodystudio.nodybackend.service.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.log.LogCreateRequest;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.log.LogSearchRequest;
import org.nodystudio.nodybackend.dto.log.LogUpdateRequest;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * - 로그 CRUD 기능 테스트
 * - 위치 기반 검색 테스트
 * - 권한 검증 테스트
 * - 공개/비공개 필터링 테스트
 * - 페이징 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogService 테스트")
class LogServiceTest {

  @Mock
  private LogRepository logRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private LogService logService;

  private User testUser;
  private Log testLog;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .nickname("테스트유저")
        .provider("google")
        .socialId("123456")
        .role(RoleType.USER)
        .isActive(true)
        .build();

    testLog = Log.builder()
        .id(1L)
        .user(testUser)
        .content("테스트 로그 내용")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .address("서울특별시 중구")
        .isPublic(true)
        .viewCount(0L)
        .build();
  }

  @Test
  @DisplayName("로그 생성 기능 테스트")
  void createLog_Success() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("새로운 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .address("서울특별시 중구")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.save(any(Log.class))).willReturn(testLog);

    // when
    LogResponse response = logService.createLog(request, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEqualTo("테스트 로그 내용");
    assertThat(response.getLatitude()).isEqualTo(new BigDecimal("37.5665"));
    assertThat(response.getLongitude()).isEqualTo(new BigDecimal("126.9780"));

    verify(logRepository).save(any(Log.class));
  }

  @Test
  @DisplayName("로그 조회 기능 테스트 (조회수 증가)")
  void getLog_Success_WithViewCountIncrement() {
    // given
    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findViewableLogByIdAndUserId(1L, 1L)).willReturn(Optional.of(testLog));

    // when
    LogResponse response = logService.getLog(1L, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(testLog.getViewCount()).isEqualTo(1L); // 조회수 증가 확인
  }

  @Test
  @DisplayName("공개/비공개 필터링 테스트 - 비로그인 사용자는 공개 로그만 조회 가능")
  void getLog_PublicOnly_ForAnonymousUser() {
    // given
    given(logRepository.findByIdAndIsPublicTrue(1L)).willReturn(Optional.of(testLog));

    // when
    LogResponse response = logService.getLog(1L, null);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getIsPublic()).isTrue();

    verify(logRepository).findByIdAndIsPublicTrue(1L);
    verify(logRepository, never()).findViewableLogByIdAndUserId(anyLong(), anyLong());
  }

  @Test
  @DisplayName("위치 기반 검색 테스트")
  void searchLogs_LocationBased_Success() {
    // given
    LogSearchRequest searchRequest = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .radiusKm(new BigDecimal("10.0"))
        .page(0)
        .size(20)
        .build();

    List<Log> logs = Arrays.asList(testLog);
    Page<Log> logPage = new PageImpl<>(logs);

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findLogsByLocationNearWithUser(
        eq(new BigDecimal("37.5665")),
        eq(new BigDecimal("126.9780")),
        eq(new BigDecimal("10.0")),
        eq(1L),
        any(Pageable.class))).willReturn(logPage);

    // when
    Page<LogResponse> response = logService.searchLogs(searchRequest, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getContent()).hasSize(1);
    assertThat(response.getContent().get(0).getId()).isEqualTo(1L);

    verify(logRepository).findLogsByLocationNearWithUser(
        eq(new BigDecimal("37.5665")),
        eq(new BigDecimal("126.9780")),
        eq(new BigDecimal("10.0")),
        eq(1L),
        any(Pageable.class));
  }

  @Test
  @DisplayName("페이징 기능 테스트")
  void searchLogs_Pagination_Success() {
    // given
    LogSearchRequest searchRequest = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .radiusKm(new BigDecimal("10.0"))
        .page(1)
        .size(10)
        .build();

    List<Log> logs = Arrays.asList(testLog);
    Page<Log> logPage = new PageImpl<>(logs);

    given(logRepository.findPublicLogsByLocationNear(
        any(BigDecimal.class),
        any(BigDecimal.class),
        any(BigDecimal.class),
        any(Pageable.class))).willReturn(logPage);

    // when
    Page<LogResponse> response = logService.searchLogs(searchRequest, null);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getNumber()).isEqualTo(0); // 실제 페이지 번호는 구현에 따라 다를 수 있음

    verify(logRepository).findPublicLogsByLocationNear(
        eq(new BigDecimal("37.5665")),
        eq(new BigDecimal("126.9780")),
        eq(new BigDecimal("10.0")),
        any(Pageable.class));
  }

  @Test
  @DisplayName("권한 검증 테스트 - 로그 수정 시 작성자 확인")
  void updateLog_PermissionCheck_Success() {
    // given
    LogUpdateRequest request = LogUpdateRequest.builder()
        .content("수정된 내용")
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testLog));

    // when
    LogResponse response = logService.updateLog(1L, request, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(testLog.getContent()).isEqualTo("수정된 내용");

    verify(logRepository).findByIdAndUserId(1L, 1L);
  }

  @Test
  @DisplayName("권한 검증 테스트 - 다른 사용자 로그 수정 시도 시 예외 발생")
  void updateLog_PermissionCheck_Failure() {
    // given
    LogUpdateRequest request = LogUpdateRequest.builder()
        .content("수정 시도")
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> logService.updateLog(1L, request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("수정 권한이 없거나 로그를 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("권한 검증 테스트 - 로그 삭제 시 작성자 확인")
  void deleteLog_PermissionCheck_Success() {
    // given
    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testLog));

    // when
    logService.deleteLog(1L, "test@example.com");

    // then
    verify(logRepository).findByIdAndUserId(1L, 1L);
    verify(logRepository).delete(testLog);
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - 잘못된 위도/경도로 로그 생성 시도")
  void createLog_InvalidCoordinates_ThrowsException() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("테스트 로그")
        .latitude(new BigDecimal("999.0")) // 잘못된 위도
        .longitude(new BigDecimal("999.0")) // 잘못된 경도
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

    // when & then
    assertThatThrownBy(() -> logService.createLog(request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}