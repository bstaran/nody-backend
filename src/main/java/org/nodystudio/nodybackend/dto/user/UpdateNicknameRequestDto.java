package org.nodystudio.nodybackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNicknameRequestDto {

  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
  private String nickname;
}