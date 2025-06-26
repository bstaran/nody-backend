package org.nodystudio.nodybackend.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.code.ErrorCode;
import org.nodystudio.nodybackend.dto.code.SuccessCode;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


/**
 * 사용자 관련 API
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /**
   * 인증된 사용자의 기본 정보를 조회
   *
   * @param user 인증된 사용자 정보 (@ignore)
   * @return UserDetailResponseDto 사용자 정보
   */
  @GetMapping(value = "/me")
  public ResponseEntity<ApiResponse<UserDetailResponseDto>> getCurrentUser(
      @AuthenticationPrincipal User user) {

    if (user == null) {
      return ResponseEntity
          .status(ErrorCode.USER_NOT_AUTHENTICATED.getStatus())
          .body(ApiResponse.error(ErrorCode.USER_NOT_AUTHENTICATED));
    }

    UserDetailResponseDto userDetail = userService.getCurrentUser(user.getId().toString());

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
  @PutMapping(value = "/nickname")
  public ResponseEntity<ApiResponse<UserDetailResponseDto>> updateNickname(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody UpdateNicknameRequestDto requestDto) {

    if (user == null) {
      return ResponseEntity
          .status(ErrorCode.USER_NOT_AUTHENTICATED.getStatus())
          .body(ApiResponse.error(ErrorCode.USER_NOT_AUTHENTICATED));
    }

    UserDetailResponseDto updatedUser = userService.updateNickname(user.getId().toString(), requestDto);

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, updatedUser));
  }

  /**
   * 사용자 계정을 탈퇴합니다
   * - 즉시 계정 비활성화 및 사용자 데이터 삭제
   * - 30일 후 완전 삭제 (배치 처리)
   *
   * @param user 인증된 사용자 정보 (@ignore)
   * @return 탈퇴 처리 결과
   */
  @DeleteMapping(value = "/me")
  public ResponseEntity<ApiResponse<Void>> deactivateAccount(
      @AuthenticationPrincipal User user) {

    if (user == null) {
      return ResponseEntity
          .status(ErrorCode.USER_NOT_AUTHENTICATED.getStatus())
          .body(ApiResponse.error(ErrorCode.USER_NOT_AUTHENTICATED));
    }

    userService.deactivateAccount(user.getId().toString());

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, null));
  }
}
