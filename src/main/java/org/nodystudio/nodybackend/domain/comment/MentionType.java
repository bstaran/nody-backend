package org.nodystudio.nodybackend.domain.comment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 멘션 타입 열거형
 * 댓글에서 사용자를 멘션하는 방식을 구분합니다.
 */
@Getter
@RequiredArgsConstructor
public enum MentionType {

  /**
   * 일반 멘션 (@username 형태)
   */
  GENERAL("일반 멘션"),

  /**
   * 답글 멘션 (대댓글에서 상위 댓글 작성자 멘션)
   */
  REPLY("답글 멘션");

  private final String description;
}