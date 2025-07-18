package org.nodystudio.nodybackend.domain.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.user.User;

/**
 * 댓글 멘션 중간 엔티티
 * 댓글과 멘션된 사용자 간의 관계를 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comment_mentions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_comment_mentions_comment_user",
            columnNames = {"comment_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_comment_mentions_comment_id", columnList = "comment_id"),
        @Index(name = "idx_comment_mentions_user_id", columnList = "user_id"),
        @Index(name = "idx_comment_mentions_notified", columnList = "is_notified"),
        @Index(name = "idx_comment_mentions_created_at", columnList = "created_at")
    })
public class CommentMention extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_mention_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", nullable = false)
  @NotNull(message = "댓글은 필수입니다.")
  private Comment comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @NotNull(message = "멘션된 사용자는 필수입니다.")
  private User mentionedUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "mention_type", nullable = false, length = 20)
  @Builder.Default
  private MentionType mentionType = MentionType.GENERAL;

  @Column(name = "is_notified", nullable = false)
  @ColumnDefault("false")
  @Builder.Default
  private Boolean isNotified = false;

  /**
   * 알림 상태를 읽음으로 변경합니다.
   */
  public void markAsNotified() {
    this.isNotified = true;
  }

  /**
   * 특정 사용자에 대한 멘션인지 확인합니다.
   */
  public boolean isMentionFor(User user) {
    if (user == null || this.mentionedUser == null) {
      return false;
    }
    return this.mentionedUser.getId().equals(user.getId());
  }

  /**
   * 특정 댓글에 대한 멘션인지 확인합니다.
   */
  public boolean isMentionIn(Comment comment) {
    if (comment == null || this.comment == null) {
      return false;
    }
    return this.comment.getId().equals(comment.getId());
  }
}