package org.nodystudio.nodybackend.dto.thread;

import java.time.LocalDateTime;

import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;

import lombok.Builder;
import lombok.Getter;

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
        .build();
  }
}