package org.nodystudio.nodybackend.dto.thread;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.SortDirection;
import org.nodystudio.nodybackend.domain.enums.ThreadSortField;
import org.nodystudio.nodybackend.domain.enums.ThreadType;
import org.springframework.test.context.ActiveProfiles;

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
    assertThat(request.getSortBy()).isEqualTo(ThreadSortField.CREATED_AT);
    assertThat(request.getSortDirection()).isEqualTo(SortDirection.DESC);
    assertThat(request.getThreadType()).isEqualTo(ThreadType.ALL);
  }

  @Test
  @DisplayName("유효한 요청 - 모든 필드 포함")
  void validate_ValidRequestAllFields_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(1)
        .size(10)
        .sortBy(ThreadSortField.VIEW_COUNT)
        .sortDirection(SortDirection.ASC)
        .keyword("검색어")
        .logId(123L)
        .threadType(ThreadType.INDEPENDENT)
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
  @DisplayName("정렬 기준 CREATED_AT - 검증 성공")
  void validate_SortBy_CreatedAt_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortBy(ThreadSortField.CREATED_AT)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 기준 VIEW_COUNT - 검증 성공")
  void validate_SortBy_ViewCount_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortBy(ThreadSortField.VIEW_COUNT)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 방향 ASC - 검증 성공")
  void validate_SortDirection_ASC_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortDirection(SortDirection.ASC)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("정렬 방향 DESC - 검증 성공")
  void validate_SortDirection_DESC_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortDirection(SortDirection.DESC)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("스레드 타입 ALL - 검증 성공")
  void validate_ThreadType_ALL_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType(ThreadType.ALL)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("스레드 타입 INDEPENDENT - 검증 성공")
  void validate_ThreadType_INDEPENDENT_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType(ThreadType.INDEPENDENT)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("스레드 타입 LINKED - 검증 성공")
  void validate_ThreadType_LINKED_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .threadType(ThreadType.LINKED)
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("enum 조합 테스트 - 모든 필드 유효한 enum 값 사용")
  void validate_EnumCombination_AllValid_Success() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .sortBy(ThreadSortField.VIEW_COUNT)
        .sortDirection(SortDirection.ASC)
        .threadType(ThreadType.INDEPENDENT)
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
  @DisplayName("여러 필드에서 유효성 검증 실패 - enum 사용으로 일부 오류 방지")
  void validate_MultipleFieldsInvalid_ValidationFails() {
    // given
    ThreadSearchRequest request = ThreadSearchRequest.builder()
        .page(-1) // 음수 페이지
        .size(0) // 0 크기
        // enum 사용으로 인해 유효하지 않은 값들은 컴파일 타임에 방지됨
        .sortBy(null) // null 값만 가능
        .sortDirection(null) // null 값만 가능
        .threadType(null) // null 값만 가능
        .build();

    // when
    Set<ConstraintViolation<ThreadSearchRequest>> violations = validator.validate(request);

    // then
    // enum 사용으로 인해 유효성 검증 오류가 줄어듦
    assertThat(violations).hasSize(2);
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .containsExactlyInAnyOrder(
            "페이지 번호는 0 이상이어야 합니다.",
            "페이지 크기는 1 이상이어야 합니다.");
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