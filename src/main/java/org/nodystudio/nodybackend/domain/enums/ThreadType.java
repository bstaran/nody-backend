package org.nodystudio.nodybackend.domain.enums;

/**
 * 스레드 타입을 나타내는 열거형
 */
public enum ThreadType {
  ALL("all", "전체"),
  INDEPENDENT("independent", "독립 스레드"),
  LINKED("linked", "로그 연결 스레드");

  private final String value;
  private final String description;

  ThreadType(String value, String description) {
    this.value = value;
    this.description = description;
  }

  /**
   * 문자열 값으로부터 ThreadType 열거형을 찾습니다.
   *
   * @param value 스레드 타입 문자열 값
   * @return 해당하는 ThreadType 열거형
   * @throws IllegalArgumentException 지원하지 않는 스레드 타입인 경우
   */
  public static ThreadType fromValue(String value) {
    for (ThreadType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 스레드 타입입니다: " + value);
  }

  /**
   * 문자열 값으로부터 ThreadType 열거형을 찾습니다. 찾지 못하면 null을 반환합니다.
   *
   * @param value 스레드 타입 문자열 값
   * @return 해당하는 ThreadType 열거형 또는 null
   */
  public static ThreadType fromValueOrNull(String value) {
    try {
      return fromValue(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 문자열 값을 반환합니다.
   *
   * @return 스레드 타입의 문자열 값
   */
  public String getValue() {
    return value;
  }

  /**
   * 스레드 타입의 설명을 반환합니다.
   *
   * @return 스레드 타입의 설명
   */
  public String getDescription() {
    return description;
  }
}