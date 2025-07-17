package org.nodystudio.nodybackend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 댓글 멘션 이벤트 리스너
 *
 * <p>
 * {@link CommentMentionEvent}를 수신하여 멘션된 사용자들에게 알림을 전송합니다.
 * 비동기 처리를 통해 성능 저하 없이 알림을 처리합니다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentMentionEventListener {

  /**
   * 댓글 멘션 이벤트를 처리합니다.
   * 
   * <p>
   * 현재는 로깅만 수행하며, 향후 실제 알림 시스템(푸시 알림, 이메일 등)과 
   * 연동할 수 있습니다.
   * </p>
   *
   * @param event 댓글 멘션 이벤트
   */
  @Async
  @EventListener
  public void handleCommentMention(CommentMentionEvent event) {
    log.info("댓글 멘션 이벤트 처리 시작 - 댓글: {}, 작성자: {}, 스레드: {}, 멘션된 사용자: {}명",
        event.getCommentId(), event.getAuthorId(), event.getThreadId(), 
        event.getMentionedUserIds().size());

    try {
      // 멘션된 각 사용자에게 알림 처리
      for (Long mentionedUserId : event.getMentionedUserIds()) {
        processUserMention(event, mentionedUserId);
      }
      
      log.info("댓글 멘션 이벤트 처리 완료 - 댓글: {}", event.getCommentId());
      
    } catch (Exception e) {
      log.error("댓글 멘션 이벤트 처리 중 오류 발생 - 댓글: {}", event.getCommentId(), e);
      // 에러가 발생해도 댓글 생성/수정은 성공적으로 완료되어야 함
      // 알림 실패는 별도로 모니터링하거나 재시도 로직 구현 가능
    }
  }

  /**
   * 개별 사용자 멘션을 처리합니다.
   *
   * @param event 댓글 멘션 이벤트
   * @param mentionedUserId 멘션된 사용자 ID
   */
  private void processUserMention(CommentMentionEvent event, Long mentionedUserId) {
    log.debug("사용자 멘션 처리 - 사용자: {}, 댓글: {}", mentionedUserId, event.getCommentId());
    
    // TODO: 실제 알림 시스템 연동
    // 1. 인앱 알림 생성
    // 2. 푸시 알림 전송 (선택사항)
    // 3. 이메일 알림 전송 (선택사항)
    // 4. 알림 히스토리 저장
    
    // 현재는 로깅으로 대체
    log.info("멘션 알림 처리됨 - 사용자: {}, 댓글: {}, 작성자: {}", 
        mentionedUserId, event.getCommentId(), event.getAuthorId());
  }
}