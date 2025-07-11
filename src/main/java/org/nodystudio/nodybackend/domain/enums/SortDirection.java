package org.nodystudio.nodybackend.domain.enums;

/**
 * 정렬 방향을 나타내는 열거형
 */
public enum SortDirection {
  ASC("asc"),
  DESC("desc");

  private final String value;

  SortDirection(String value) {
    this.value = value;
  }

  /**
   * 문자열 값으로부터 SortDirection 열거형을 찾습니다.
   *
   * @param value 정렬 방향 문자열 값
   * @return 해당하는 SortDirection 열거형
   * @throws IllegalArgumentException 지원하지 않는 정렬 방향인 경우
   */
  public static SortDirection fromValue(String value) {
    for (SortDirection direction : values()) {
      if (direction.value.equals(value)) {
        return direction;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 정렬 방향입니다: " + value);
  }

  /**
   * 문자열 값으로부터 SortDirection 열거형을 찾습니다. 찾지 못하면 null을 반환합니다.
   *
   * @param value 정렬 방향 문자열 값
   * @return 해당하는 SortDirection 열거형 또는 null
   */
  public static SortDirection fromValueOrNull(String value) {
    try {
      return fromValue(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 문자열 값을 반환합니다.
   *
   * @return 정렬 방향의 문자열 값
   */
  public String getValue() {
    return value;
  }
}