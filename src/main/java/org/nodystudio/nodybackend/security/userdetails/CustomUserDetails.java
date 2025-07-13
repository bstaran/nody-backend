package org.nodystudio.nodybackend.security.userdetails;

import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security의 인증 주체로 사용되는 커스텀 UserDetails 구현체입니다.
 *
 * <p>
 * 이 클래스는 {@link User} 엔티티를 래핑하여 Spring Security가 요구하는
 * {@link UserDetails} 인터페이스를 구현합니다. JWT 기반 인증 시스템에서
 * 인증된 사용자 정보를 보안 컨텍스트에 저장하는 데 사용됩니다.
 * </p>
 *
 * @see UserDetails
 * @see User
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  private static final long serialVersionUID = 1L;

  /**
   * 래핑된 User 엔티티
   */
  private final User user;

  /**
   * 사용자의 권한 목록을 반환합니다.
   *
   * @return 사용자가 가진 권한 목록
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return user.getRoles();
  }

  public String getEmail() { return user.getEmail(); }

  public String getNickname() { return user.getNickname(); }

  public OAuthProvider getProvider() { return user.getProvider(); }

  /**
   * 사용자의 비밀번호를 반환합니다.
   *
   * <p>
   * OAuth2 기반 인증 시스템이므로 비밀번호는 사용하지 않습니다.
   * </p>
   *
   * @return null (OAuth2 인증 사용)
   */
  @Override
  public String getPassword() {
    return null;
  }

  /**
   * 사용자의 고유 식별자를 반환합니다.
   *
   * @return 사용자 ID 문자열
   */
  @Override
  public String getUsername() {
    return user.getId().toString();
  }

  /**
   * 계정 만료 여부를 반환합니다.
   *
   * @return true (계정 만료 기능 미사용)
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 계정 잠금 여부를 반환합니다.
   *
   * @return 사용자 활성화 상태
   */
  @Override
  public boolean isAccountNonLocked() {
    return user.getIsActive();
  }

  /**
   * 자격 증명 만료 여부를 반환합니다.
   *
   * @return true (자격 증명 만료 기능 미사용)
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 계정 활성화 여부를 반환합니다.
   *
   * @return 사용자 활성화 상태
   */
  @Override
  public boolean isEnabled() {
    return user.getIsActive();
  }
}