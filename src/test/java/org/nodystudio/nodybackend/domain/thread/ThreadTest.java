package org.nodystudio.nodybackend.domain.thread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.comment.Comment;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;

@DisplayName("Thread 도메인 엔티티 테스트")
class ThreadTest {

  private User testUser;
  private User otherUser;
  private Log testLog;
  private Log otherUserLog;
  private Thread thread;

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

    otherUserLog = Log.builder()
        .id(2L)
        .user(otherUser)
        .content("다른 사용자 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .build();

    thread = Thread.builder()
        .id(1L)
        .user(testUser)
        .log(testLog)
        .content("테스트 스레드 내용")
        .isPublic(true)
        .viewCount(0L)
        .build();
  }


  @Test
  @DisplayName("내용 업데이트 성공")
  void updateContent_Success() {
    // given
    String newContent = "새로운 내용";

    // when
    thread.updateContent(newContent);

    // then
    assertThat(thread.getContent()).isEqualTo(newContent);
  }

  @Test
  @DisplayName("내용을 null로 업데이트 시 예외 발생")
  void updateContent_NullContent_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> thread.updateContent(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("내용은 공백일 수 없습니다");
  }

  @Test
  @DisplayName("내용이 빈 문자열인 경우 예외 발생")
  void updateContent_EmptyContent_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> thread.updateContent(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("내용은 공백일 수 없습니다");
  }

