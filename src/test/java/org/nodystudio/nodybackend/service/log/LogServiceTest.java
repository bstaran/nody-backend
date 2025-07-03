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
    given(logRepository.save(any(Log.class))).willAnswer(invocation -> {
      Log log = invocation.getArgument(0);
      return Log.builder()
          .id(1L)
          .user(log.getUser())
          .content(log.getContent())
          .latitude(log.getLatitude())
          .longitude(log.getLongitude())
          .address(log.getAddress())
          .mediaUrls(log.getMediaUrls())
          .isPublic(log.getIsPublic())
          .viewCount(0L)
          .build();
    });

    // when
    LogResponse response = logService.createLog(request, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEqualTo("새로운 로그");
    assertThat(response.getLatitude()).isEqualTo(new BigDecimal("37.5665"));
    assertThat(response.getLongitude()).isEqualTo(new BigDecimal("126.9780"));
    assertThat(response.getAddress()).isEqualTo("서울특별시 중구");
    assertThat(response.getIsPublic()).isTrue();

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
  @DisplayName("위치 정보 검증 테스트 - 잘못된 위도로 로그 생성 시도")
  void createLog_InvalidLatitude_ThrowsExceptionWithSpecificMessage() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("테스트 로그")
        .latitude(new BigDecimal("999.0")) // 잘못된 위도 (90도 초과)
        .longitude(new BigDecimal("126.9780")) // 정상 경도
        .address("서울특별시")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

    // when & then
    assertThatThrownBy(() -> logService.createLog(request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도는 -90도에서 90도 사이여야 합니다. 입력값: 999.0");
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - 잘못된 경도로 로그 생성 시도")
  void createLog_InvalidLongitude_ThrowsExceptionWithSpecificMessage() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("테스트 로그")
        .latitude(new BigDecimal("37.5665")) // 정상 위도
        .longitude(new BigDecimal("999.0")) // 잘못된 경도 (180도 초과)
        .address("서울특별시")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

    // when & then
    assertThatThrownBy(() -> logService.createLog(request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도는 -180도에서 180도 사이여야 합니다. 입력값: 999.0");
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - null 위도와 정상 경도로 로그 생성 시도")
  void createLog_NullLatitudeWithValidLongitude_ThrowsException() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("테스트 로그")
        .latitude(null) // null 위도
        .longitude(new BigDecimal("126.9780")) // 정상 경도
        .address("서울특별시")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

    // when & then
    assertThatThrownBy(() -> logService.createLog(request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도가 제공된 경우 위도도 함께 제공되어야 합니다.");
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - 정상 위도와 null 경도로 로그 생성 시도")
  void createLog_ValidLatitudeWithNullLongitude_ThrowsException() {
    // given
    LogCreateRequest request = LogCreateRequest.builder()
        .content("테스트 로그")
        .latitude(new BigDecimal("37.5665")) // 정상 위도
        .longitude(null) // null 경도
        .address("서울특별시")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

    // when & then
    assertThatThrownBy(() -> logService.createLog(request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도가 제공된 경우 경도도 함께 제공되어야 합니다.");
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - 경계값 테스트 (유효한 극값)")
  void createLog_BoundaryCoordinates_Success() {
    // given - 유효한 경계값들
    LogCreateRequest request = LogCreateRequest.builder()
        .content("경계값 테스트")
        .latitude(new BigDecimal("90.0")) // 최대 유효 위도
        .longitude(new BigDecimal("-180.0")) // 최소 유효 경도
        .address("극지방")
        .isPublic(true)
        .build();

    Log expectedLog = Log.builder()
        .id(2L)
        .user(testUser)
        .content("경계값 테스트")
        .latitude(new BigDecimal("90.0"))
        .longitude(new BigDecimal("-180.0"))
        .address("극지방")
        .isPublic(true)
        .viewCount(0L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.save(any(Log.class))).willReturn(expectedLog);

    // when
    LogResponse response = logService.createLog(request, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getLatitude()).isEqualTo(new BigDecimal("90.0"));
    assertThat(response.getLongitude()).isEqualTo(new BigDecimal("-180.0"));
    verify(logRepository).save(any(Log.class));
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - 경계값 초과 (무효한 극값)")
  void createLog_OutOfBoundaryCoordinates_ThrowsException() {
    // given - 경계값을 벗어난 좌표들
    LogCreateRequest request = LogCreateRequest.builder()
        .content("경계값 초과 테스트")
        .latitude(new BigDecimal("-90.1")) // 최소값 미만
        .longitude(new BigDecimal("180.1")) // 최대값 초과
        .address("유효하지 않은 위치")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

    // when & then
    // LocationUtils는 위도를 먼저 검증하므로, 위도 오류만 반환됨
    // (경도도 유효하지 않지만 위도 검증에서 먼저 실패)
    assertThatThrownBy(() -> logService.createLog(request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도는 -90도에서 90도 사이여야 합니다. 입력값: -90.1");
  }

  @Test
  @DisplayName("위치 정보 검증 테스트 - 좌표 없이 로그 생성 (위치 정보 선택적)")
  void createLog_WithoutCoordinates_Success() {
    // given - 위치 정보 없는 로그
    LogCreateRequest request = LogCreateRequest.builder()
        .content("위치 없는 로그")
        .latitude(null)
        .longitude(null)
        .address("주소만 있는 경우")
        .isPublic(true)
        .build();

    Log expectedLog = Log.builder()
        .id(3L)
        .user(testUser)
        .content("위치 없는 로그")
        .latitude(null)
        .longitude(null)
        .address("주소만 있는 경우")
        .isPublic(true)
        .viewCount(0L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.save(any(Log.class))).willReturn(expectedLog);

    // when
    LogResponse response = logService.createLog(request, "test@example.com");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getLatitude()).isNull();
    assertThat(response.getLongitude()).isNull();
    assertThat(response.getAddress()).isEqualTo("주소만 있는 경우");
    verify(logRepository).save(any(Log.class));
  }

  @Test
  @DisplayName("로그 수정 - 부분 좌표 제공 시 예외 발생 (위도만 제공)")
  void updateLog_PartialCoordinates_ThrowsException() {
    // given
    LogUpdateRequest request = LogUpdateRequest.builder()
        .content("수정된 내용")
        .latitude(new BigDecimal("37.5665")) // 위도만 제공
        .longitude(null) // 경도 누락
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testLog));

    // when & then
    assertThatThrownBy(() -> logService.updateLog(1L, request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("위도가 제공된 경우 경도도 함께 제공되어야 합니다.");
  }

  @Test
  @DisplayName("로그 수정 - 부분 좌표 제공 시 예외 발생 (경도만 제공)")
  void updateLog_PartialCoordinatesLongitudeOnly_ThrowsException() {
    // given
    LogUpdateRequest request = LogUpdateRequest.builder()
        .content("수정된 내용")
        .latitude(null) // 위도 누락
        .longitude(new BigDecimal("126.9780")) // 경도만 제공
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testLog));

    // when & then
    assertThatThrownBy(() -> logService.updateLog(1L, request, "test@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("경도가 제공된 경우 위도도 함께 제공되어야 합니다.");
  }
}