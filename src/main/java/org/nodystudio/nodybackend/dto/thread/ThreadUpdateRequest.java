package org.nodystudio.nodybackend.dto.thread;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

  private Long logId; // 로그 연결 변경

  private Boolean disconnectLog; // 로그 연결 해제 (true로 설정하면 독립 스레드로 변경)

  /**
   * logId와 disconnectLog는 동시에 설정될 수 없습니다.
   */
  @AssertTrue(message = "로그 연결과 연결 해제는 동시에 요청할 수 없습니다.")
  public boolean isLogRequestValid() {
    return !(logId != null && Boolean.TRUE.equals(disconnectLog));
  }
}