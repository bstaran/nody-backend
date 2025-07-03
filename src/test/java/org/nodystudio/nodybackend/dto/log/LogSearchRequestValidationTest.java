package org.nodystudio.nodybackend.dto.log;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

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
        .sortBy("createdAt")
        .sortDirection("desc")
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("잘못된 정렬 기준 - 검증 실패")
  void invalidSortBy_ShouldFailValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortBy("invalidSort") // 잘못된 정렬 기준
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    ConstraintViolation<LogSearchRequest> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("정렬 기준은 createdAt, viewCount, distance 중 하나여야 합니다.");
    assertThat(violation.getPropertyPath().toString()).isEqualTo("sortBy");
  }

  @Test
  @DisplayName("잘못된 정렬 방향 - 검증 실패")
  void invalidSortDirection_ShouldFailValidation() {
    // given
    LogSearchRequest request = LogSearchRequest.builder()
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .sortDirection("invalidDirection") // 잘못된 정렬 방향
        .build();

    // when
    Set<ConstraintViolation<LogSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    ConstraintViolation<LogSearchRequest> violation = violations.iterator().next();
    assertThat(violation.getMessage()).isEqualTo("정렬 방향은 asc 또는 desc여야 합니다.");
    assertThat(violation.getPropertyPath().toString()).isEqualTo("sortDirection");
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