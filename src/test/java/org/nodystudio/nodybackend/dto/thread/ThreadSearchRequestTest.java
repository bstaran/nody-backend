package org.nodystudio.nodybackend.dto.thread;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@ActiveProfiles("test")
@DisplayName("ThreadSearchRequest 유효성 검증 테스트")
class ThreadSearchRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("유효한 요청 - 기본값 사용")
  void validate_ValidRequestWithDefaults_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder().build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
    assertThat(request.getPage()).isEqualTo(0);
    assertThat(request.getSize()).isEqualTo(20);
    assertThat(request.getSortBy()).isEqualTo("createdAt");
    assertThat(request.getSortDirection()).isEqualTo("desc");
    assertThat(request.getThreadType()).isEqualTo("all");
  }

  @Test
  @DisplayName("유효한 요청 - 모든 필드 포함")
  void validate_ValidRequestAllFields_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(1)
        .size(10)
        .sortBy("viewCount")
        .sortDirection("asc")
        .keyword("검색어")
        .logId(123L)
        .threadType("independent")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("페이지 번호가 음수인 경우 유효성 검증 실패")
  void validate_NegativePage_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(-1)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("페이지 번호는 0 이상이어야 합니다.");
  }

  @Test
  @DisplayName("페이지 번호가 0인 경우 유효성 검증 성공")
  void validate_PageZero_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(0)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("페이지 크기가 0인 경우 유효성 검증 실패")
  void validate_SizeZero_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .size(0)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("페이지 크기는 1 이상이어야 합니다.");
  }

  @Test
  @DisplayName("페이지 크기가 음수인 경우 유효성 검증 실패")
  void validate_NegativeSize_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .size(-1)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("페이지 크기는 1 이상이어야 합니다.");
  }

  @Test
  @DisplayName("정렬 기준이 유효하지 않은 경우 유효성 검증 실패")
  void validate_InvalidSortBy_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortBy("invalidField")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("정렬 기준은 createdAt 또는 viewCount만 가능합니다.");
  }

  @Test
  @DisplayName("정렬 기준이 createdAt인 경우 유효성 검증 성공")
  void validate_SortByCreatedAt_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortBy("createdAt")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 기준이 viewCount인 경우 유효성 검증 성공")
  void validate_SortByViewCount_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortBy("viewCount")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 방향이 유효하지 않은 경우 유효성 검증 실패")
  void validate_InvalidSortDirection_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortDirection("invalid")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("정렬 방향은 asc 또는 desc만 가능합니다.");
  }

  @Test
  @DisplayName("정렬 방향이 asc인 경우 유효성 검증 성공")
  void validate_SortDirectionAsc_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortDirection("asc")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 방향이 desc인 경우 유효성 검증 성공")
  void validate_SortDirectionDesc_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortDirection("desc")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("스레드 타입이 유효하지 않은 경우 유효성 검증 실패")
  void validate_InvalidThreadType_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType("invalid")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("스레드 타입은 all, independent, linked만 가능합니다.");
  }

  @Test
  @DisplayName("스레드 타입이 all인 경우 유효성 검증 성공")
  void validate_ThreadTypeAll_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType("all")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("스레드 타입이 independent인 경우 유효성 검증 성공")
  void validate_ThreadTypeIndependent_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType("independent")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("스레드 타입이 linked인 경우 유효성 검증 성공")
  void validate_ThreadTypeLinked_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType("linked")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("키워드가 null인 경우 유효성 검증 성공")
  void validate_NullKeyword_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .keyword(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("키워드가 빈 문자열인 경우 유효성 검증 성공")
  void validate_EmptyKeyword_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .keyword("")
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("logId가 null인 경우 유효성 검증 성공")
  void validate_NullLogId_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .logId(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("여러 필드에서 유효성 검증 실패")
  void validate_MultipleFieldsInvalid_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(-1) // 음수 페이지
        .size(0) // 0 크기
        .sortBy("invalid") // 유효하지 않은 정렬 기준
        .sortDirection("wrong") // 유효하지 않은 정렬 방향
        .threadType("wrong") // 유효하지 않은 스레드 타입
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(5);
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .containsExactlyInAnyOrder(
            "페이지 번호는 0 이상이어야 합니다.",
            "페이지 크기는 1 이상이어야 합니다.",
            "정렬 기준은 createdAt 또는 viewCount만 가능합니다.",
            "정렬 방향은 asc 또는 desc만 가능합니다.",
            "스레드 타입은 all, independent, linked만 가능합니다.");
  }

  @Test
  @DisplayName("경계값 테스트 - 최소값")
  void validate_BoundaryValues_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(0) // 최소 페이지
        .size(1) // 최소 크기
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("큰 값들로 유효성 검증 성공")
  void validate_LargeValues_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(1000)
        .size(100)
        .logId(999999L)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }
}