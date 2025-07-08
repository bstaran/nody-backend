package org.nodystudio.nodybackend.domain.thread;

import org.hibernate.annotations.ColumnDefault;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "threads", indexes = {
    @Index(name = "idx_threads_user_id", columnList = "user_id"),
    @Index(name = "idx_threads_log_id", columnList = "log_id"),
    @Index(name = "idx_threads_is_public", columnList = "is_public"),
    @Index(name = "idx_threads_created_at", columnList = "created_at")
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
   * 로그 연결을 업데이트합니다.
   */
  public void updateLog(Log log) {
    this.log = log;
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
   * 현재 사용자가 이 스레드를 볼 수 있는지 확인합니다.
   * - 공개 스레드는 모든 사용자가 조회 가능
   * - 비공개 스레드는 작성자만 조회 가능
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
   * 연결된 로그의 작성자와 스레드 작성자가 동일한지 확인합니다.
   * 로그가 연결되지 않은 경우 true를 반환합니다.
   */
  public boolean isLogOwnerMatchesThreadOwner() {
    if (this.log == null) {
      return true;
    }
    return this.log.isOwnedBy(this.user);
  }
}