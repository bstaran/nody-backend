package org.nodystudio.nodybackend.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("사용자 계정 재활성화 테스트")
class UserReactivationTest {

  private User deactivatedUser;

  @BeforeEach
  void setUp() {
    deactivatedUser = User.builder()
        .id(1L)
        .provider("google")
        .socialId("123456789")
        .email("test@example.com")
        .nickname("기존닉네임")
        .role(RoleType.USER)
        .isActive(true)
        .build();

    // 계정 탈퇴 처리
    deactivatedUser.deactivateAccount();
  }

  @Test
  @DisplayName("탈퇴한 계정 재활성화 - 완전 복구 (30일 유예기간)")
  void reactivateAccount_success() {
    // given
    String originalNickname = deactivatedUser.getNickname();
    String originalEmail = deactivatedUser.getEmail();

    // 탈퇴 상태 확인
    assertThat(deactivatedUser.getIsActive()).isFalse();
    assertThat(deactivatedUser.getDeletedAt()).isNotNull();

    // when
    deactivatedUser.reactivateAccount();

    // then
    assertThat(deactivatedUser.getIsActive()).isTrue();
    assertThat(deactivatedUser.getDeletedAt()).isNull();
    assertThat(deactivatedUser.getNickname()).isEqualTo(originalNickname);
    assertThat(deactivatedUser.getEmail()).isEqualTo(originalEmail);
    assertThat(deactivatedUser.getRefreshToken()).isNull();
    assertThat(deactivatedUser.getRefreshTokenExpiry()).isNull();
  }

  @Test
  @DisplayName("이미 활성 상태인 계정 재활성화 시도 - 예외 발생")
  void reactivateAccount_alreadyActive_throwsException() {
    // given
    User activeUser = User.builder()
        .id(2L)
        .provider("google")
        .socialId("987654321")
        .email("active@example.com")
        .nickname("활성사용자")
        .role(RoleType.USER)
        .isActive(true)
        .build();

    // when & then
    assertThatThrownBy(() -> activeUser.reactivateAccount())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("이미 활성 상태인 계정입니다.");
  }

  @Test
  @DisplayName("재활성화 후 사용자 정보 완전 복구 확인")
  void reactivateAccount_shouldRestoreAllUserData() {
    // given
    String originalNickname = "원래닉네임";
    String originalEmail = "original@test.com";
    String originalProvider = "google";
    String originalSocialId = "123456789";
    RoleType originalRole = RoleType.USER;

    User user = User.builder()
        .id(3L)
        .provider(originalProvider)
        .socialId(originalSocialId)
        .email(originalEmail)
        .nickname(originalNickname)
        .role(originalRole)
        .isActive(true)
        .build();

    // 탈퇴 처리
    user.deactivateAccount();

    // when
    user.reactivateAccount();

    // then - 모든 원래 정보가 그대로 복구되어야 함
    assertThat(user.getIsActive()).isTrue();
    assertThat(user.getDeletedAt()).isNull();
    assertThat(user.getNickname()).isEqualTo(originalNickname);
    assertThat(user.getEmail()).isEqualTo(originalEmail);
    assertThat(user.getProvider()).isEqualTo(originalProvider);
    assertThat(user.getSocialId()).isEqualTo(originalSocialId);
    assertThat(user.getRole()).isEqualTo(originalRole);
  }

  @Test
  @DisplayName("재활성화 시 리프레시 토큰 초기화 확인")
  void reactivateAccount_shouldClearRefreshToken() {
    // given
    deactivatedUser.updateRefreshToken("existing-refresh-token", 
        java.time.LocalDateTime.now().plusDays(7));
    
    // 토큰이 설정되어 있는지 확인
    assertThat(deactivatedUser.getRefreshToken()).isNotNull();
    assertThat(deactivatedUser.getRefreshTokenExpiry()).isNotNull();

    // when
    deactivatedUser.reactivateAccount();

    // then - 리프레시 토큰이 초기화되어야 함
    assertThat(deactivatedUser.getRefreshToken()).isNull();
    assertThat(deactivatedUser.getRefreshTokenExpiry()).isNull();
  }

}