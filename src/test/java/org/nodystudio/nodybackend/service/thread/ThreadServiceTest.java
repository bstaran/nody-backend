package org.nodystudio.nodybackend.service.thread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.enums.ThreadType;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadSearchRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadUpdateRequest;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UnauthorizedException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThreadService 테스트")
class ThreadServiceTest {

  @Mock
  private ThreadRepository threadRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private LogRepository logRepository;

  @InjectMocks
  private ThreadService threadService;

  private User testUser;
  private User otherUser;
  private Log testLog;
  private Thread testThread;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .nickname("testuser")
        .provider(OAuthProvider.GOOGLE)
        .socialId("12345")
        .role(RoleType.USER)
        .build();

    otherUser = User.builder()
        .id(2L)
        .email("other@example.com")
        .nickname("otheruser")
        .provider(OAuthProvider.GOOGLE)
        .socialId("67890")
        .role(RoleType.USER)
        .build();

    testLog = Log.builder()
        .id(1L)
        .user(testUser)
        .content("테스트 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .build();

    testThread = Thread.builder()
        .id(1L)
        .user(testUser)
        .log(testLog)
        .content("테스트 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .build();
  }

  @Test
  @DisplayName("독립 스레드 생성 성공")
  void createIndependentThread_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("새 스레드 내용")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
      Thread savedThread = invocation.getArgument(0);
      return Thread.builder()
          .id(1L)
          .user(savedThread.getUser())
          .log(savedThread.getLog())
          .content(savedThread.getContent())
          .isPublic(savedThread.getIsPublic())
          .viewCount(savedThread.getViewCount())
          .build();
    });

    // when
    ThreadResponse response = threadService.createThread(request, "test@example.com");

    // then
    assertThat(response.getContent()).isEqualTo("새 스레드 내용");
    assertThat(response.getIsIndependent()).isTrue();
    verify(threadRepository).save(any(Thread.class));
  }

  @Test
  @DisplayName("로그 연결 스레드 생성 성공")
  void createLinkedThread_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("로그 연결 스레드 내용")
        .isPublic(true)
        .logId(1L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findById(1L)).willReturn(Optional.of(testLog));
    given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
      Thread savedThread = invocation.getArgument(0);
      return Thread.builder()
          .id(1L)
          .user(savedThread.getUser())
          .log(savedThread.getLog())
          .content(savedThread.getContent())
          .isPublic(savedThread.getIsPublic())
          .viewCount(savedThread.getViewCount())
          .build();
    });

    // when
    ThreadResponse response = threadService.createThread(request, "test@example.com");

    // then
    assertThat(response.getContent()).isEqualTo("로그 연결 스레드 내용");
    assertThat(response.getIsLinkedToLog()).isTrue();
    verify(threadRepository).save(any(Thread.class));
  }

  @Test
  @DisplayName("존재하지 않는 사용자로 스레드 생성 시 예외 발생")
  void createThread_UserNotFound_ThrowsException() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("새 스레드 내용")
        .build();

    given(userRepository.findByEmail("nonexistent@example.com")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> threadService.createThread(request, "nonexistent@example.com"))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("사용자를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("존재하지 않는 로그에 연결하려 할 때 예외 발생")
  void createThread_LogNotFound_ThrowsException() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("로그 연결 스레드 내용")
        .logId(999L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findById(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> threadService.createThread(request, "test@example.com"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("연결할 로그를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("공개 로그에 다른 사용자가 스레드 생성 성공")
  void createThread_PublicLogByOtherUser_Success() {
    // given
    Log publicLog = Log.builder()
        .id(2L)
        .user(otherUser)
        .content("다른 사용자의 공개 로그")
        .isPublic(true)
        .build();

    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("로그 연결 스레드 내용")
        .logId(2L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findById(2L)).willReturn(Optional.of(publicLog));
    given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
      Thread savedThread = invocation.getArgument(0);
      return Thread.builder()
          .id(2L)
          .user(savedThread.getUser())
          .log(savedThread.getLog())
          .content(savedThread.getContent())
          .isPublic(savedThread.getIsPublic())
          .viewCount(savedThread.getViewCount())
          .build();
    });

    // when
    ThreadResponse response = threadService.createThread(request, "test@example.com");

    // then
    assertThat(response.getContent()).isEqualTo("로그 연결 스레드 내용");
    assertThat(response.getIsLinkedToLog()).isTrue();
    verify(threadRepository).save(any(Thread.class));
  }

  @Test
  @DisplayName("비공개 로그에 다른 사용자가 스레드 생성하려 할 때 예외 발생")
  void createThread_PrivateLogUnauthorizedAccess_ThrowsException() {
    // given
    Log privateLog = Log.builder()
        .id(2L)
        .user(otherUser)
        .content("다른 사용자의 비공개 로그")
        .isPublic(false)
        .build();

    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("로그 연결 스레드 내용")
        .logId(2L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(logRepository.findById(2L)).willReturn(Optional.of(privateLog));

    // when & then
    assertThatThrownBy(() -> threadService.createThread(request, "test@example.com"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("비공개 로그에는 작성자만 스레드를 생성할 수 있습니다");
  }

  @Test
  @DisplayName("공개 스레드 조회 성공")
  void getThread_PublicThread_Success() {
    // given
    given(threadRepository.findByIdAndIsPublicTrue(1L)).willReturn(Optional.of(testThread));

    // when
    ThreadResponse response = threadService.getThread(1L, null);

    // then
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getContent()).isEqualTo("테스트 스레드 내용");
    verify(threadRepository).findByIdAndIsPublicTrue(1L);
  }

  @Test
  @DisplayName("로그인 사용자의 스레드 조회 성공")
  void getThread_WithUser_Success() {
    // given
    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findViewableThreadByIdAndUserId(1L, 1L))
        .willReturn(Optional.of(testThread));

    // when
    ThreadResponse response = threadService.getThread(1L, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getContent()).isEqualTo("테스트 스레드 내용");
    verify(threadRepository).findViewableThreadByIdAndUserId(1L, 1L);
  }

  @Test
  @DisplayName("존재하지 않는 스레드 조회 시 예외 발생")
  void getThread_NotFound_ThrowsException() {
    // given
    given(threadRepository.findByIdAndIsPublicTrue(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> threadService.getThread(999L, null))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("스레드를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("스레드 목록 검색 성공")
  void searchThreads_Success() {
    // given
    ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
        .page(0)
        .size(10)
        .threadType(ThreadType.ALL)
        .build();

    Page<Thread> threadPage = new PageImpl<>(List.of(testThread));
    given(threadRepository.findByIsPublicTrueOrderByCreatedAtDesc(any(Pageable.class)))
        .willReturn(threadPage);

    // when
    Page<ThreadResponse> response = threadService.searchThreads(searchRequest, null);

    // then
    assertThat(response.getContent()).hasSize(1);
    assertThat(response.getContent().get(0).getContent()).isEqualTo("테스트 스레드 내용");
  }

  @Test
  @DisplayName("키워드로 스레드 검색 성공")
  void searchThreads_WithKeyword_Success() {
    // given
    ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
        .page(0)
        .size(10)
        .keyword("검색")
        .threadType(ThreadType.ALL)
        .build();

    Thread searchThread = Thread.builder()
        .id(2L)
        .user(testUser)
        .content("검색용 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .build();

    Page<Thread> threadPage = new PageImpl<>(List.of(searchThread));
    given(threadRepository.searchPublicThreadsByContent(anyString(), any(PageRequest.class)))
        .willReturn(threadPage);

    // when
    Page<ThreadResponse> response = threadService.searchThreads(searchRequest, null);

    // then
    assertThat(response.getContent()).hasSize(1);
    assertThat(response.getContent().get(0).getContent()).isEqualTo("검색용 스레드 내용");

    ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
    verify(threadRepository).searchPublicThreadsByContent(keywordCaptor.capture(),
        pageRequestCaptor.capture());

    assertThat(keywordCaptor.getValue()).isEqualTo("검색");
    assertThat(pageRequestCaptor.getValue().getPageNumber()).isEqualTo(0);
    assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(10);
  }

  @Test
  @DisplayName("스레드 수정 성공")
  void updateThread_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("수정된 내용")
        .isPublic(false)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testThread));

    // when
    ThreadResponse response = threadService.updateThread(1L, request, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getContent()).isEqualTo("수정된 내용");
    assertThat(response.getIsPublic()).isFalse();
    verify(threadRepository).findByIdAndUserId(1L, 1L);
  }

  @Test
  @DisplayName("권한 없는 스레드 수정 시 예외 발생")
  void updateThread_Unauthorized_ThrowsException() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("수정된 내용")
        .build();

    given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
    given(threadRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> threadService.updateThread(1L, request, "other@example.com"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("수정 권한이 없거나 스레드를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("스레드 삭제 성공")
  void deleteThread_Success() {
    // given
    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testThread));

    // when
    threadService.deleteThread(1L, "test@example.com");

    // then
    verify(threadRepository).delete(testThread);
  }

  @Test
  @DisplayName("권한 없는 스레드 삭제 시 예외 발생")
  void deleteThread_Unauthorized_ThrowsException() {
    // given
    given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
    given(threadRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> threadService.deleteThread(1L, "other@example.com"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("수정 권한이 없거나 스레드를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("로그별 스레드 목록 조회 성공")
  void getThreadsByLog_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Thread> threadPage = new PageImpl<>(List.of(testThread));

    given(logRepository.existsById(1L)).willReturn(true);
    given(threadRepository.findPublicThreadsByLogIdOrderByCreatedAtDesc(1L, pageable))
        .willReturn(threadPage);

    // when
    Page<ThreadResponse> response = threadService.getThreadsByLog(1L, null, pageable);

    // then
    assertThat(response.getContent()).hasSize(1);
    assertThat(response.getContent().get(0).getContent()).isEqualTo("테스트 스레드 내용");
  }

  @Test
  @DisplayName("존재하지 않는 로그의 스레드 목록 조회 시 예외 발생")
  void getThreadsByLog_LogNotFound_ThrowsException() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    given(logRepository.existsById(999L)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> threadService.getThreadsByLog(999L, null, pageable))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("로그를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("조회수 증가 동시성 테스트 - 원자적 연산 확인")
  void incrementViewCount_ConcurrencyTest() {
    // given
    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findViewableThreadByIdAndUserId(1L, 1L)).willReturn(
        Optional.of(testThread));
    given(threadRepository.incrementViewCount(1L)).willReturn(1);

    // when
    ThreadResponse response = threadService.getThread(1L, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);

    // 원자적 조회수 증가 메서드가 호출되는지 확인
    verify(threadRepository).incrementViewCount(1L);

    // 엔티티의 incrementViewCount 메서드는 호출되지 않아야 함
    verify(threadRepository, times(0)).save(any(Thread.class));
  }

  @Test
  @DisplayName("조회수 증가 실패 처리 테스트")
  void incrementViewCount_FailureHandling() {
    // given
    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findViewableThreadByIdAndUserId(1L, 1L)).willReturn(
        Optional.of(testThread));
    given(threadRepository.incrementViewCount(1L)).willReturn(0); // 업데이트 실패 시뮬레이션

    // when
    ThreadResponse response = threadService.getThread(1L, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);

    // 조회수 증가가 실패해도 스레드 조회는 성공해야 함
    verify(threadRepository).incrementViewCount(1L);
  }

  @Test
  @DisplayName("스레드 생성 시 content trim 처리 테스트")
  void createThread_ContentTrimming() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("  앞뒤 공백이 있는 내용  ")
        .isPublic(true)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
      Thread savedThread = invocation.getArgument(0);
      return Thread.builder()
          .id(1L)
          .user(savedThread.getUser())
          .log(savedThread.getLog())
          .content(savedThread.getContent())
          .isPublic(savedThread.getIsPublic())
          .viewCount(savedThread.getViewCount())
          .build();
    });

    // when
    ThreadResponse response = threadService.createThread(request, "test@example.com");

    // then
    assertThat(response.getContent()).isEqualTo("앞뒤 공백이 있는 내용");

    // Thread.builder에 전달되는 content가 trim되었는지 확인
    ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
    verify(threadRepository).save(threadCaptor.capture());
    Thread capturedThread = threadCaptor.getValue();
    assertThat(capturedThread.getContent()).isEqualTo("앞뒤 공백이 있는 내용");
  }

  @Test
  @DisplayName("스레드 수정 시 content trim 처리 테스트")
  void updateThread_ContentTrimming() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("  수정된 내용  ")
        .isPublic(false)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testThread));

    // when
    ThreadResponse response = threadService.updateThread(1L, request, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);

    // 실제 엔티티의 content가 trim되었는지 확인
    assertThat(testThread.getContent()).isEqualTo("수정된 내용");

    verify(threadRepository).findByIdAndUserId(1L, 1L);
  }

  @Test
  @DisplayName("스레드 로그 연결 해제 테스트")
  void updateThread_DisconnectLog_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .disconnectLog(true)
        .build();

    // 로그가 연결된 스레드 생성
    Thread linkedThread = Thread.builder()
        .id(1L)
        .user(testUser)
        .log(testLog)
        .content("로그 연결된 스레드")
        .isPublic(true)
        .viewCount(0L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(linkedThread));

    // when
    ThreadResponse response = threadService.updateThread(1L, request, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);

    // 로그 연결이 해제되었는지 확인
    assertThat(linkedThread.getLog()).isNull();
    assertThat(linkedThread.isIndependent()).isTrue();
    assertThat(linkedThread.isLinkedToLog()).isFalse();

    verify(threadRepository).findByIdAndUserId(1L, 1L);
  }

  @Test
  @DisplayName("스레드 로그 연결 변경 테스트")
  void updateThread_ChangeLog_Success() {
    // given
    Log newLog = Log.builder()
        .id(2L)
        .user(testUser)
        .content("새로운 로그")
        .isPublic(true)
        .build();

    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .logId(2L)
        .build();

    given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
    given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testThread));
    given(logRepository.findById(2L)).willReturn(Optional.of(newLog));

    // when
    ThreadResponse response = threadService.updateThread(1L, request, "test@example.com");

    // then
    assertThat(response.getId()).isEqualTo(1L);

    // 새로운 로그로 연결되었는지 확인
    assertThat(testThread.getLog()).isEqualTo(newLog);
    assertThat(testThread.isLinkedToLog()).isTrue();
    assertThat(testThread.isIndependent()).isFalse();

    verify(threadRepository).findByIdAndUserId(1L, 1L);
    verify(logRepository).findById(2L);
  }

  @Nested
  @DisplayName("XSS 공격 방어 (Security)")
  class XssProtectionTest {

    @Test
    @DisplayName("스레드 생성 시 악성 스크립트 태그를 제거한다")
    void createThread_WithScriptTag_ShouldRemoveScript() {
      // Given
      String maliciousContent = "안녕하세요 <script>alert('XSS');</script> 좋은 스레드입니다!";
      String expectedSanitizedContent = "안녕하세요  좋은 스레드입니다!";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(maliciousContent)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .log(savedThread.getLog())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(savedThread.getViewCount())
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedSanitizedContent);
      assertThat(response.getContent()).doesNotContain("<script>");
      assertThat(response.getContent()).doesNotContain("alert");

      ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
      verify(threadRepository).save(threadCaptor.capture());
      Thread capturedThread = threadCaptor.getValue();
      assertThat(capturedThread.getContent()).isEqualTo(expectedSanitizedContent);
    }

    @Test
    @DisplayName("스레드 생성 시 악성 이미지 태그를 제거한다")
    void createThread_WithMaliciousImgTag_ShouldRemoveImgTag() {
      // Given
      String maliciousContent = "스레드입니다 <img src=x onerror=alert('XSS')> 계속 텍스트";
      String expectedSanitizedContent = "스레드입니다  계속 텍스트";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(maliciousContent)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(0L)
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedSanitizedContent);
      assertThat(response.getContent()).doesNotContain("<img");
      assertThat(response.getContent()).doesNotContain("onerror");
    }

    @Test
    @DisplayName("스레드 생성 시 iframe 태그를 제거한다")
    void createThread_WithIframeTag_ShouldRemoveIframe() {
      // Given
      String maliciousContent = "내용 <iframe src='javascript:alert(1)'></iframe> 더 내용";
      String expectedSanitizedContent = "내용  더 내용";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(maliciousContent)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(0L)
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedSanitizedContent);
      assertThat(response.getContent()).doesNotContain("<iframe");
      assertThat(response.getContent()).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("스레드 생성 시 기본 포맷팅 태그는 허용한다")
    void createThread_WithFormattingTags_ShouldKeepFormatting() {
      // Given
      String formattedContent = "이것은 <b>굵은 글씨</b>이고 <i>기울임</i>입니다. <br>줄바꿈도 됩니다.";
      String expectedContent = "이것은 <b>굵은 글씨</b>이고 <i>기울임</i>입니다. <br />줄바꿈도 됩니다.";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(formattedContent)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(0L)
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedContent);
      assertThat(response.getContent()).contains("<b>굵은 글씨</b>");
      assertThat(response.getContent()).contains("<i>기울임</i>");
    }

    @Test
    @DisplayName("스레드 생성 시 안전한 링크는 허용한다")
    void createThread_WithSafeLink_ShouldKeepLink() {
      // Given
      String linkContent = "참고 링크: <a href=\"https://example.com\">example.com</a>";
      String expectedContent = "참고 링크: <a href=\"https://example.com\" rel=\"nofollow\">example.com</a>";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(linkContent)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(0L)
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedContent);
      assertThat(response.getContent()).contains("href=\"https://example.com\"");
      assertThat(response.getContent()).contains("rel=\"nofollow\"");
    }

    @Test
    @DisplayName("스레드 생성 시 javascript: 프로토콜 링크를 제거한다")
    void createThread_WithJavascriptLink_ShouldRemoveLink() {
      // Given
      String maliciousLink = "클릭: <a href=\"javascript:alert('XSS')\">여기</a>";
      String expectedSanitizedContent = "클릭: 여기";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(maliciousLink)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(0L)
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedSanitizedContent);
      assertThat(response.getContent()).doesNotContain("javascript:");
      assertThat(response.getContent()).doesNotContain("alert");
    }

    @Test
    @DisplayName("스레드 수정 시 악성 스크립트 태그를 제거한다")
    void updateThread_WithScriptTag_ShouldRemoveScript() {
      // Given
      String maliciousContent = "수정된 내용 <script>document.cookie</script> 입니다";
      String expectedSanitizedContent = "수정된 내용  입니다";
      
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content(maliciousContent)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testThread));

      // When
      ThreadResponse response = threadService.updateThread(1L, request, "test@example.com");

      // Then
      assertThat(testThread.getContent()).isEqualTo(expectedSanitizedContent);
      assertThat(testThread.getContent()).doesNotContain("<script>");
      assertThat(testThread.getContent()).doesNotContain("document.cookie");
    }

    @Test
    @DisplayName("스레드 생성 시 앞뒤 공백 제거 후 sanitization 적용")
    void createThread_WithWhitespaceAndHtml_ShouldTrimAndSanitize() {
      // Given
      String contentWithWhitespace = "   <b>안전한 내용</b> <script>alert('악성')</script>   ";
      // HTML Sanitizer는 스크립트 제거 후 뒤쪽 공백 하나를 남길 수 있음
      String expectedContent = "<b>안전한 내용</b> ";
      
      ThreadCreateRequest request = ThreadCreateRequest.builder()
          .content(contentWithWhitespace)
          .isPublic(true)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.save(any(Thread.class))).willAnswer(invocation -> {
        Thread savedThread = invocation.getArgument(0);
        return Thread.builder()
            .id(1L)
            .user(savedThread.getUser())
            .content(savedThread.getContent())
            .isPublic(savedThread.getIsPublic())
            .viewCount(0L)
            .build();
      });

      // When
      ThreadResponse response = threadService.createThread(request, "test@example.com");

      // Then
      assertThat(response.getContent()).isEqualTo(expectedContent);
      assertThat(response.getContent()).doesNotContain("<script>");
      assertThat(response.getContent()).contains("<b>안전한 내용</b>");
      assertThat(response.getContent()).doesNotStartWith(" ");
    }

    @Test
    @DisplayName("스레드 수정 시 trim 없이 sanitization만 적용")
    void updateThread_WithWhitespaceAndHtml_ShouldSanitizeWithoutTrim() {
      // Given
      String contentWithWhitespace = "   <b>안전한 내용</b> <script>alert('악성')</script>   ";
      
      ThreadUpdateRequest request = ThreadUpdateRequest.builder()
          .content(contentWithWhitespace)
          .build();

      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(testThread));

      // When
      threadService.updateThread(1L, request, "test@example.com");

      // Then
      // HTML Sanitizer는 스크립트를 제거하고 공백을 정리함 (trim 없이도)
      assertThat(testThread.getContent()).doesNotContain("<script>");
      assertThat(testThread.getContent()).contains("<b>안전한 내용</b>");
      // HTML Sanitizer 자체가 앞뒤 공백을 정리하므로 updateThread에서도 공백이 제거됨
      assertThat(testThread.getContent()).doesNotStartWith("   ");
      assertThat(testThread.getContent()).doesNotEndWith("   ");
    }
  }
}