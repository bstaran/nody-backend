package org.nodystudio.nodybackend.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.SortDirection;
import org.nodystudio.nodybackend.domain.enums.ThreadSortField;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DisplayName("PageableUtils 테스트")
class PageableUtilsTest {

  @Test
  @DisplayName("기본 페이지네이션 객체 생성 - ASC 정렬")
  void createPageable_AscSort_Success() {
    // when
    Pageable pageable = PageableUtils.createPageable(0, 10, "name", "asc");

    // then
    assertThat(pageable.getPageNumber()).isEqualTo(0);
    assertThat(pageable.getPageSize()).isEqualTo(10);
    Sort.Order order = pageable.getSort().getOrderFor("name");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("기본 페이지네이션 객체 생성 - DESC 정렬")
  void createPageable_DescSort_Success() {
    // when
    Pageable pageable = PageableUtils.createPageable(1, 20, "createdAt", "desc");

    // then
    assertThat(pageable.getPageNumber()).isEqualTo(1);
    assertThat(pageable.getPageSize()).isEqualTo(20);
    Sort.Order order = pageable.getSort().getOrderFor("createdAt");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("Thread 페이지네이션 객체 생성 - 유효한 필드")
  void createThreadPageable_ValidFields_Success() {
    // when
    Pageable pageable = PageableUtils.createThreadPageable(0, 10, ThreadSortField.VIEW_COUNT,
        SortDirection.ASC);

    // then
    assertThat(pageable.getPageNumber()).isEqualTo(0);
    assertThat(pageable.getPageSize()).isEqualTo(10);
    Sort.Order order = pageable.getSort().getOrderFor("viewCount");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("Thread 페이지네이션 객체 생성 - enum 사용으로 유효하지 않은 필드 입력 방지")
  void createThreadPageable_EnumPreventInvalidField() {
    // enum 사용으로 유효하지 않은 필드 입력이 컴파일 타임에 방지됨
    // 기본값 사용 테스트
    Pageable pageable = PageableUtils.createThreadPageable(0, 10, ThreadSortField.CREATED_AT,
        SortDirection.ASC);

    // then
    assertThat(pageable.getPageNumber()).isEqualTo(0);
    assertThat(pageable.getPageSize()).isEqualTo(10);
    Sort.Order order = pageable.getSort().getOrderFor("createdAt");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("정렬 방향 변환 - asc")
  void getSortDirection_Asc_Success() {
    // when
    Sort.Direction direction = PageableUtils.getSortDirection("asc");

    // then
    assertThat(direction).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("정렬 방향 변환 - ASC (대문자)")
  void getSortDirection_AscUpperCase_Success() {
    // when
    Sort.Direction direction = PageableUtils.getSortDirection("ASC");

    // then
    assertThat(direction).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("정렬 방향 변환 - desc")
  void getSortDirection_Desc_Success() {
    // when
    Sort.Direction direction = PageableUtils.getSortDirection("desc");

    // then
    assertThat(direction).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("정렬 방향 변환 - 기본값은 DESC")
  void getSortDirection_InvalidValue_DefaultDesc() {
    // when
    Sort.Direction direction = PageableUtils.getSortDirection("invalid");

    // then
    assertThat(direction).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("Thread 정렬 객체 생성 - createdAt")
  void createThreadSort_CreatedAt_Success() {
    // when
    Sort sort = PageableUtils.createThreadSort(ThreadSortField.CREATED_AT, SortDirection.ASC);

    // then
    Sort.Order order = sort.getOrderFor("createdAt");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("Thread 정렬 객체 생성 - viewCount")
  void createThreadSort_ViewCount_Success() {
    // when
    Sort sort = PageableUtils.createThreadSort(ThreadSortField.VIEW_COUNT, SortDirection.DESC);

    // then
    Sort.Order order = sort.getOrderFor("viewCount");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("Thread 정렬 객체 생성 - SortDirection.ASC 테스트")
  void createThreadSort_SortDirection_ASC_Success() {
    // when
    Sort sort = PageableUtils.createThreadSort(ThreadSortField.VIEW_COUNT, SortDirection.ASC);

    // then
    Sort.Order order = sort.getOrderFor("viewCount");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("Thread 정렬 객체 생성 - SortDirection.DESC 테스트")
  void createThreadSort_SortDirection_DESC_Success() {
    // when
    Sort sort = PageableUtils.createThreadSort(ThreadSortField.CREATED_AT, SortDirection.DESC);

    // then
    Sort.Order order = sort.getOrderFor("createdAt");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("Thread 정렬 객체 생성 - enum 조합 테스트")
  void createThreadSort_EnumCombination_Success() {
    // when - 모든 enum 조합 테스트
    Sort sort1 = PageableUtils.createThreadSort(ThreadSortField.CREATED_AT, SortDirection.ASC);
    Sort sort2 = PageableUtils.createThreadSort(ThreadSortField.VIEW_COUNT, SortDirection.DESC);

    // then
    Sort.Order order1 = sort1.getOrderFor("createdAt");
    assertThat(order1).isNotNull();
    assertThat(Objects.requireNonNull(order1).getDirection()).isEqualTo(Sort.Direction.ASC);

    Sort.Order order2 = sort2.getOrderFor("viewCount");
    assertThat(order2).isNotNull();
    assertThat(Objects.requireNonNull(order2).getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("기본 정렬 객체 생성")
  void createSort_Basic_Success() {
    // when
    Sort sort = PageableUtils.createSort("name", "asc");

    // then
    Sort.Order order = sort.getOrderFor("name");
    assertThat(order).isNotNull();
    assertThat(Objects.requireNonNull(order).getDirection()).isEqualTo(Sort.Direction.ASC);
  }
}