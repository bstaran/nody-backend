package org.nodystudio.nodybackend.dto.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;

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

  private Long likeCount;
  private Boolean isLiked;

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
        .likeCount(0L)
        .isLiked(false)
        .build();
  }

  /**
   * 좋아요 정보를 설정한 LogResponse를 반환합니다.
   *
   * @param likeCount 좋아요 개수
   * @param isLiked   사용자의 좋아요 여부
   * @return 좋아요 정보가 설정된 LogResponse
   */
  public LogResponse withLikeInfo(Long likeCount, Boolean isLiked) {
    return LogResponse.builder()
        .id(this.id)
        .author(this.author)
        .content(this.content)
        .latitude(this.latitude)
        .longitude(this.longitude)
        .address(this.address)
        .mediaUrls(this.mediaUrls)
        .isPublic(this.isPublic)
        .viewCount(this.viewCount)
        .createdAt(this.createdAt)
        .updatedAt(this.updatedAt)
        .likeCount(likeCount)
        .isLiked(isLiked)
        .build();
  }
}