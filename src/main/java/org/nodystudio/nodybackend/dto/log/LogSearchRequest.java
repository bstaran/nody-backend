package org.nodystudio.nodybackend.dto.log;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 로그 검색을 위한 요청 DTO 클래스
 * 
 * <p>
 * 위치 기반 로그 검색 시 사용되는 파라미터들을 포함합니다.
 * 위도와 경도는 필수 입력값이며, 그 외의 필드는 기본값이 설정되어 있습니다.
 * </p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogSearchRequest {

  @NotNull(message = "위도는 필수입니다.")
  @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다.")
  @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다.")
  private BigDecimal latitude;

  @NotNull(message = "경도는 필수입니다.")
  @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다.")
  @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다.")
  private BigDecimal longitude;

  /**
   * 검색 반경 (단위: km)
   * <p>
   * 기본값: 10.0km (최소 0.1km ~ 최대 100km)
   * </p>
   */
  @DecimalMin(value = "0.1", message = "반경은 0.1km 이상이어야 합니다.")
  @DecimalMax(value = "100.0", message = "반경은 100km 이하여야 합니다.")
  @Builder.Default
  private BigDecimal radiusKm = BigDecimal.valueOf(10.0);

  /**
   * 공개 여부 필터
   * <p>
   * true: 공개 로그만, false: 비공개 로그만, null: 전체 (권한에 따라 필터링)
   * </p>
   */
  private Boolean isPublic;

  @Min(value = 0, message = "페이지는 0 이상이어야 합니다.")
  @Builder.Default
  private Integer page = 0;

  /**
   * 페이지 크기
   * <p>
   * 기본값: 20 (최소 1 ~ 최대 100)
   * </p>
   */
  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
  @Builder.Default
  private Integer size = 20;

  /**
   * 정렬 기준
   * <p>
   * 가능한 값: createdAt, viewCount, distance
   * </p>
   * <p>
   * 기본값: createdAt
   * </p>
   */
  @Builder.Default
  private String sortBy = "createdAt";

  /**
   * 정렬 방향
   * <p>
   * 가능한 값: asc, desc
   * </p>
   * <p>
   * 기본값: desc
   * </p>
   */
  @Builder.Default
  private String sortDirection = "desc";
}