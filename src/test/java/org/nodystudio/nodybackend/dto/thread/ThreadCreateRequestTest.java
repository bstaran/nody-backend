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
@DisplayName("ThreadCreateRequest 유효성 검증 테스트")
class ThreadCreateRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("유효한 요청 - 모든 필드 포함")
  void validate_ValidRequest_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("유효한 내용")
        .isPublic(true)
        .logId(1L)
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("유효한 요청 - 최소 필드만 포함")
  void validate_ValidRequestMinimalFields_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("내용")
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("내용이 null인 경우 유효성 검증 실패")
  void validate_NullContent_ValidationFails() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("내용은 필수입니다.");
  }

  @Test
  @DisplayName("내용이 빈 문자열인 경우 유효성 검증 실패")
  void validate_EmptyContent_ValidationFails() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("")
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(2);
    assertThat(violations.stream().map(ConstraintViolation::getMessage))
        .containsExactlyInAnyOrder("내용은 필수입니다.", "내용은 1자 이상 5000자 이하여야 합니다.");
  }

  @Test
  @DisplayName("내용이 공백만 있는 경우 유효성 검증 실패")
  void validate_BlankContent_ValidationFails() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("   ")
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("내용은 필수입니다.");
  }

  @Test
  @DisplayName("내용이 최대 길이를 초과하는 경우 유효성 검증 실패")
  void validate_ContentTooLong_ValidationFails() {
    // given
    String longContent = "a".repeat(5001); // 5001자
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content(longContent)
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("내용은 1자 이상 5000자 이하여야 합니다.");
  }

  @Test
  @DisplayName("내용이 최대 길이인 경우 유효성 검증 성공")
  void validate_ContentMaxLength_Success() {
    // given
    String maxLengthContent = "a".repeat(5000); // 5000자
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content(maxLengthContent)
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("내용이 최소 길이인 경우 유효성 검증 성공")
  void validate_ContentMinLength_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("a") // 1자
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("isPublic이 null인 경우 기본값 사용")
  void validate_NullIsPublic_UsesDefault() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("내용")
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
    assertThat(request.getIsPublic()).isTrue(); // 기본값 확인
  }

  @Test
  @DisplayName("logId가 null인 경우 유효성 검증 성공")
  void validate_NullLogId_Success() {
    // given
    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("내용")
        .logId(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }
}