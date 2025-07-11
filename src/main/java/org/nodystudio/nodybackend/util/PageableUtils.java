package org.nodystudio.nodybackend.util;

import org.nodystudio.nodybackend.domain.enums.SortDirection;
import org.nodystudio.nodybackend.domain.enums.ThreadSortField;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 페이지네이션 관련 유틸리티 클래스
 */
public final class PageableUtils {

  private PageableUtils() {
    // 유틸리티 클래스는 인스턴스 생성 방지
  }

  /**
   * 기본 페이지네이션 객체를 생성합니다.
   *
   * @param page          페이지 번호 (0부터 시작)
   * @param size          페이지 크기
   * @param sortBy        정렬 필드
   * @param sortDirection 정렬 방향 ("asc" 또는 "desc")
   * @return Pageable 객체
   */
  public static Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
    Sort sort = createSort(sortBy, sortDirection);
    return PageRequest.of(page, size, sort);
  }

  /**
   * Thread 엔티티에 최적화된 페이지네이션 객체를 생성합니다.
   *
   * @param page          페이지 번호 (0부터 시작)
   * @param size          페이지 크기
   * @param sortBy        정렬 필드
   * @param sortDirection 정렬 방향 (ASC 또는 DESC)
   * @return Pageable 객체
   */
  public static Pageable createThreadPageable(int page, int size, ThreadSortField sortBy,
      SortDirection sortDirection) {
    Sort sort = createThreadSort(sortBy, sortDirection);
    return PageRequest.of(page, size, sort);
  }

  /**
   * 정렬 객체를 생성합니다.
   *
   * @param sortBy        정렬 필드
   * @param sortDirection 정렬 방향 ("asc" 또는 "desc")
   * @return Sort 객체
   */
  public static Sort createSort(String sortBy, String sortDirection) {
    Sort.Direction direction = getSortDirection(sortDirection);
    return Sort.by(direction, sortBy);
  }

  /**
   * Thread 엔티티에 최적화된 정렬 객체를 생성합니다. 유효하지 않은 정렬 필드의 경우 기본값(createdAt DESC)을 사용합니다.
   *
   * @param sortBy        정렬 필드
   * @param sortDirection 정렬 방향 (ASC 또는 DESC)
   * @return Sort 객체
   */
  public static Sort createThreadSort(ThreadSortField sortBy, SortDirection sortDirection) {
    Sort.Direction direction = sortDirection == SortDirection.ASC
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;

    return switch (sortBy) {
      case CREATED_AT -> Sort.by(direction, "createdAt");
      case VIEW_COUNT -> Sort.by(direction, "viewCount");
      default -> Sort.by(Sort.Direction.DESC, "createdAt"); // 기본값
    };
  }

  /**
   * 문자열을 Sort.Direction으로 변환합니다.
   *
   * @param sortDirection 정렬 방향 ("asc" 또는 "desc")
   * @return Sort.Direction
   */
  public static Sort.Direction getSortDirection(String sortDirection) {
    return "asc".equalsIgnoreCase(sortDirection)
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;
  }
}