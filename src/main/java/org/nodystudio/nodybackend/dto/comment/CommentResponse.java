package org.nodystudio.nodybackend.dto.comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import org.nodystudio.nodybackend.domain.comment.Comment;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;

@Getter
@Builder
public class CommentResponse {

  private Long id;
  private String content;
  private UserSummaryResponse author;
  private List<UserSummaryResponse> mentionedUsers;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long parentId;
  @Builder.Default
  private List<CommentResponse> children = new ArrayList<>();
  private Boolean isDeleted;

  /**
   * Comment 엔티티를 CommentResponse DTO로 변환합니다. 주의: children은 서비스 레이어에서 별도로 채워야 합니다 (N+1 쿼리 방지)
   */
  public static CommentResponse from(Comment comment) {
    // 삭제된 댓글 처리
    if (comment.isDeleted()) {
      return CommentResponse.builder()
          .id(comment.getId())
          .content("[삭제된 댓글입니다.]")
          .author(null)  // 프라이버시를 위해 작성자 정보 제거
          .mentionedUsers(Collections.emptyList())
          .createdAt(comment.getCreatedAt())
          .updatedAt(comment.getUpdatedAt())
          .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
          .children(new ArrayList<>())  // 서비스 레이어에서 채움
          .isDeleted(true)
          .build();
    }

    return CommentResponse.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .author(UserSummaryResponse.from(comment.getAuthor()))
        .mentionedUsers(comment.getMentionedUsers().stream()
            .map(UserSummaryResponse::from)
            .collect(Collectors.toList()))
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
        .children(new ArrayList<>())  // 서비스 레이어에서 채움
        .isDeleted(false)
        .build();
  }

  /**
   * 자식 댓글을 추가합니다. 서비스 레이어에서 계층 구조를 구성할 때 사용합니다.
   */
  public void addChild(CommentResponse child) {
    this.children.add(child);
  }
}