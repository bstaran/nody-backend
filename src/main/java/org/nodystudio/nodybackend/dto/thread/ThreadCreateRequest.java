package org.nodystudio.nodybackend.dto.thread;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadCreateRequest {

  @NotBlank(message = "내용은 필수입니다.")
  @Size(min = 1, max = 5000, message = "내용은 1자 이상 5000자 이하여야 합니다.")
  private String content;

  @Builder.Default
  private Boolean isPublic = true;

  private Long logId;
}