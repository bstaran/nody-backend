package org.nodystudio.nodybackend.security.userdetails;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.security.core.GrantedAuthority;

/**
 * CustomUserDetails 클래스의 단위 테스트
 */
@DisplayName("CustomUserDetails 단위 테스트")
class CustomUserDetailsTest {

  private User user;
  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .id(1L)
        .provider(OAuthProvider.GOOGLE)
        .socialId("google123")
        .email("test@example.com")
        .nickname("테스트유저")
        .isActive(true)
        .role(RoleType.USER)
        .build();
    
    userDetails = new CustomUserDetails(user);
  }

  @Nested
  @DisplayName("UserDetails 인터페이스 구현 테스트")
  class UserDetailsImplementationTest {

    @Test
    @DisplayName("사용자 권한 목록을 올바르게 반환한다")
    void getAuthorities_shouldReturnUserAuthorities() {
      // when
      Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

      // then
      assertThat(authorities)
          .hasSize(1)
          .extracting(GrantedAuthority::getAuthority)
          .contains("ROLE_USER");
    }

    @Test
    @DisplayName("OAuth2 인증 사용으로 패스워드는 null을 반환한다")
    void getPassword_shouldReturnNull() {
      // when
      String password = userDetails.getPassword();

      // then
      assertThat(password).isNull();
    }

    @Test
    @DisplayName("사용자 ID를 username으로 반환한다")
    void getUsername_shouldReturnUserId() {
      // when
      String username = userDetails.getUsername();

      // then
      assertThat(username).isEqualTo("1");
    }

    @Test
    @DisplayName("계정 만료 여부는 항상 true를 반환한다")
    void isAccountNonExpired_shouldReturnTrue() {
      // when
      boolean nonExpired = userDetails.isAccountNonExpired();

      // then
      assertThat(nonExpired).isTrue();
    }

    @Test
    @DisplayName("자격 증명 만료 여부는 항상 true를 반환한다")
    void isCredentialsNonExpired_shouldReturnTrue() {
      // when
      boolean credentialsNonExpired = userDetails.isCredentialsNonExpired();

      // then
      assertThat(credentialsNonExpired).isTrue();
    }
  }

  @Nested
  @DisplayName("계정 상태 테스트")
  class AccountStatusTest {

    @Test
    @DisplayName("활성 사용자의 경우 계정 잠금 여부는 true를 반환한다")
    void isAccountNonLocked_shouldReturnTrue_whenUserIsActive() {
      // when
      boolean nonLocked = userDetails.isAccountNonLocked();

      // then
      assertThat(nonLocked).isTrue();
    }

    @Test
    @DisplayName("비활성 사용자의 경우 계정 잠금 여부는 false를 반환한다")
    void isAccountNonLocked_shouldReturnFalse_whenUserIsInactive() {
      // given
      User inactiveUser = User.builder()
          .id(2L)
          .provider(OAuthProvider.GOOGLE)
          .socialId("google456")
          .email("inactive@example.com")
          .nickname("비활성유저")
          .isActive(false)
          .role(RoleType.USER)
          .build();
      CustomUserDetails inactiveUserDetails = new CustomUserDetails(inactiveUser);

      // when
      boolean nonLocked = inactiveUserDetails.isAccountNonLocked();

      // then
      assertThat(nonLocked).isFalse();
    }

    @Test
    @DisplayName("활성 사용자의 경우 계정 활성화 여부는 true를 반환한다")
    void isEnabled_shouldReturnTrue_whenUserIsActive() {
      // when
      boolean enabled = userDetails.isEnabled();

      // then
      assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("비활성 사용자의 경우 계정 활성화 여부는 false를 반환한다")
    void isEnabled_shouldReturnFalse_whenUserIsInactive() {
      // given
      User inactiveUser = User.builder()
          .id(2L)
          .provider(OAuthProvider.GOOGLE)
          .socialId("google456")
          .email("inactive@example.com")
          .nickname("비활성유저")
          .isActive(false)
          .role(RoleType.USER)
          .build();
      CustomUserDetails inactiveUserDetails = new CustomUserDetails(inactiveUser);

      // when
      boolean enabled = inactiveUserDetails.isEnabled();

      // then
      assertThat(enabled).isFalse();
    }
  }

  @Nested
  @DisplayName("User 엔티티 접근 테스트")
  class UserEntityAccessTest {

    @Test
    @DisplayName("래핑된 User 엔티티를 올바르게 반환한다")
    void getUser_shouldReturnWrappedUser() {
      // when
      User returnedUser = userDetails.getUser();

      // then
      assertThat(returnedUser)
          .isEqualTo(user)
          .extracting(User::getId, User::getEmail, User::getNickname)
          .containsExactly(1L, "test@example.com", "테스트유저");
    }
  }

  @Nested
  @DisplayName("관리자 권한 테스트")
  class AdminRoleTest {

    @Test
    @DisplayName("관리자 사용자의 경우 ROLE_ADMIN 권한을 반환한다")
    void getAuthorities_shouldReturnAdminRole_whenUserIsAdmin() {
      // given
      User adminUser = User.builder()
          .id(3L)
          .provider(OAuthProvider.GOOGLE)
          .socialId("google789")
          .email("admin@example.com")
          .nickname("관리자")
          .isActive(true)
          .role(RoleType.ADMIN)
          .build();
      CustomUserDetails adminUserDetails = new CustomUserDetails(adminUser);

      // when
      Collection<? extends GrantedAuthority> authorities = adminUserDetails.getAuthorities();

      // then
      assertThat(authorities)
          .isNotNull()
          .hasSize(1)
          .extracting(GrantedAuthority::getAuthority)
          .contains("ROLE_ADMIN");
    }
  }
}