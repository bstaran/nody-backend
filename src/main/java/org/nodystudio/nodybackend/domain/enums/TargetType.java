package org.nodystudio.nodybackend.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 좋아요 대상 타입을 나타내는 열거형
 */
@Getter
@RequiredArgsConstructor
public enum TargetType {

  THREAD("thread", "스레드"),
  LOG("log", "로그");

  private final String code;
  private final String description;
}