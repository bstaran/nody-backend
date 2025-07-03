package org.nodystudio.nodybackend.dto.log;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogCreateRequest {

    @NotBlank(message = "로그 내용은 필수입니다.")
    @Size(max = 2000, message = "로그 내용은 2000자를 초과할 수 없습니다.")
    private String content;

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다.")
    private BigDecimal longitude;

    @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다.")
    private String address;

    @Size(max = 10, message = "미디어 파일은 최대 10개까지 업로드할 수 있습니다.")
    private List<String> mediaUrls;

    @Builder.Default
    private Boolean isPublic = true;
}