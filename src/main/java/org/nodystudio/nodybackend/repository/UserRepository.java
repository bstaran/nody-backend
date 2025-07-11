package org.nodystudio.nodybackend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 소셜 로그인 제공자와 소셜 ID로 사용자를 찾습니다.
   *
   * @param provider 소셜 로그인 제공자 (e.g., OAuthProvider.GOOGLE)
   * @param socialId 소셜 ID
   * @return Optional<User>
   */
  Optional<User> findByProviderAndSocialId(OAuthProvider provider, String socialId);

  /**
   * Refresh Token으로 사용자를 찾습니다. Refresh Token은 고유하거나, 특정 사용자와 1:1 매핑된다고 가정합니다. (만약 한 사용자가 여러 기기에서
   * 로그인하여 여러 Refresh Token을 가질 수 있다면 로직 수정 필요)
   *
   * @param refreshToken Refresh Token
   * @return Optional<User>
   */
  Optional<User> findByRefreshToken(String refreshToken);

  /**
   * 이메일로 사용자를 찾습니다. (선택적: 필요시 사용)
   *
   * @param email 사용자 이메일
   * @return Optional<User>
   */
  Optional<User> findByEmail(String email);

  /**
   * 활성 사용자만 ID로 조회합니다.
   *
   * @param id 사용자 ID
   * @return Optional<User>
   */
  Optional<User> findByIdAndIsActiveTrue(Long id);

  /**
   * 활성 사용자만 이메일로 조회합니다.
   *
   * @param email 사용자 이메일
   * @return Optional<User>
   */
  Optional<User> findByEmailAndIsActiveTrue(String email);

  /**
   * 활성 사용자만 소셜 정보로 조회합니다.
   *
   * @param provider 소셜 로그인 제공자
   * @param socialId 소셜 ID
   * @return Optional<User>
   */
  Optional<User> findByProviderAndSocialIdAndIsActiveTrue(OAuthProvider provider, String socialId);

  /**
   * 특정 시점 이전에 탈퇴한 비활성 사용자들을 조회합니다. (배치 삭제용)
   *
   * @param cutoffTime 기준 시점
   * @return List<User>
   */
  List<User> findByIsActiveFalseAndDeletedAtBefore(LocalDateTime cutoffTime);

  /**
   * 특정 시점 이후에 탈퇴한 사용자를 이메일로 조회합니다. (재가입 검증용)
   *
   * @param email      이메일
   * @param cutoffTime 기준 시점
   * @return Optional<User>
   */
  Optional<User> findByEmailAndDeletedAtAfter(String email, LocalDateTime cutoffTime);

  /**
   * 특정 시점 이전에 탈퇴한 비활성 사용자 수를 조회합니다. (배치 모니터링용)
   *
   * @param cutoffTime 기준 시점
   * @return 해당 조건에 맞는 사용자 수
   */
  long countByIsActiveFalseAndDeletedAtBefore(LocalDateTime cutoffTime);
}