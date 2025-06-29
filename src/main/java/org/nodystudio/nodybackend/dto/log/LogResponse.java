package org.nodystudio.nodybackend.dto.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogResponse {

    private Long id;
    private UserSummaryResponse author;
    private String content;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private List<String> mediaUrls;
    private Boolean isPublic;
    private Long viewCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static LogResponse from(Log log) {
        return LogResponse.builder()
                .id(log.getId())
                .author(UserSummaryResponse.from(log.getUser()))
                .content(log.getContent())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .address(log.getAddress())
                .mediaUrls(log.getMediaUrls())
                .isPublic(log.getIsPublic())
                .viewCount(log.getViewCount())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}