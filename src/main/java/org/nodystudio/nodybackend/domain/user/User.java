package org.nodystudio.nodybackend.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.exception.custom.AccountAlreadyActivatedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "social_id"})
// 이메일 unique constraint는 DB 레벨에서 부분 인덱스로 처리
// CREATE UNIQUE INDEX idx_users_email_active ON users (email) WHERE is_active =
// true AND deleted_at IS NULL;
})
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 50)
  private OAuthProvider provider;

  @Column(name = "social_id", nullable = false)
  private String socialId;

  @Column(name = "email", length = 255)
  @Email(message = "유효한 이메일 형식이 아닙니다.")
  private String email;

  @Column(name = "nickname", nullable = false, length = 50)
  private String nickname;

  @Column(name = "is_active", nullable = false)
  @ColumnDefault("true")
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "refresh_token", length = 500)
  private String refreshToken;

  @Column(name = "refresh_token_expiry")
  private LocalDateTime refreshTokenExpiry;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  @Builder.Default
  private RoleType role = RoleType.USER;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateRefreshToken(String refreshToken, LocalDateTime refreshTokenExpiry) {
    this.refreshToken = refreshToken;
    this.refreshTokenExpiry = refreshTokenExpiry;
  }

  public User updateOAuthInfo(String nickname, String email) {
    if (nickname != null && !nickname.isBlank()) {
      this.nickname = nickname;
    }
    return this;
  }

  public void clearRefreshToken() {
    this.refreshToken = null;
    this.refreshTokenExpiry = null;
  }

  public void updateNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      throw new IllegalArgumentException("닉네임은 공백일 수 없습니다.");
    }
    this.nickname = nickname;
  }

  /**
   * 계정을 비활성화합니다 (회원탈퇴) - isActive를 false로 설정 - deletedAt을 현재 시간으로 설정 - 리프레시 토큰 무효화
   */
  public void deactivateAccount() {
    this.isActive = false;
    this.deletedAt = LocalDateTime.now();
    this.clearRefreshToken();
  }

  /**
   * 탈퇴한 계정을 재활성화합니다. (30일 유예기간 내 완전 복구) 기존 사용자 정보를 그대로 유지하며 계정만 활성화합니다.
   *
   * @throws AccountAlreadyActivatedException 이미 활성 상태인 계정인 경우
   */
  public void reactivateAccount() {
    if (this.isActive) {
      throw new AccountAlreadyActivatedException("이미 활성 상태인 계정입니다.");
    }

    this.isActive = true;
    this.deletedAt = null;

    // 재활성화 시 기존 refresh token 제거
    this.refreshToken = null;
    this.refreshTokenExpiry = null;
  }

  /**
   * 사용자의 역할을 Spring Security {@link GrantedAuthority} 목록으로 반환합니다. 현재 시스템에서는 사용자가 단일 역할만 가지지만,
   * Spring Security 호환성을 위해 목록 형태로 반환합니다.
   *
   * @return 사용자의 권한 목록 (항상 단일 요소)
   */
  public List<GrantedAuthority> getRoles() {
    return Collections.singletonList(new SimpleGrantedAuthority(this.role.getKey()));
  }
}
