package org.nodystudio.nodybackend.domain.thread;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.comment.Comment;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.user.User;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "threads", indexes = {
    @Index(name = "idx_threads_user_id", columnList = "user_id"),
    @Index(name = "idx_threads_log_id", columnList = "log_id"),
    @Index(name = "idx_threads_is_public", columnList = "is_public"),
    @Index(name = "idx_threads_created_at", columnList = "created_at"),
    @Index(name = "idx_threads_deactivated_at", columnList = "deactivated_at")
})
public class Thread extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "thread_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @NotNull(message = "작성자는 필수입니다.")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "log_id")
  private Log log;

  @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  @NotNull(message = "내용은 필수입니다.")
  @Size(min = 1, max = 5000, message = "내용은 1자 이상 5000자 이하여야 합니다.")
  private String content;

  @Column(name = "is_public", nullable = false)
  @ColumnDefault("true")
  @Builder.Default
  private Boolean isPublic = true;

  @Column(name = "view_count", nullable = false)
  @ColumnDefault("0")
  @Builder.Default
  private Long viewCount = 0L;

  @OneToMany(mappedBy = "thread", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @Builder.Default
  private List<Comment> comments = new ArrayList<>();

  /**
   * 계정 탈퇴 시 스레드 비활성화 시점을 기록합니다.
   * NULL이면 활성 상태, NULL이 아니면 비활성 상태입니다.
   */
  @Column(name = "deactivated_at")
  private LocalDateTime deactivatedAt;

  /**
   * 스레드 내용을 업데이트합니다.
   */
  public void updateContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new IllegalArgumentException("내용은 공백일 수 없습니다.");
    }
    this.content = content.trim();
  }

  /**
   * 공개 설정을 업데이트합니다.
   */
  public void updatePublicSetting(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  /**
   * 로그에 연결합니다.
   */
  public void connectToLog(Log log) {
    this.log = log;
  }

  /**
   * 로그 연결을 해제합니다.
   */
  public void disconnectFromLog() {
    this.log = null;
  }

  /**
   * 조회수를 증가시킵니다.
   */
  public void incrementViewCount() {
    this.viewCount++;
  }

  /**
   * 현재 사용자가 이 스레드의 작성자인지 확인합니다.
   */
  public boolean isOwnedBy(User user) {
    if (user == null || this.user == null) {
      return false;
    }
    return this.user.getId().equals(user.getId());
  }

  /**
   * 현재 사용자가 이 스레드를 볼 수 있는지 확인합니다. - 공개 스레드는 모든 사용자가 조회 가능 - 비공개 스레드는 작성자만 조회 가능
   */
  public boolean isViewableBy(User viewer) {
    if (this.isPublic) {
      return true;
    }
    return viewer != null && isOwnedBy(viewer);
  }

  /**
   * 로그 연결 스레드인지 확인합니다.
   */
  public boolean isLinkedToLog() {
    return this.log != null;
  }

  /**
   * 독립 스레드인지 확인합니다.
   */
  public boolean isIndependent() {
    return this.log == null;
  }

  /**
   * 연결된 로그의 작성자와 스레드 작성자가 동일한지 확인합니다. 로그가 연결되지 않은 경우 true를 반환합니다.
   */
  public boolean isLogOwnerMatchesThreadOwner() {
    if (this.log == null) {
      return true;
    }
    return this.log.isOwnedBy(this.user);
  }

  /**
   * 연관관계 편의 메서드 - 댓글 추가
   */
  public void addComment(Comment comment) {
    this.comments.add(comment);
    // 양방향 연관관계 설정
    if (comment.getThread() != this) {
      comment.setThread(this);
    }
  }

  /**
   * 연관관계 편의 메서드 - 댓글 제거
   */
  public void removeComment(Comment comment) {
    this.comments.remove(comment);
    // 양방향 연관관계 해제
    if (comment.getThread() == this) {
      comment.setThread(null);
    }
  }

  /**
   * 스레드를 비활성화합니다.
   * 계정 탈퇴 시 사용되며, 비활성화된 스레드는 일반 조회에서 제외됩니다.
   */
  public void deactivate() {
    this.deactivatedAt = LocalDateTime.now();
  }

  /**
   * 스레드를 재활성화합니다.
   * 계정 복구 시 사용되며, 원본 공개설정이 그대로 복원됩니다.
   */
  public void reactivate() {
    this.deactivatedAt = null;
  }

  /**
   * 스레드가 활성 상태인지 확인합니다.
   *
   * @return 활성 상태이면 true, 비활성 상태이면 false
   */
  public boolean isActive() {
    return this.deactivatedAt == null;
  }

  /**
   * 스레드가 비활성 상태인지 확인합니다.
   *
   * @return 비활성 상태이면 true, 활성 상태이면 false
   */
  public boolean isDeactivated() {
    return this.deactivatedAt != null;
  }
}