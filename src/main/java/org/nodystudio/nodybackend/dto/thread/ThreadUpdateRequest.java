package org.nodystudio.nodybackend.dto.thread;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.nodystudio.nodybackend.validation.NotBlankIfPresent;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadUpdateRequest {

  @NotBlankIfPresent(message = "내용은 공백일 수 없습니다.")
  @Size(min = 1, max = 5000, message = "내용은 1자 이상 5000자 이하여야 합니다.")
  private String content;

  private Boolean isPublic;

  private Long logId; // 로그 연결 변경 (null로 설정하면 독립 스레드로 변경)
}