  @Test
  @DisplayName("내용이 공백만 있는 경우 예외 발생")
  void updateContent_BlankContent_ThrowsException() {
    // when & then
    assertThatThrownBy(() -> thread.updateContent("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("내용은 공백일 수 없습니다");
  }

  @Test
  @DisplayName("공개 설정 업데이트 성공")
  void updatePublicSetting_Success() {
    // given
    assertThat(thread.getIsPublic()).isTrue();

    // when
    thread.updatePublicSetting(false);

    // then
    assertThat(thread.getIsPublic()).isFalse();
  }

  @Test
  @DisplayName("로그 연결 성공")
  void connectToLog_Success() {
    // given
    Log newLog = Log.builder()
        .id(3L)
        .user(testUser)
        .content("새 로그")
        .build();

    // when
    thread.connectToLog(newLog);

    // then
    assertThat(thread.getLog()).isEqualTo(newLog);
  }

  @Test
  @DisplayName("로그 연결 해제 성공")
  void disconnectFromLog_Success() {
    // when
    thread.disconnectFromLog();

    // then
    assertThat(thread.getLog()).isNull();
    assertThat(thread.isIndependent()).isTrue();
  }

  @Test
  @DisplayName("조회수 증가 성공")
  void incrementViewCount_Success() {
    // given
    long initialViewCount = thread.getViewCount();

    // when
    thread.incrementViewCount();

    // then
    assertThat(thread.getViewCount()).isEqualTo(initialViewCount + 1);
  }

  @Test
  @DisplayName("조회수 여러 번 증가")
  void incrementViewCount_Multiple_Success() {
    // given
    long initialViewCount = thread.getViewCount();

    // when
    thread.incrementViewCount();
    thread.incrementViewCount();
    thread.incrementViewCount();

    // then
    assertThat(thread.getViewCount()).isEqualTo(initialViewCount + 3);
  }

  @Test
  @DisplayName("스레드 소유자 확인 - 본인인 경우")
  void isOwnedBy_OwnThread_ReturnsTrue() {
    // when & then
    assertThat(thread.isOwnedBy(testUser)).isTrue();
  }

  @Test
  @DisplayName("스레드 소유자 확인 - 다른 사용자인 경우")
  void isOwnedBy_OtherUser_ReturnsFalse() {
    // when & then
    assertThat(thread.isOwnedBy(otherUser)).isFalse();
  }

  @Test
  @DisplayName("스레드 소유자 확인 - null 사용자인 경우")
  void isOwnedBy_NullUser_ReturnsFalse() {
    // when & then
    assertThat(thread.isOwnedBy(null)).isFalse();
  }

  @Test
  @DisplayName("스레드 소유자 확인 - 스레드 사용자가 null인 경우")
  void isOwnedBy_ThreadUserIsNull_ReturnsFalse() {
    // given
    Thread threadWithNullUser = Thread.builder()
        .id(2L)
        .user(null)
        .content("내용")
        .build();

    // when & then
    assertThat(threadWithNullUser.isOwnedBy(testUser)).isFalse();
  }

  @Test
  @DisplayName("공개 스레드 조회 권한 확인 - 익명 사용자")
  void isViewableBy_PublicThread_AnonymousUser_ReturnsTrue() {
    // given
    assertThat(thread.getIsPublic()).isTrue();

    // when & then
    assertThat(thread.isViewableBy(null)).isTrue();
  }

  @Test
  @DisplayName("공개 스레드 조회 권한 확인 - 다른 사용자")
  void isViewableBy_PublicThread_OtherUser_ReturnsTrue() {
    // given
    assertThat(thread.getIsPublic()).isTrue();

    // when & then
    assertThat(thread.isViewableBy(otherUser)).isTrue();
  }

  @Test
  @DisplayName("비공개 스레드 조회 권한 확인 - 작성자")
  void isViewableBy_PrivateThread_Owner_ReturnsTrue() {
    // given
    thread.updatePublicSetting(false);

    // when & then
    assertThat(thread.isViewableBy(testUser)).isTrue();
  }

  @Test
  @DisplayName("비공개 스레드 조회 권한 확인 - 다른 사용자")
  void isViewableBy_PrivateThread_OtherUser_ReturnsFalse() {
    // given
    thread.updatePublicSetting(false);

    // when & then
    assertThat(thread.isViewableBy(otherUser)).isFalse();
  }

  @Test
  @DisplayName("비공개 스레드 조회 권한 확인 - 익명 사용자")
  void isViewableBy_PrivateThread_AnonymousUser_ReturnsFalse() {
    // given
    thread.updatePublicSetting(false);

    // when & then
    assertThat(thread.isViewableBy(null)).isFalse();
  }

  @Test
  @DisplayName("로그 연결 여부 확인 - 연결된 경우")
  void isLinkedToLog_WithLog_ReturnsTrue() {
    // when & then
    assertThat(thread.isLinkedToLog()).isTrue();
  }

  @Test
  @DisplayName("로그 연결 여부 확인 - 연결되지 않은 경우")
  void isLinkedToLog_WithoutLog_ReturnsFalse() {
    // given
    thread.disconnectFromLog();

    // when & then
    assertThat(thread.isLinkedToLog()).isFalse();
  }

  @Test
  @DisplayName("독립 스레드 여부 확인 - 독립 스레드인 경우")
  void isIndependent_IndependentThread_ReturnsTrue() {
    // given
    thread.disconnectFromLog();

    // when & then
    assertThat(thread.isIndependent()).isTrue();
  }

  @Test
  @DisplayName("독립 스레드 여부 확인 - 로그 연결 스레드인 경우")
  void isIndependent_LinkedThread_ReturnsFalse() {
    // when & then
    assertThat(thread.isIndependent()).isFalse();
  }

  @Test
  @DisplayName("로그 소유자와 스레드 소유자 일치 확인 - 일치하는 경우")
  void isLogOwnerMatchesThreadOwner_MatchingOwners_ReturnsTrue() {
    // when & then
    assertThat(thread.isLogOwnerMatchesThreadOwner()).isTrue();
  }

  @Test
  @DisplayName("로그 소유자와 스레드 소유자 일치 확인 - 일치하지 않는 경우")
  void isLogOwnerMatchesThreadOwner_NonMatchingOwners_ReturnsFalse() {
    // given
    thread.connectToLog(otherUserLog);

    // when & then
    assertThat(thread.isLogOwnerMatchesThreadOwner()).isFalse();
  }

  @Test
  @DisplayName("로그 소유자와 스레드 소유자 일치 확인 - 독립 스레드인 경우")
  void isLogOwnerMatchesThreadOwner_IndependentThread_ReturnsTrue() {
    // given
    thread.disconnectFromLog();

    // when & then
    assertThat(thread.isLogOwnerMatchesThreadOwner()).isTrue();
  }

  @Test
  @DisplayName("빌더 패턴으로 독립 스레드 생성")
  void builder_IndependentThread_Success() {
    // when
    Thread independentThread = Thread.builder()
        .user(testUser)
        .content("독립 스레드 내용")
        .isPublic(true)
        .build();

    // then
    assertThat(independentThread.getUser()).isEqualTo(testUser);
    assertThat(independentThread.getContent()).isEqualTo("독립 스레드 내용");
    assertThat(independentThread.getIsPublic()).isTrue();
    assertThat(independentThread.getViewCount()).isZero();
    assertThat(independentThread.getLog()).isNull();
    assertThat(independentThread.isIndependent()).isTrue();
    assertThat(independentThread.isLinkedToLog()).isFalse();
  }

  @Test
  @DisplayName("빌더 패턴으로 로그 연결 스레드 생성")
  void builder_LinkedThread_Success() {
    // when
    Thread linkedThread = Thread.builder()
        .user(testUser)
        .log(testLog)
        .content("로그 연결 스레드 내용")
        .isPublic(false)
        .build();

    // then
    assertThat(linkedThread.getUser()).isEqualTo(testUser);
    assertThat(linkedThread.getLog()).isEqualTo(testLog);
    assertThat(linkedThread.getContent()).isEqualTo("로그 연결 스레드 내용");
    assertThat(linkedThread.getIsPublic()).isFalse();
    assertThat(linkedThread.getViewCount()).isZero();
    assertThat(linkedThread.isLinkedToLog()).isTrue();
    assertThat(linkedThread.isIndependent()).isFalse();
    assertThat(linkedThread.isLogOwnerMatchesThreadOwner()).isTrue();
  }

  @Test
  @DisplayName("기본값 확인")
  void builder_DefaultValues_Success() {
    // when
    Thread threadWithDefaults = Thread.builder()
        .user(testUser)
        .content("기본값 테스트")
        .build();

    // then
    assertThat(threadWithDefaults.getIsPublic()).isTrue();
    assertThat(threadWithDefaults.getViewCount()).isZero();
    assertThat(threadWithDefaults.getContent()).isEqualTo("기본값 테스트");
    assertThat(threadWithDefaults.getLog()).isNull();
  }

  @Test
  @DisplayName("댓글 추가 시 양방향 연관관계가 올바르게 설정된다")
  void addComment_SetsUpBidirectionalRelationship() {
    // given
    Comment comment = Comment.builder()
        .id(1L)
        .content("테스트 댓글")
        .author(testUser)
        .mentionedUsers(new HashSet<>())
        .build();

    // when
    thread.addComment(comment);

    // then
    assertThat(thread.getComments()).contains(comment);
    assertThat(comment.getThread()).isEqualTo(thread);
  }

  @Test
  @DisplayName("댓글 제거 시 양방향 연관관계가 올바르게 해제된다")
  void removeComment_ClearsBidirectionalRelationship() {
    // given
    Comment comment = Comment.builder()
        .id(1L)
        .content("테스트 댓글")
        .author(testUser)
        .thread(thread)
        .mentionedUsers(new HashSet<>())
        .build();
    thread.addComment(comment);

    // when
    thread.removeComment(comment);

    // then
    assertThat(thread.getComments()).doesNotContain(comment);
    assertThat(comment.getThread()).isNull();
  }

  @Test
  @DisplayName("이미 올바른 스레드에 속한 댓글을 추가할 때 중복 설정을 피한다")
  void addComment_AlreadyLinked_DoesNotDuplicateSetup() {
    // given
    Comment comment = Comment.builder()
        .id(1L)
        .content("테스트 댓글")
        .author(testUser)
        .thread(thread)
        .mentionedUsers(new HashSet<>())
        .build();

    // when
    thread.addComment(comment);

    // then
    assertThat(thread.getComments()).contains(comment);
    assertThat(comment.getThread()).isEqualTo(thread);
  }

  @Test
  @DisplayName("다른 스레드에 속한 댓글을 제거할 때는 스레드 설정을 변경하지 않는다")
  void removeComment_DifferentThread_DoesNotClearThread() {
    // given
    Thread otherThread = Thread.builder()
        .id(2L)
        .user(testUser)
        .content("다른 스레드")
        .build();

    Comment comment = Comment.builder()
        .id(1L)
        .content("테스트 댓글")
        .author(testUser)
        .thread(otherThread)
        .mentionedUsers(new HashSet<>())
        .build();

    // when
    thread.removeComment(comment);

    // then
    assertThat(thread.getComments()).doesNotContain(comment);
    assertThat(comment.getThread()).isEqualTo(otherThread); // 다른 스레드 유지
  }

  @Test
  @DisplayName("스레드 비활성화 - 성공")
  void deactivate_Success() {
    // given
    assertThat(thread.isActive()).isTrue();
    assertThat(thread.isDeactivated()).isFalse();

    // when
    thread.deactivate();

    // then
    assertThat(thread.isActive()).isFalse();
    assertThat(thread.isDeactivated()).isTrue();
    assertThat(thread.getDeactivatedAt()).isNotNull();
  }

  @Test
  @DisplayName("스레드 재활성화 - 성공")
  void reactivate_Success() {
    // given
    thread.deactivate();
    assertThat(thread.isDeactivated()).isTrue();

    // when
    thread.reactivate();

    // then
    assertThat(thread.isActive()).isTrue();
    assertThat(thread.isDeactivated()).isFalse();
    assertThat(thread.getDeactivatedAt()).isNull();
  }

  @Test
  @DisplayName("활성 스레드 상태 확인")
  void isActive_ActiveThread_ReturnsTrue() {
    // given & when & then
    assertThat(thread.isActive()).isTrue();
    assertThat(thread.isDeactivated()).isFalse();
  }

  @Test
  @DisplayName("비활성 스레드 상태 확인")
  void isDeactivated_DeactivatedThread_ReturnsTrue() {
    // given
    thread.deactivate();

    // when & then
    assertThat(thread.isDeactivated()).isTrue();
    assertThat(thread.isActive()).isFalse();
  }

  @Test
  @DisplayName("비활성화된 스레드의 원본 공개설정 보존 확인")
  void deactivate_PreservesOriginalPublicSetting() {
    // given
    thread.updatePublicSetting(false); // 비공개로 설정
    assertThat(thread.getIsPublic()).isFalse();

    // when
    thread.deactivate();

    // then
    assertThat(thread.getIsPublic()).isFalse(); // 원본 설정 보존
    assertThat(thread.isDeactivated()).isTrue();
  }

  @Test
  @DisplayName("재활성화된 스레드의 원본 공개설정 보존 확인")
  void reactivate_PreservesOriginalPublicSetting() {
    // given
    thread.updatePublicSetting(false); // 비공개로 설정
    thread.deactivate();
    
    // when
    thread.reactivate();

    // then
    assertThat(thread.getIsPublic()).isFalse(); // 원본 설정 보존
    assertThat(thread.isActive()).isTrue();
  }
}