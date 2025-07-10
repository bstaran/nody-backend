package org.nodystudio.nodybackend.controller.test;

import static org.nodystudio.nodybackend.dto.code.ErrorCode.USER_NOT_AUTHENTICATED;

import java.util.ArrayList;
import java.util.List;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.FieldErrorDto;
import org.nodystudio.nodybackend.dto.code.SuccessCode;
import org.nodystudio.nodybackend.exception.custom.ForbiddenException;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UnauthorizedException;
import org.nodystudio.nodybackend.exception.custom.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예외 처리 테스트를 위한 컨트롤러 테스트 환경에서만 사용되는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/test/exceptions")
public class ExceptionTestController {

  /**
   * 정상 응답 테스트
   */
  @GetMapping("/ok")
  public ResponseEntity<ApiResponse<?>> getOk() {
    String data = "이것은 성공적인 응답의 데이터 부분입니다.";
    return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, data));
  }

  /**
   * ResourceNotFoundException 테스트
   */
  @GetMapping("/not-found")
  public ResponseEntity<Void> getNotFound() {
    throw ResourceNotFoundException.of("User", "id", "12345");
  }

  @GetMapping("/unauthorized")
  public ResponseEntity<Void> getUnauthorized() {
    throw new UnauthorizedException("인증되지 않은 사용자입니다", USER_NOT_AUTHENTICATED);
  }

  /**
   * ForbiddenException 테스트
   */
  @GetMapping("/forbidden")
  public ResponseEntity<Void> getForbidden() {
    throw new ForbiddenException();
  }

  /**
   * ValidationException 테스트
   */
  @GetMapping("/validation")
  public ResponseEntity<Void> getValidationError() {
    List<FieldErrorDto> fieldErrorsList = new ArrayList<>();
    fieldErrorsList.add(new FieldErrorDto("username", "사용자 이름은 4자에서 20자 사이여야 합니다"));
    fieldErrorsList.add(new FieldErrorDto("email", "이메일 형식이 올바르지 않습니다"));

    throw new ValidationException("요청 유효성 검사에 실패했습니다", fieldErrorsList);
  }

  /**
   * 일반 예외 테스트
   */
  @GetMapping("/error")
  public ResponseEntity<Void> getError() {
    throw new RuntimeException();
  }

  /**
   * Spring Security 인증 실패 테스트 (401 Unauthorized) 이 엔드포인트는 인증이 필요하므로 토큰 없이 접근하면
   * CustomAuthenticationEntryPoint가 호출됩니다.
   */
  @GetMapping("/security-auth-test")
  public ResponseEntity<ApiResponse<?>> getSecurityAuthTest() {
    return ResponseEntity.ok(ApiResponse.success("인증된 사용자만 볼 수 있는 데이터"));
  }

  /**
   * Spring Security 권한 부족 테스트 (403 Forbidden) 이 엔드포인트는 ADMIN 권한이 필요하므로 일반 사용자가 접근하면
   * CustomAccessDeniedHandler가 호출됩니다.
   */
  @GetMapping("/security-access-test")
  public ResponseEntity<ApiResponse<?>> getSecurityAccessTest() {
    return ResponseEntity.ok(ApiResponse.success("ADMIN 권한이 있는 사용자만 볼 수 있는 데이터"));
  }
}
