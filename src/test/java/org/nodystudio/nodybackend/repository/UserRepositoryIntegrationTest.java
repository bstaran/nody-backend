package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  private User testUser1;
  private User testUser2;

  @BeforeEach
  void setUp() {
    testUser1 = User.builder()
        .provider(OAuthProvider.GOOGLE)
        .socialId("123456789")
        .email("user1@example.com")
        .nickname("사용자1")
        .role(RoleType.USER)
        .isActive(true)
        .build();

    testUser2 = User.builder()
        .provider(OAuthProvider.GOOGLE)
        .socialId("987654321")
        .email("user2@example.com")
        .nickname("사용자2")
        .role(RoleType.USER)
        .isActive(true)
        .build();

    entityManager.persistAndFlush(testUser1);
    entityManager.persistAndFlush(testUser2);
  }

  @Test
  @DisplayName("닉네임 중복 검증 - 다른 사용자 제외 후 중복 확인")
  void existsByNicknameAndIdNotAndIsActiveTrue_shouldReturnCorrectResult() {
    // given
    String existingNickname = "사용자2";
    String newNickname = "새로운사용자";

    // when - 다른 사용자의 닉네임을 사용하려고 할 때
    boolean isDuplicate = userRepository.existsByNicknameAndIdNotAndIsActiveTrue(
        existingNickname, testUser1.getId());

    // then
    assertThat(isDuplicate).isTrue();

    // when - 존재하지 않는 닉네임을 사용하려고 할 때
    boolean isNotDuplicate = userRepository.existsByNicknameAndIdNotAndIsActiveTrue(
        newNickname, testUser1.getId());

    // then
    assertThat(isNotDuplicate).isFalse();

    // when - 본인의 닉네임을 그대로 사용하려고 할 때
    boolean isSameNickname = userRepository.existsByNicknameAndIdNotAndIsActiveTrue(
        testUser1.getNickname(), testUser1.getId());

    // then
    assertThat(isSameNickname).isFalse();
  }

  @Test
  @DisplayName("활성 사용자만 닉네임 존재 확인")
  void existsByNicknameAndIsActiveTrue_shouldOnlyCheckActiveUsers() {
    // given
    String nickname = "사용자1";

    // when - 활성 사용자의 닉네임 확인
    boolean exists = userRepository.existsByNicknameAndIsActiveTrue(nickname);

    // then
    assertThat(exists).isTrue();

    // given - 사용자를 비활성화
    testUser1.deactivateAccount();
    entityManager.persistAndFlush(testUser1);

    // when - 비활성화된 사용자의 닉네임 확인
    boolean existsAfterDeactivation = userRepository.existsByNicknameAndIsActiveTrue(nickname);

    // then
    assertThat(existsAfterDeactivation).isFalse();
  }

  @Test
  @DisplayName("활성 사용자만 닉네임으로 조회")
  void findByNicknameAndIsActiveTrue_shouldOnlyReturnActiveUsers() {
    // given
    String nickname = "사용자1";

    // when - 활성 사용자 조회
    var activeUser = userRepository.findByNicknameAndIsActiveTrue(nickname);

    // then
    assertThat(activeUser).isPresent();
    assertThat(activeUser.get().getId()).isEqualTo(testUser1.getId());

    // given - 사용자를 비활성화
    testUser1.deactivateAccount();
    entityManager.persistAndFlush(testUser1);

    // when - 비활성화된 사용자 조회
    var deactivatedUser = userRepository.findByNicknameAndIsActiveTrue(nickname);

    // then
    assertThat(deactivatedUser).isEmpty();
  }
}