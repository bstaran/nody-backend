package org.nodystudio.nodybackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Access Token 재발급 요청 DTO")
public class TokenRefreshRequestDto {

  @NotBlank(message = "Refresh Token은 필수입니다")
  @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
  private String refreshToken;
}