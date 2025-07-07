package org.nodystudio.nodybackend.dto.thread;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadSearchRequest {

  @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
  @Builder.Default
  private int page = 0;

  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Builder.Default
  private int size = 20;

  @Pattern(regexp = "^(createdAt|viewCount)$", message = "정렬 기준은 createdAt 또는 viewCount만 가능합니다.")
  @Builder.Default
  private String sortBy = "createdAt";

  @Pattern(regexp = "^(asc|desc)$", message = "정렬 방향은 asc 또는 desc만 가능합니다.")
  @Builder.Default
  private String sortDirection = "desc";

  private String keyword;

  private Long logId;

  @Pattern(regexp = "^(all|independent|linked)$", message = "스레드 타입은 all, independent, linked만 가능합니다.")
  @Builder.Default
  private String threadType = "all"; // all: 전체, independent: 독립 스레드, linked: 로그 연결 스레드
}