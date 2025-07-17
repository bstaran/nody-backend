package org.nodystudio.nodybackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 좋아요 데이터 접근을 위한 Repository
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

  /**
   * 원자적 좋아요 토글 연산을 수행합니다.
   *
   * <p>
   * 존재하지 않으면 새로운 활성 좋아요를 생성하고, 존재하면 상태를 토글합니다.
   * MySQL의 ON DUPLICATE KEY UPDATE 문법을 사용하여 동시성 안전성을 보장합니다.
   * </p>
   *
   * <p>
   * <strong>개선 사항:</strong> 기존 "NOT is_active" 대신 "IF(is_active = 1, 0, 1)"을 사용하여
   * MySQL 버전별 호환성과 NULL 값 처리 안정성을 개선했습니다.
   * </p>
   *
   * <p>
   * <strong>주의:</strong> 이 메서드는 MySQL 전용입니다. H2 테스트 환경에서는 지원되지 않습니다.
   * </p>
   *
   * @param userId     사용자 ID
   * @param targetType 대상 타입 (THREAD, LOG)
   * @param targetId   대상 ID
   * @return 영향받은 행 수 (항상 1)
   */
  @Modifying
  @Query(value = """
      INSERT INTO likes (user_id, target_type, target_id, is_active, created_at, updated_at)
      VALUES (:userId, :targetType, :targetId, 1, NOW(), NOW())
      ON DUPLICATE KEY UPDATE
          is_active = IF(is_active = 1, 0, 1),
          updated_at = NOW()
      """, nativeQuery = true)
  int atomicToggleLike(@Param("userId") Long userId,
      @Param("targetType") String targetType,
      @Param("targetId") Long targetId);

  /**
   * 특정 사용자가 특정 대상에 활성 좋아요를 눌렀는지 확인합니다. (비관적 락 적용)
   *
   * @param userId     사용자 ID
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @return 좋아요 엔티티 (존재하지 않으면 empty)
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.targetType = :targetType AND l.targetId = :targetId")
  Optional<Like> findByUserIdAndTargetTypeAndTargetId(@Param("userId") Long userId,
      @Param("targetType") TargetType targetType,
      @Param("targetId") Long targetId);

  /**
   * 특정 대상의 활성 좋아요 개수를 조회합니다.
   *
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @return 활성 좋아요 개수
   */
  @Query("SELECT COUNT(l) FROM Like l WHERE l.targetType = :targetType AND l.targetId = :targetId AND l.isActive = true")
  long countByTargetTypeAndTargetIdAndIsActiveTrue(@Param("targetType") TargetType targetType,
      @Param("targetId") Long targetId);

  /**
   * 레거시 호환성을 위한 전체 좋아요 개수 조회 (deprecated)
   */
  @Deprecated
  long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

  /**
   * 특정 사용자가 특정 대상에 활성 좋아요를 눌렀는지 확인합니다.
   *
   * @param userId     사용자 ID
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @return 활성 좋아요 존재 여부
   */
  @Query("SELECT COUNT(l) > 0 FROM Like l WHERE l.user.id = :userId AND l.targetType = :targetType AND l.targetId = :targetId AND l.isActive = true")
  boolean existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(@Param("userId") Long userId,
      @Param("targetType") TargetType targetType,
      @Param("targetId") Long targetId);

  /**
   * 특정 사용자의 좋아요를 삭제합니다. (물리적 삭제)
   * 테스트 용도로만 사용해야 합니다.
   *
   * @param userId     사용자 ID
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   */
  void deleteByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);
}