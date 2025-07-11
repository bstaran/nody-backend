package org.nodystudio.nodybackend.dto.thread;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nodystudio.nodybackend.domain.enums.SortDirection;
import org.nodystudio.nodybackend.domain.enums.ThreadSortField;
import org.nodystudio.nodybackend.domain.enums.ThreadType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadSearchRequest {

  @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
  @Builder.Default
  private int page = 0;

  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
  @Builder.Default
  private int size = 20;

  @Builder.Default
  private ThreadSortField sortBy = ThreadSortField.CREATED_AT;

  @Builder.Default
  private SortDirection sortDirection = SortDirection.DESC;

  private String keyword;

  private Long logId;

  @Builder.Default
  private ThreadType threadType = ThreadType.ALL; // all: 전체, independent: 독립 스레드, linked: 로그 연결 스레드
}