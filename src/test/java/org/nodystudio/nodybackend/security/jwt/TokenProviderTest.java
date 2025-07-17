package org.nodystudio.nodybackend.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.code.ErrorCode;
import org.nodystudio.nodybackend.exception.custom.InvalidTokenException;

class TokenProviderTest {

  private TokenProvider tokenProvider;
  private String testSecret;
  private SecretKey testSecretKey;
  private long accessTokenExpirationMinutes = 1;
  private long refreshTokenExpirationDays = 1;

  private User testUser;

  @BeforeEach
  void setUp() {
    SecretKey key = Jwts.SIG.HS512.key().build();
    testSecret = Base64.getEncoder().encodeToString(key.getEncoded());
    testSecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret));

    tokenProvider = new TokenProvider(testSecret, accessTokenExpirationMinutes,
        refreshTokenExpirationDays);

    testUser = User.builder()
        .id(1L)
        .email("test@example.com")
        .nickname("testuser")
        .provider(OAuthProvider.GOOGLE)
        .socialId("google_12345")
        .role(RoleType.USER)
        .build();
  }

  @Test
  @DisplayName("액세스 토큰 생성 성공")
  void createAccessToken_shouldGenerateValidToken_whenUserProvided() {
    // when
    String accessToken = tokenProvider.createAccessToken(testUser);

    // then
    assertThat(accessToken).isNotNull();
    assertThat(tokenProvider.validateToken(accessToken)).isTrue();

    Claims claims = Jwts.parser()
        .verifyWith(testSecretKey)
        .build()
        .parseSignedClaims(accessToken)
        .getPayload();

    assertThat(claims.getSubject()).isEqualTo(testUser.getSocialId());
    assertThat(claims.get("userId", Long.class)).isEqualTo(testUser.getId());
    assertThat(claims.get("email", String.class)).isEqualTo(testUser.getEmail());
    assertThat(claims.get("provider", String.class)).isEqualTo(OAuthProvider.GOOGLE.getValue());
    assertThat(claims.getExpiration()).isAfter(new Date());
  }

  @Test
  @DisplayName("리프레시 토큰 생성 성공")
  void createRefreshToken_shouldGenerateValidToken_whenUserProvided() {
    // when
    String refreshToken = tokenProvider.createRefreshToken(testUser);

    // then
    assertThat(refreshToken).isNotNull();

    Claims claims = Jwts.parser()
        .verifyWith(testSecretKey)
        .build()
        .parseSignedClaims(refreshToken)
        .getPayload();

    assertThat(claims.getSubject()).isEqualTo(testUser.getSocialId());
    assertThat(claims.get("userId", Long.class)).isEqualTo(testUser.getId());
    assertThat(claims.getExpiration()).isAfter(new Date());
  }

  @Test
  @DisplayName("유효한 토큰 검증 성공")
  void validateToken_shouldReturnTrue_whenTokenIsValid() {
    // given
    String token = tokenProvider.createAccessToken(testUser);

    // when
    boolean isValid = tokenProvider.validateToken(token);

    // then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("만료된 토큰 검증 시 InvalidTokenException(EXPIRED_TOKEN) 발생")
  void validateToken_shouldThrowException_whenTokenIsExpired() {
    // given
    TokenProvider shortLivedTokenProvider = new TokenProvider(testSecret, 0, 0);
    String expiredToken = shortLivedTokenProvider.createAccessToken(testUser);

    // when and then
    assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
        .isInstanceOf(InvalidTokenException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.EXPIRED_TOKEN);
    
    assertThatThrownBy(() -> tokenProvider.validateToken(expiredToken))
        .isInstanceOf(InvalidTokenException.class)
        .hasCauseInstanceOf(ExpiredJwtException.class);
  }

  @Test
  @DisplayName("잘못된 서명 토큰 검증 시 InvalidTokenException 발생")
  void validateToken_shouldThrowException_whenTokenHasInvalidSignature() {
    // given
    String token = tokenProvider.createAccessToken(testUser);
    String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

    // when and then
    assertThatThrownBy(() -> tokenProvider.validateToken(tamperedToken))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessage("유효하지 않은 토큰 서명입니다.")
        .satisfies(exception -> {
          assertThat(exception.getCause()).isInstanceOfAny(
              io.jsonwebtoken.security.SecurityException.class,
              io.jsonwebtoken.MalformedJwtException.class);
        });
  }

  @Test
  @DisplayName("유효하지 않은 형식 토큰 검증 시 InvalidTokenException 발생")
  void validateToken_shouldThrowException_whenTokenIsMalformed() {
    // given
    String malformedToken = "this.is.not.a.jwt";

    // when and then
    assertThatThrownBy(() -> tokenProvider.validateToken(malformedToken))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessage("유효하지 않은 토큰 서명입니다.")
        .hasCauseInstanceOf(io.jsonwebtoken.MalformedJwtException.class);
  }

  @Test
  @DisplayName("토큰에서 사용자 ID 추출 성공")
  void getUserIdFromToken_shouldReturnUserId_whenTokenIsValid() {
    // given
    String token = tokenProvider.createAccessToken(testUser);

    // when
    Long userId = tokenProvider.getUserIdFromToken(token);

    // then
    assertThat(userId).isEqualTo(testUser.getId());
  }

  @Test
  @DisplayName("리프레시 토큰에서 사용자 ID 추출 성공")
  void getUserIdFromRefreshToken_shouldReturnUserId_whenTokenIsValid() {
    // given
    String refreshToken = tokenProvider.createRefreshToken(testUser);

    // when
    Long userId = tokenProvider.getUserIdFromToken(refreshToken);

    // then
    assertThat(userId).isEqualTo(testUser.getId());
  }

  @Test
  @DisplayName("만료된 토큰에서 사용자 ID 추출 시 예외 발생")
  void getUserIdFromToken_shouldThrowException_whenTokenIsExpired() {
    // given
    TokenProvider shortLivedTokenProvider = new TokenProvider(testSecret, 0, 0);
    String expiredToken = shortLivedTokenProvider.createAccessToken(testUser);

    // when and then
    assertThatThrownBy(() -> tokenProvider.getUserIdFromToken(expiredToken))
        .isInstanceOf(ExpiredJwtException.class);
  }
}