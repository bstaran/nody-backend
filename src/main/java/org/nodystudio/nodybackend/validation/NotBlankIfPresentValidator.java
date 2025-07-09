package org.nodystudio.nodybackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * NotBlankIfPresent 애노테이션의 유효성 검증 로직
 */
public class NotBlankIfPresentValidator implements ConstraintValidator<NotBlankIfPresent, String> {

  @Override
  public void initialize(NotBlankIfPresent constraintAnnotation) {
    // 초기화 로직
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // null은 허용
    if (value == null) {
      return true;
    }

    // 값이 제공된 경우 공백이 아니어야 함
    return !value.trim().isEmpty();
  }
}