package org.nodystudio.nodybackend.event;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 댓글 멘션 이벤트
 *
 * <p>
 * 댓글에서 사용자가 멘션되었을 때 발행되는 이벤트입니다. 비동기 이벤트 리스너에서 이를 처리하여 멘션된 사용자들에게 알림을 전송합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public class CommentMentionEvent {

  private final Long commentId;
  private final Long authorId;
  private final Long threadId;
  private final Set<Long> mentionedUserIds;
}