package org.nodystudio.nodybackend.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nodystudio.nodybackend.controller.user.docs.UserApiDocs;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.code.SuccessCode;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관련 API
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController implements UserApiDocs {

  private final UserService userService;

  /**
   * 인증된 사용자의 기본 정보를 조회
   *
   * <p>
   * 이 엔드포인트는 Spring Security에 의해 관리되며, 인증된 사용자만 접근할 수 있습니다. {@code @AuthenticationPrincipal}을 통해
   * 주입되는 user 객체는 Spring Security가 이미 검증했으므로 별도의 null 체크가 불필요합니다.
   * </p>
   *
   * @param user 인증된 사용자 정보 (Spring Security에 의해 보장됨)
   * @return UserDetailResponseDto 사용자 정보
   */
  @Override
  @GetMapping(value = "/me")
  public ResponseEntity<ApiResponse<UserDetailResponseDto>> getCurrentUser(
      @AuthenticationPrincipal Object user) {
    User currentUser = (User) user;
    UserDetailResponseDto userDetail = userService.getCurrentUser(currentUser.getId().toString());

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, userDetail));
  }

  /**
   * 사용자의 닉네임을 변경
   *
   * @param user       인증된 사용자 정보 (@ignore)
   * @param requestDto 닉네임 변경 요청 DTO
   * @return UserDetailResponseDto 변경된 사용자 정보
   */
  @Override
  @PutMapping(value = "/nickname")
  public ResponseEntity<ApiResponse<UserDetailResponseDto>> updateNickname(
      @AuthenticationPrincipal Object user,
      @Valid @RequestBody UpdateNicknameRequestDto requestDto) {
    User currentUser = (User) user;
    UserDetailResponseDto updatedUser = userService.updateNickname(currentUser.getId().toString(),
        requestDto);

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, updatedUser));
  }

  /**
   * 사용자 계정을 탈퇴합니다 - 즉시 계정 비활성화 및 사용자 데이터 삭제 - 30일 후 완전 삭제 (배치 처리)
   *
   * @param user 인증된 사용자 정보 (@ignore)
   * @return 탈퇴 처리 결과
   */
  @Override
  @DeleteMapping(value = "/me")
  public ResponseEntity<ApiResponse<Void>> deactivateAccount(
      @AuthenticationPrincipal Object user) {
    User currentUser = (User) user;
    userService.deactivateAccount(currentUser.getId().toString());

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, null));
  }
}
