package org.nodystudio.nodybackend.dto.thread;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;

@Getter
@Builder
public class ThreadResponse {

  private Long id;
  private String content;
  private Boolean isPublic;
  private Long viewCount;
  private UserSummaryResponse user;
  private LogResponse log;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean isLinkedToLog;
  private Boolean isIndependent;
  private Long likeCount;
  private Boolean isLiked;

  public static ThreadResponse from(Thread thread) {
    return ThreadResponse.builder()
        .id(thread.getId())
        .content(thread.getContent())
        .isPublic(thread.getIsPublic())
        .viewCount(thread.getViewCount())
        .user(UserSummaryResponse.from(thread.getUser()))
        .log(thread.getLog() != null ? LogResponse.from(thread.getLog()) : null)
        .createdAt(thread.getCreatedAt())
        .updatedAt(thread.getUpdatedAt())
        .isLinkedToLog(thread.isLinkedToLog())
        .isIndependent(thread.isIndependent())
        .likeCount(0L)
        .isLiked(false)
        .build();
  }

  public static ThreadResponse fromWithoutLog(Thread thread) {
    return ThreadResponse.builder()
        .id(thread.getId())
        .content(thread.getContent())
        .isPublic(thread.getIsPublic())
        .viewCount(thread.getViewCount())
        .user(UserSummaryResponse.from(thread.getUser()))
        .log(null) // 로그 정보 제외
        .createdAt(thread.getCreatedAt())
        .updatedAt(thread.getUpdatedAt())
        .isLinkedToLog(thread.isLinkedToLog())
        .isIndependent(thread.isIndependent())
        .likeCount(0L)
        .isLiked(false)
        .build();
  }

  /**
   * 좋아요 정보를 설정한 ThreadResponse를 반환합니다.
   *
   * @param likeCount 좋아요 개수
   * @param isLiked   사용자의 좋아요 여부
   * @return 좋아요 정보가 설정된 ThreadResponse
   */
  public ThreadResponse withLikeInfo(Long likeCount, Boolean isLiked) {
    return ThreadResponse.builder()
        .id(this.id)
        .content(this.content)
        .isPublic(this.isPublic)
        .viewCount(this.viewCount)
        .user(this.user)
        .log(this.log)
        .createdAt(this.createdAt)
        .updatedAt(this.updatedAt)
        .isLinkedToLog(this.isLinkedToLog)
        .isIndependent(this.isIndependent)
        .likeCount(likeCount)
        .isLiked(isLiked)
        .build();
  }
}