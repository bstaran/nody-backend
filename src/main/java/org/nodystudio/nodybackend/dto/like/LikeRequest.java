package org.nodystudio.nodybackend.dto.like;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nodystudio.nodybackend.domain.enums.TargetType;

/**
 * 좋아요 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "좋아요 요청 DTO")
public class LikeRequest {

  @Schema(description = "대상 타입", example = "THREAD", allowableValues = {"THREAD", "LOG"})
  @NotNull(message = "대상 타입은 필수입니다.")
  private TargetType targetType;

  @Schema(description = "대상 ID", example = "1")
  @NotNull(message = "대상 ID는 필수입니다.")
  @Positive(message = "대상 ID는 양수여야 합니다.")
  private Long targetId;
}