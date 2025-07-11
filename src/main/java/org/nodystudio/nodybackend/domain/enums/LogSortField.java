package org.nodystudio.nodybackend.domain.enums;

/**
 * 로그 정렬 필드를 나타내는 열거형
 */
public enum LogSortField {
  CREATED_AT("createdAt", "생성일시"),
  VIEW_COUNT("viewCount", "조회수"),
  DISTANCE("distance", "거리");

  private final String value;
  private final String description;

  LogSortField(String value, String description) {
    this.value = value;
    this.description = description;
  }

  /**
   * 문자열 값으로부터 LogSortField 열거형을 찾습니다.
   *
   * @param value 정렬 필드 문자열 값
   * @return 해당하는 LogSortField 열거형
   * @throws IllegalArgumentException 지원하지 않는 정렬 필드인 경우
   */
  public static LogSortField fromValue(String value) {
    for (LogSortField field : values()) {
      if (field.value.equals(value)) {
        return field;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 로그 정렬 필드입니다: " + value);
  }

  /**
   * 문자열 값으로부터 LogSortField 열거형을 찾습니다. 찾지 못하면 null을 반환합니다.
   *
   * @param value 정렬 필드 문자열 값
   * @return 해당하는 LogSortField 열거형 또는 null
   */
  public static LogSortField fromValueOrNull(String value) {
    try {
      return fromValue(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 문자열 값을 반환합니다.
   *
   * @return 정렬 필드의 문자열 값
   */
  public String getValue() {
    return value;
  }

  /**
   * 정렬 필드의 설명을 반환합니다.
   *
   * @return 정렬 필드의 설명
   */
  public String getDescription() {
    return description;
  }
}