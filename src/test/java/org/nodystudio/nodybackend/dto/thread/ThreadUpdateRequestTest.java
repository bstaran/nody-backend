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
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DisplayName("ThreadUpdateRequest 유효성 검증 테스트")
class ThreadUpdateRequestTest {

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
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("수정된 내용")
        .isPublic(false)
        .logId(1L)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("유효한 요청 - 일부 필드만 포함")
  void validate_ValidRequestPartialFields_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("수정된 내용")
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("유효한 요청 - 모든 필드가 null")
  void validate_AllFieldsNull_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder().build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("내용이 빈 문자열인 경우 유효성 검증 실패")
  void validate_EmptyContent_ValidationFails() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("")
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(2);
    assertThat(violations.stream().map(ConstraintViolation::getMessage))
        .containsExactlyInAnyOrder("내용은 공백일 수 없습니다.", "내용은 1자 이상 5000자 이하여야 합니다.");
  }

  @Test
  @DisplayName("내용이 공백만 있는 경우 유효성 검증 실패")
  void validate_BlankContent_ValidationFails() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("   ")
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("내용은 공백일 수 없습니다.");
  }

  @Test
  @DisplayName("내용이 1자인 경우 유효성 검증 성공")
  void validate_ContentMinLength_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("a")
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("내용이 최대 길이를 초과하는 경우 유효성 검증 실패")
  void validate_ContentTooLong_ValidationFails() {
    // given
    String longContent = "a".repeat(5001); // 5001자
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content(longContent)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

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
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content(maxLengthContent)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("내용이 null인 경우 유효성 검증 성공")
  void validate_NullContent_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("isPublic 필드 설정")
  void validate_IsPublicField_Success() {
    // given
    ThreadUpdateRequest request1 = ThreadUpdateRequest.builder()
        .isPublic(true)
        .build();

    ThreadUpdateRequest request2 = ThreadUpdateRequest.builder()
        .isPublic(false)
        .build();

    ThreadUpdateRequest request3 = ThreadUpdateRequest.builder()
        .isPublic(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations1 = validator.validate(request1);
    Set<ConstraintViolation<ThreadUpdateRequest>> violations2 = validator.validate(request2);
    Set<ConstraintViolation<ThreadUpdateRequest>> violations3 = validator.validate(request3);

    // then
    assertThat(violations1).isEmpty();
    assertThat(violations2).isEmpty();
    assertThat(violations3).isEmpty();
  }

  @Test
  @DisplayName("logId 필드 설정")
  void validate_LogIdField_Success() {
    // given
    ThreadUpdateRequest request1 = ThreadUpdateRequest.builder()
        .logId(1L)
        .build();

    ThreadUpdateRequest request2 = ThreadUpdateRequest.builder()
        .logId(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations1 = validator.validate(request1);
    Set<ConstraintViolation<ThreadUpdateRequest>> violations2 = validator.validate(request2);

    // then
    assertThat(violations1).isEmpty();
    assertThat(violations2).isEmpty();
  }

  @Test
  @DisplayName("내용이 매우 긴 경우 유효성 검증 실패")
  void validate_VeryLongContent_ValidationFails() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .content("a".repeat(5001)) // 너무 긴 내용
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("내용은 1자 이상 5000자 이하여야 합니다.");
  }

  @Test
  @DisplayName("경계값 테스트 - 내용 최소/최대 길이")
  void validate_ContentBoundaryValues_Success() {
    // given
    ThreadUpdateRequest minRequest = ThreadUpdateRequest.builder()
        .content("a") // 1자
        .build();

    ThreadUpdateRequest maxRequest = ThreadUpdateRequest.builder()
        .content("a".repeat(5000)) // 5000자
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> minViolations = validator.validate(minRequest);
    Set<ConstraintViolation<ThreadUpdateRequest>> maxViolations = validator.validate(maxRequest);

    // then
    assertThat(minViolations).isEmpty();
    assertThat(maxViolations).isEmpty();
  }

  @Test
  @DisplayName("로그 연결 해제 - 유효한 요청")
  void validate_DisconnectLog_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .disconnectLog(true)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("로그 연결과 해제 동시 요청 - 유효성 검증 실패")
  void validate_LogIdAndDisconnectLogBoth_ValidationFails() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .logId(1L)
        .disconnectLog(true)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .isEqualTo("로그 연결과 연결 해제는 동시에 요청할 수 없습니다.");
  }

  @Test
  @DisplayName("로그 연결 변경 - 유효한 요청")
  void validate_ChangeLogId_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .logId(2L)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("disconnectLog가 false인 경우와 logId 동시 요청 - 유효한 요청")
  void validate_DisconnectLogFalseWithLogId_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .logId(1L)
        .disconnectLog(false)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("disconnectLog가 null인 경우와 logId 동시 요청 - 유효한 요청")
  void validate_DisconnectLogNullWithLogId_Success() {
    // given
    ThreadUpdateRequest request = ThreadUpdateRequest.builder()
        .logId(1L)
        .disconnectLog(null)
        .build();

    // when
    Set<ConstraintViolation<ThreadUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }
}