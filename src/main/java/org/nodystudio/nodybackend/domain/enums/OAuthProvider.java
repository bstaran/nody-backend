package org.nodystudio.nodybackend.domain.enums;

/**
 * OAuth2 소셜 로그인 제공자를 나타내는 열거형
 */
public enum OAuthProvider {
  GOOGLE("google"),
  KAKAO("kakao");

  private final String value;

  OAuthProvider(String value) {
    this.value = value;
  }

  /**
   * 문자열 값으로부터 OAuthProvider 열거형을 찾습니다.
   *
   * @param value 소셜 로그인 제공자 문자열 값
   * @return 해당하는 OAuthProvider 열거형
   * @throws IllegalArgumentException 지원하지 않는 제공자인 경우
   */
  public static OAuthProvider fromValue(String value) {
    for (OAuthProvider provider : values()) {
      if (provider.value.equals(value)) {
        return provider;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + value);
  }

  /**
   * 문자열 값으로부터 OAuthProvider 열거형을 찾습니다. 찾지 못하면 null을 반환합니다.
   *
   * @param value 소셜 로그인 제공자 문자열 값
   * @return 해당하는 OAuthProvider 열거형 또는 null
   */
  public static OAuthProvider fromValueOrNull(String value) {
    try {
      return fromValue(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 문자열 값을 반환합니다.
   *
   * @return 소셜 로그인 제공자의 문자열 값
   */
  public String getValue() {
    return value;
  }
}