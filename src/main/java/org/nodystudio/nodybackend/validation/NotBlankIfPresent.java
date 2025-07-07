package org.nodystudio.nodybackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 값이 제공된 경우 공백이 아니어야 함을 검증하는 애노테이션
 * null은 허용하지만, 값이 있으면 공백이면 안됨
 */
@Documented
@Constraint(validatedBy = NotBlankIfPresentValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlankIfPresent {
    
    String message() default "값이 제공된 경우 공백일 수 없습니다.";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}