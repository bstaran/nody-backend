package org.nodystudio.nodybackend.repository;

import java.util.List;
import java.util.Optional;
import org.nodystudio.nodybackend.domain.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  /**
   * 특정 스레드의 모든 댓글을 생성 시간 오름차순으로 조회합니다. 활성 댓글만 조회됩니다.
   */
  @Query("""
          SELECT c FROM Comment c 
          LEFT JOIN FETCH c.author 
          LEFT JOIN FETCH c.mentionedUsers 
          WHERE c.thread.id = :threadId AND c.deletedAt IS NULL
          ORDER BY c.createdAt ASC
      """)
  List<Comment> findByThreadIdWithAuthorAndMentions(@Param("threadId") Long threadId);

  /**
   * 특정 스레드의 댓글을 페이징하여 조회합니다.
   */
  @Query("""
          SELECT c FROM Comment c 
          LEFT JOIN FETCH c.author 
          WHERE c.thread.id = :threadId AND c.deletedAt IS NULL
          ORDER BY c.createdAt ASC
      """)
  Page<Comment> findByThreadIdWithAuthor(@Param("threadId") Long threadId, Pageable pageable);

  /**
   * 특정 사용자가 작성한 댓글을 조회합니다.
   */
  Page<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

  /**
   * 특정 사용자가 멘션된 댓글을 조회합니다.
   */
  @Query("""
          SELECT c FROM Comment c 
          JOIN c.mentionedUsers m 
          WHERE m.id = :userId AND c.deletedAt IS NULL
          ORDER BY c.createdAt DESC
      """)
  Page<Comment> findByMentionedUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * ID와 작성자로 댓글을 조회합니다 (수정/삭제 권한 확인용).
   */
  Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);

  /**
   * 특정 스레드의 댓글 개수를 조회합니다. 삭제된 댓글은 제외됩니다.
   */
  @Query("SELECT COUNT(c) FROM Comment c WHERE c.thread.id = :threadId AND c.deletedAt IS NULL")
  long countActiveByThreadId(@Param("threadId") Long threadId);

  /**
   * 특정 스레드의 모든 댓글 개수를 조회합니다 (삭제된 댓글 포함).
   */
  @Query(value = "SELECT COUNT(*) FROM comments WHERE thread_id = :threadId", nativeQuery = true)
  long countByThreadId(@Param("threadId") Long threadId);

  /**
   * 특정 사용자가 작성한 활성 댓글 개수를 조회합니다.
   */
  @Query("SELECT COUNT(c) FROM Comment c WHERE c.author.id = :authorId AND c.deletedAt IS NULL")
  long countActiveByAuthorId(@Param("authorId") Long authorId);

  /**
   * 최상위 댓글만 조회합니다 (대댓글 제외).
   */
  @Query("""
          SELECT c FROM Comment c 
          LEFT JOIN FETCH c.author 
          WHERE c.thread.id = :threadId AND c.parent IS NULL AND c.deletedAt IS NULL
          ORDER BY c.createdAt ASC
      """)
  List<Comment> findRootCommentsByThreadId(@Param("threadId") Long threadId);

  /**
   * 특정 댓글의 자식 댓글들을 조회합니다.
   */
  @Query("""
          SELECT c FROM Comment c 
          LEFT JOIN FETCH c.author 
          WHERE c.parent.id = :parentId AND c.deletedAt IS NULL
          ORDER BY c.createdAt ASC
      """)
  List<Comment> findChildCommentsByParentId(@Param("parentId") Long parentId);

  /**
   * 특정 스레드에서 특정 사용자가 작성한 댓글을 조회합니다.
   */
  @Query("""
          SELECT c FROM Comment c 
          WHERE c.thread.id = :threadId AND c.author.id = :authorId AND c.deletedAt IS NULL
          ORDER BY c.createdAt DESC
      """)
  List<Comment> findByThreadIdAndAuthorId(@Param("threadId") Long threadId,
      @Param("authorId") Long authorId);

  /**
   * 사용자별 댓글을 비활성화합니다 (계정 탈퇴 시).
   * Soft delete로 처리하여 데이터는 보존하되 공개되지 않도록 합니다.
   */
  @Modifying
  @Query("UPDATE Comment c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.author.id = :userId AND c.deletedAt IS NULL")
  int deactivateByUserId(@Param("userId") Long userId);

  /**
   * 사용자별 댓글을 재활성화합니다 (계정 복구 시).
   * Soft delete 상태를 해제하여 댓글을 다시 공개합니다.
   */
  @Modifying
  @Query("UPDATE Comment c SET c.deletedAt = NULL WHERE c.author.id = :userId AND c.deletedAt IS NOT NULL")
  int reactivateByUserId(@Param("userId") Long userId);
}