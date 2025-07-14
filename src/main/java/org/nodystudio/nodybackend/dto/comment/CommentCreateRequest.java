package org.nodystudio.nodybackend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequest {

  @NotBlank(message = "댓글 내용은 필수입니다.")
  @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다.")
  private String content;

  private Long parentId;  // 대댓글인 경우 부모 댓글 ID
}