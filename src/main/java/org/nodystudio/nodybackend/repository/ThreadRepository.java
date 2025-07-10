package org.nodystudio.nodybackend.repository;

import java.util.Optional;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {

  /**
   * 사용자가 소유한 스레드를 조회합니다.
   */
  Optional<Thread> findByIdAndUserId(Long id, Long userId);

  /**
   * 공개 스레드를 ID로 조회합니다.
   */
  Optional<Thread> findByIdAndIsPublicTrue(Long id);

  /**
   * 사용자가 볼 수 있는 스레드를 조회합니다 (공개 스레드 + 본인 스레드).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.id = :threadId
            AND (t.isPublic = true OR t.user.id = :userId)
      """)
  Optional<Thread> findViewableThreadByIdAndUserId(@Param("threadId") Long threadId,
      @Param("userId") Long userId);

  /**
   * 공개 스레드 목록을 최신순으로 조회합니다.
   */
  Page<Thread> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);

  /**
   * 사용자가 볼 수 있는 스레드 목록을 조회합니다 (공개 스레드 + 본인 스레드).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.isPublic = true OR t.user.id = :userId
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findPublicOrUserThreadsOrderByCreatedAtDesc(@Param("userId") Long userId,
      Pageable pageable);

  /**
   * 특정 로그에 연결된 스레드 목록을 조회합니다 (공개 스레드만).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.log.id = :logId AND t.isPublic = true
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findPublicThreadsByLogIdOrderByCreatedAtDesc(@Param("logId") Long logId,
      Pageable pageable);

  /**
   * 특정 로그에 연결된 스레드 목록을 조회합니다 (사용자별 권한 고려).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.log.id = :logId AND (t.isPublic = true OR t.user.id = :userId)
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findThreadsByLogIdWithUser(@Param("logId") Long logId,
      @Param("userId") Long userId,
      Pageable pageable);

  /**
   * 특정 사용자가 작성한 스레드 목록을 조회합니다.
   */
  Page<Thread> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * 독립 스레드 목록을 조회합니다 (로그에 연결되지 않은 스레드, 공개만).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.log IS NULL AND t.isPublic = true
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findIndependentPublicThreadsOrderByCreatedAtDesc(Pageable pageable);

  /**
   * 독립 스레드 목록을 조회합니다 (사용자별 권한 고려).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.log IS NULL AND (t.isPublic = true OR t.user.id = :userId)
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findIndependentThreadsWithUser(@Param("userId") Long userId,
      Pageable pageable);

  /**
   * 로그 연결 스레드 목록을 조회합니다 (로그에 연결된 스레드, 공개만).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.log IS NOT NULL AND t.isPublic = true
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findLinkedPublicThreadsOrderByCreatedAtDesc(Pageable pageable);

  /**
   * 로그 연결 스레드 목록을 조회합니다 (사용자별 권한 고려).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.log IS NOT NULL AND (t.isPublic = true OR t.user.id = :userId)
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> findLinkedThreadsWithUser(@Param("userId") Long userId,
      Pageable pageable);

  /**
   * 내용으로 공개 스레드를 검색합니다.
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE t.isPublic = true AND LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> searchPublicThreadsByContent(@Param("keyword") String keyword,
      Pageable pageable);

  /**
   * 내용으로 스레드를 검색합니다 (사용자별 권한 고려).
   */
  @Query("""
          SELECT t FROM Thread t
          WHERE (t.isPublic = true OR t.user.id = :userId) AND
          LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ORDER BY t.createdAt DESC
      """)
  Page<Thread> searchThreadsByContentWithUser(@Param("keyword") String keyword,
      @Param("userId") Long userId,
      Pageable pageable);

  /**
   * 특정 로그에 연결된 스레드 개수를 조회합니다.
   */
  long countByLogId(Long logId);

  /**
   * 특정 사용자가 작성한 스레드 개수를 조회합니다.
   */
  long countByUserId(Long userId);

  /**
   * 스레드의 조회수를 원자적으로 증가시킵니다. 동시성 이슈를 해결하기 위해 데이터베이스 레벨에서 원자적 증가를 수행합니다.
   */
  @Modifying
  @Query("UPDATE Thread t SET t.viewCount = t.viewCount + 1 WHERE t.id = :threadId")
  int incrementViewCount(@Param("threadId") Long threadId);
}