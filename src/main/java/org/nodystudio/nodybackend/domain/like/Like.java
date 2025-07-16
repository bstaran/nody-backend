package org.nodystudio.nodybackend.domain.like;

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
import org.nodystudio.nodybackend.domain.BaseTimeEntity;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.domain.user.User;

/**
 * 좋아요 엔티티
 * 스레드와 로그에 대한 좋아요 정보를 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_likes_user_target",
            columnNames = {"user_id", "target_type", "target_id"})
    },
    indexes = {
        @Index(name = "idx_likes_target_active", columnList = "target_type, target_id, is_active"),
        @Index(name = "idx_likes_user_active", columnList = "user_id, is_active"),
        @Index(name = "idx_likes_created_at", columnList = "created_at")
    })
public class Like extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "like_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @NotNull(message = "사용자는 필수입니다.")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 10)
  @NotNull(message = "대상 타입은 필수입니다.")
  private TargetType targetType;

  @Column(name = "target_id", nullable = false)
  @NotNull(message = "대상 ID는 필수입니다.")
  private Long targetId;

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
   * 특정 대상에 대한 좋아요인지 확인합니다.
   */
  public boolean isTargetMatch(TargetType targetType, Long targetId) {
    return this.targetType == targetType && this.targetId.equals(targetId);
  }
}