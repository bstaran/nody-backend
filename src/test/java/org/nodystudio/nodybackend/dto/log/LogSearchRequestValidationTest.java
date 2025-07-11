package org.nodystudio.nodybackend.dto.log;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.LogSortField;
import org.nodystudio.nodybackend.domain.enums.SortDirection;

/**
 * LogSearchRequest Bean Validation 테스트
 */
@DisplayName("LogSearchRequest 검증 테스트")
class LogSearchRequestValidationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("유효한 LogSearchRequest - 검증 통과")
  void validLogSearchRequest_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .radiusKm(new BigDecimal("10.0"))
        .page(0)
        .size(20)
        .sortBy(LogSortField.CREATED_AT)
        .sortDirection(SortDirection.DESC)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 기준 CREATED_AT - 검증 성공")
  void validSortBy_CreatedAt_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortBy(LogSortField.CREATED_AT)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 기준 VIEW_COUNT - 검증 성공")
  void validSortBy_ViewCount_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortBy(LogSortField.VIEW_COUNT)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 기준 DISTANCE - 검증 성공")
  void validSortBy_Distance_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortBy(LogSortField.DISTANCE)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 방향 ASC - 검증 성공")
  void validSortDirection_Asc_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortDirection(SortDirection.ASC)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 방향 DESC - 검증 성공")
  void validSortDirection_Desc_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortDirection(SortDirection.DESC)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 필드와 방향 조합 - 검증 성공")
  void validSortCombination_ShouldPassValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortBy(LogSortField.DISTANCE)
        .sortDirection(SortDirection.ASC)
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("null 위도 - 검증 실패")
  void nullLatitude_ShouldFailValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(null) // null 위도
        .longitude(new BigDecimal("126.9780"))
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    ConstraintViolation<LogSearchRequest> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("위도는 필수입니다.");
    assertThat(violation.getPropertyPath().toString()).isEqualTo("latitude");
  }

  @Test
  @DisplayName("null 경도 - 검증 실패")
  void nullLongitude_ShouldFailValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(null) // null 경도
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    ConstraintViolation<LogSearchRequest> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("경도는 필수입니다.");
    assertThat(violation.getPropertyPath().toString()).isEqualTo("longitude");
  }

  @Test
  @DisplayName("범위 초과 위도 - 검증 실패")
  void outOfRangeLatitude_ShouldFailValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("999.0")) // 범위 초과 위도
        .longitude(new BigDecimal("126.9780"))
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    ConstraintViolation<LogSearchRequest> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("위도는 90도 이하여야 합니다.");
    assertThat(violation.getPropertyPath().toString()).isEqualTo("latitude");
  }

  @Test
  @DisplayName("범위 초과 경도 - 검증 실패")
  void outOfRangeLongitude_ShouldFailValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("999.0")) // 범위 초과 경도
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    ConstraintViolation<LogSearchRequest> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("경도는 180도 이하여야 합니다.");
    assertThat(violation.getPropertyPath().toString()).isEqualTo("longitude");
  }
}