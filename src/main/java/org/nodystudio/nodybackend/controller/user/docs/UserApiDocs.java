package org.nodystudio.nodybackend.controller.user.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 사용자 관리 API 문서화 인터페이스
 */
@Tag(name = "사용자", description = "사용자 정보 관리 및 계정 관련 API")
@SecurityRequirement(name = "bearerAuth")
public interface UserApiDocs {

  @Operation(
      summary = "현재 사용자 정보 조회",
      description = "로그인한 사용자의 상세 정보를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "사용자 정보 조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<UserDetailResponseDto>> getCurrentUser(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser
  );

  @Operation(
      summary = "닉네임 변경",
      description = "현재 사용자의 닉네임을 변경합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "닉네임 변경 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "400",
          description = "유효하지 않은 닉네임 형식",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 사용 중인 닉네임",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<UserDetailResponseDto>> updateNickname(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
      @Valid @RequestBody UpdateNicknameRequestDto requestDto
  );

  @Operation(
      summary = "계정 탈퇴",
      description = "현재 사용자의 계정을 탈퇴 처리합니다. 30일 후 완전히 삭제됩니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "계정 탈퇴 처리 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Void>> deactivateAccount(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser
  );
}