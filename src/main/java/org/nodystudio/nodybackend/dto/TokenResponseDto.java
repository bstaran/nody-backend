package org.nodystudio.nodybackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "JWT 토큰 응답 DTO")
public class TokenResponseDto {

  @Schema(description = "권한 부여 타입", example = "Bearer")
  private String grantType;

  @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String accessToken;

  @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String refreshToken;

  @Schema(description = "Access Token 만료 시간 (초 단위)", example = "3600")
  private Long accessTokenExpiresIn;
}