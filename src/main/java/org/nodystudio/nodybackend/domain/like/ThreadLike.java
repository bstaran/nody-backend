package org.nodystudio.nodybackend.domain.like;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;

/**
 * 스레드 좋아요 중간 엔티티
 * 스레드와 사용자 간의 좋아요 관계를 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "thread_likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_thread_likes_user_thread", columnNames = {"user_id",
            "thread_id"})
    },
    indexes = {
        @Index(name = "idx_thread_likes_thread_active", columnList = "thread_id, is_active"),
        @Index(name = "idx_thread_likes_user_active", columnList = "user_id, is_active"),
        @Index(name = "idx_thread_likes_created_at", columnList = "created_at")
    })
public class ThreadLike extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "thread_like_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @NotNull(message = "사용자는 필수입니다.")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thread_id", nullable = false)
  @NotNull(message = "스레드는 필수입니다.")
  private Thread thread;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  /**
   * 좋아요를 활성화합니다.
   */
  public void activate() {
    this.isActive = true;
  }

  /**
   * 좋아요를 비활성화합니다.
   */
  public void deactivate() {
    this.isActive = false;
  }

  /**
   * 좋아요 상태를 토글합니다.
   */
  public void toggle() {
    this.isActive = !this.isActive;
  }

  /**
   * 현재 사용자가 이 좋아요를 누른 사용자인지 확인합니다.
   */
  public boolean isOwnedBy(User user) {
    if (user == null || this.user == null) {
      return false;
    }
    return this.user.getId().equals(user.getId());
  }

  /**
   * 특정 스레드에 대한 좋아요인지 확인합니다.
   */
  public boolean isForThread(Thread thread) {
    if (thread == null || this.thread == null) {
      return false;
    }
    return this.thread.getId().equals(thread.getId());
  }
}