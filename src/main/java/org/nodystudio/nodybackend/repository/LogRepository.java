package org.nodystudio.nodybackend.repository;

import org.nodystudio.nodybackend.domain.log.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * - findByLocationNear: 특정 좌표 반경 내 로그 조회 (Haversine 공식)
 * - findByUserIdOrderByCreatedAtDesc: 사용자별 로그 조회
 * - @Query 어노테이션으로 네이티브 쿼리 작성
 */
@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

  /**
   * 사용자별 로그 조회
   */
  List<Log> findByUserIdOrderByCreatedAtDesc(Long userId);

  /**
   * 사용자별 로그 조회 (페이징)
   */
  Page<Log> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * 특정 좌표 반경 내 로그 조회 (Haversine 공식 사용)
   * 
   * @param latitude  중심 위도
   * @param longitude 중심 경도
   * @param radiusKm  반경 (km)
   * @param pageable  페이징 정보
   * @return 반경 내 로그 목록
   */
  @Query(value = """
      SELECT l.*,
             (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) as distance
      FROM logs l
      WHERE l.latitude IS NOT NULL
        AND l.longitude IS NOT NULL
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radiusKm
      ORDER BY distance ASC, l.created_at DESC
      """, countQuery = """
      SELECT COUNT(*)
      FROM logs l
      WHERE l.latitude IS NOT NULL
        AND l.longitude IS NOT NULL
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radiusKm
      """, nativeQuery = true)
  Page<Log> findByLocationNear(@Param("latitude") BigDecimal latitude,
      @Param("longitude") BigDecimal longitude,
      @Param("radiusKm") BigDecimal radiusKm,
      Pageable pageable);

  /**
   * 공개 로그만 위치 기반으로 조회
   */
  @Query(value = """
      SELECT l.*,
             (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) as distance
      FROM logs l
      WHERE l.latitude IS NOT NULL
        AND l.longitude IS NOT NULL
        AND l.is_public = true
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radiusKm
      ORDER BY distance ASC, l.created_at DESC
      """, countQuery = """
      SELECT COUNT(*)
      FROM logs l
      WHERE l.latitude IS NOT NULL
        AND l.longitude IS NOT NULL
        AND l.is_public = true
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radiusKm
      """, nativeQuery = true)
  Page<Log> findPublicLogsByLocationNear(@Param("latitude") BigDecimal latitude,
      @Param("longitude") BigDecimal longitude,
      @Param("radiusKm") BigDecimal radiusKm,
      Pageable pageable);

  /**
   * 특정 사용자의 위치 기반 로그 조회 (본인 비공개 로그 포함)
   */
  @Query(value = """
      SELECT l.*,
             (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) as distance
      FROM logs l
      WHERE l.latitude IS NOT NULL
        AND l.longitude IS NOT NULL
        AND (l.is_public = true OR l.user_id = :userId)
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radiusKm
      ORDER BY distance ASC, l.created_at DESC
      """, countQuery = """
      SELECT COUNT(*)
      FROM logs l
      WHERE l.latitude IS NOT NULL
        AND l.longitude IS NOT NULL
        AND (l.is_public = true OR l.user_id = :userId)
        AND (6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) *
             cos(radians(l.longitude) - radians(:longitude)) +
             sin(radians(:latitude)) * sin(radians(l.latitude)))) <= :radiusKm
      """, nativeQuery = true)
  Page<Log> findLogsByLocationNearWithUser(@Param("latitude") BigDecimal latitude,
      @Param("longitude") BigDecimal longitude,
      @Param("radiusKm") BigDecimal radiusKm,
      @Param("userId") Long userId,
      Pageable pageable);

  /**
   * 로그 ID와 작성자 확인을 위한 조회
   */
  Optional<Log> findByIdAndUserId(Long id, Long userId);

  /**
   * 공개 로그 또는 특정 사용자의 로그 조회
   */
  @Query("SELECT l FROM Log l WHERE l.id = :id AND (l.isPublic = true OR l.user.id = :userId)")
  Optional<Log> findViewableLogByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  /**
   * 공개 로그만 조회
   */
  Optional<Log> findByIdAndIsPublicTrue(Long id);

  /**
   * 공개 로그만 전체 조회 (비로그인 사용자용)
   */
  Page<Log> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

  /**
   * 공개 로그 + 특정 사용자의 비공개 로그 조회 (로그인 사용자용)
   */
  @Query("SELECT l FROM Log l WHERE l.isPublic = true OR l.user.id = :userId ORDER BY l.createdAt DESC")
  Page<Log> findPublicOrUserLogsOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}