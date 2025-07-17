package org.nodystudio.nodybackend.dto.like;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.nodystudio.nodybackend.domain.enums.TargetType;

/**
 * 좋아요 상태 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "좋아요 상태 응답 DTO")
public class LikeStatusResponse {

  @Schema(description = "좋아요 여부", example = "true")
  private final Boolean isLiked;

  @Schema(description = "좋아요 개수", example = "15")
  private final Long likeCount;

  @Schema(description = "대상 타입", example = "THREAD")
  private final TargetType targetType;

  @Schema(description = "대상 ID", example = "1")
  private final Long targetId;

  /**
   * 좋아요 상태 응답 생성 메서드
   *
   * @param isLiked    좋아요 여부
   * @param likeCount  좋아요 개수
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @return 좋아요 상태 응답 DTO
   */
  public static LikeStatusResponse of(Boolean isLiked, Long likeCount, TargetType targetType,
      Long targetId) {
    return LikeStatusResponse.builder()
        .isLiked(isLiked)
        .likeCount(likeCount)
        .targetType(targetType)
        .targetId(targetId)
        .build();
  }
}