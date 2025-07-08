package org.nodystudio.nodybackend.service.thread;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadSearchRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadUpdateRequest;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UnauthorizedException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스레드 관리 서비스
 * 
 * <p>
 * 로그 연결 스레드와 독립 스레드의 생성, 조회, 수정, 삭제 기능을 제공합니다.
 * 사용자 권한에 따라 공개/비공개 스레드에 대한 접근을 제어하며,
 * 로그-스레드 연관관계 검증을 수행합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadService {

  private final ThreadRepository threadRepository;
  private final UserRepository userRepository;
  private final LogRepository logRepository;

  /**
   * 새로운 스레드를 생성합니다.
   * 
   * @param request   스레드 생성 요청 정보
   * @param userEmail 스레드 작성자의 이메일
   * @return 생성된 스레드 정보
   * @throws UserNotFoundException     사용자를 찾을 수 없는 경우
   * @throws ResourceNotFoundException 연결할 로그를 찾을 수 없는 경우
   * @throws UnauthorizedException     로그 연결 권한이 없는 경우
   */
  @Transactional
  public ThreadResponse createThread(ThreadCreateRequest request, String userEmail) {
    log.info("스레드 생성 요청 - 사용자: {}, 로그ID: {}",
        userEmail, request.getLogId());

    User user = findUserByEmail(userEmail);
    Log linkedLog = null;

    // 로그 연결이 요청된 경우 검증
    if (request.getLogId() != null) {
      linkedLog = validateAndGetLinkedLog(request.getLogId(), user);
    }

    Thread thread = Thread.builder()
        .user(user)
        .log(linkedLog)
        .content(request.getContent() != null ? request.getContent().trim() : null)
        .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
        .build();

    Thread savedThread = threadRepository.save(thread);
    log.info("스레드 생성 완료 - ID: {}, 로그 연결: {}",
        savedThread.getId(), savedThread.isLinkedToLog());

    return ThreadResponse.from(savedThread);
  }

  /**
   * 스레드를 조회하고 조회수를 증가시킵니다.
   * 
   * @param threadId  조회할 스레드 ID
   * @param userEmail 조회 요청자의 이메일 (null 가능)
   * @return 스레드 상세 정보
   * @throws ResourceNotFoundException 스레드를 찾을 수 없거나 접근 권한이 없는 경우
   */
  @Transactional
  public ThreadResponse getThread(Long threadId, String userEmail) {
    log.info("스레드 조회 요청 - ID: {}, 사용자: {}", threadId, userEmail);

    User viewer = userEmail != null ? findUserByEmail(userEmail) : null;
    Thread thread = findViewableThread(threadId, viewer);

    threadRepository.incrementViewCount(threadId);

    log.info("스레드 조회 완료 - ID: {}, 조회수 증가됨", thread.getId());
    return ThreadResponse.from(thread);
  }

  /**
   * 스레드 목록을 검색합니다.
   * 
   * @param searchRequest 검색 조건
   * @param userEmail     검색 요청자의 이메일 (null 가능)
   * @return 검색된 스레드 목록 (페이징 처리됨)
   */
  public Page<ThreadResponse> searchThreads(ThreadSearchRequest searchRequest, String userEmail) {
    log.info("스레드 검색 요청 - 키워드: {}, 타입: {}, 로그ID: {}, 사용자: {}",
        searchRequest.getKeyword(), searchRequest.getThreadType(),
        searchRequest.getLogId(), userEmail);

    User viewer = userEmail != null ? findUserByEmail(userEmail) : null;
    Pageable pageable = createPageable(searchRequest);

    Page<Thread> threads = performSearch(searchRequest, viewer, pageable);

    log.info("스레드 검색 완료 - 총 {}건", threads.getTotalElements());
    return threads.map(ThreadResponse::from);
  }

  /**
   * 스레드를 수정합니다.
   * 
   * @param threadId  수정할 스레드 ID
   * @param request   수정할 정보
   * @param userEmail 수정 요청자의 이메일
   * @return 수정된 스레드 정보
   * @throws ResourceNotFoundException 스레드를 찾을 수 없는 경우
   * @throws UnauthorizedException     수정 권한이 없는 경우
   */
  @Transactional
  public ThreadResponse updateThread(Long threadId, ThreadUpdateRequest request, String userEmail) {
    log.info("스레드 수정 요청 - ID: {}, 사용자: {}", threadId, userEmail);

    User user = findUserByEmail(userEmail);
    Thread thread = findUserOwnedThread(threadId, user.getId());

    updateThreadFields(thread, request, user);

    log.info("스레드 수정 완료 - ID: {}", thread.getId());
    return ThreadResponse.from(thread);
  }

  /**
   * 스레드를 삭제합니다.
   * 
   * @param threadId  삭제할 스레드 ID
   * @param userEmail 삭제 요청자의 이메일
   * @throws ResourceNotFoundException 스레드를 찾을 수 없는 경우
   * @throws UnauthorizedException     삭제 권한이 없는 경우
   */
  @Transactional
  public void deleteThread(Long threadId, String userEmail) {
    log.info("스레드 삭제 요청 - ID: {}, 사용자: {}", threadId, userEmail);

    User user = findUserByEmail(userEmail);
    Thread thread = findUserOwnedThread(threadId, user.getId());

    threadRepository.delete(thread);
    log.info("스레드 삭제 완료 - ID: {}", threadId);
  }

  /**
   * 특정 로그의 스레드 목록을 조회합니다.
   * 
   * @param logId     로그 ID
   * @param userEmail 조회 요청자의 이메일 (null 가능)
   * @param pageable  페이징 정보
   * @return 로그의 스레드 목록
   */
  public Page<ThreadResponse> getThreadsByLog(Long logId, String userEmail, Pageable pageable) {
    log.info("로그 스레드 목록 조회 - 로그ID: {}, 사용자: {}", logId, userEmail);

    // 로그 존재 여부 확인
    if (!logRepository.existsById(logId)) {
      throw new ResourceNotFoundException("로그를 찾을 수 없습니다: " + logId);
    }

    User viewer = userEmail != null ? findUserByEmail(userEmail) : null;

    Page<Thread> threads = viewer != null
        ? threadRepository.findThreadsByLogIdWithUser(logId, viewer.getId(), pageable)
        : threadRepository.findPublicThreadsByLogIdOrderByCreatedAtDesc(logId, pageable);

    return threads.map(ThreadResponse::from);
  }

  // === Private Helper Methods ===

  private User findUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
  }

  private Thread findViewableThread(Long threadId, User viewer) {
    if (viewer != null) {
      return threadRepository.findViewableThreadByIdAndUserId(threadId, viewer.getId())
          .orElseThrow(() -> new ResourceNotFoundException("스레드를 찾을 수 없습니다."));
    } else {
      return threadRepository.findByIdAndIsPublicTrue(threadId)
          .orElseThrow(() -> new ResourceNotFoundException("스레드를 찾을 수 없습니다."));
    }
  }

  private Thread findUserOwnedThread(Long threadId, Long userId) {
    return threadRepository.findByIdAndUserId(threadId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("수정 권한이 없거나 스레드를 찾을 수 없습니다."));
  }

  private Log validateAndGetLinkedLog(Long logId, User user) {
    Log log = logRepository.findById(logId)
        .orElseThrow(() -> new ResourceNotFoundException("연결할 로그를 찾을 수 없습니다: " + logId));

    // 공개 로그인 경우: 모든 사용자가 스레드 생성 가능
    if (log.getIsPublic()) {
      return log;
    }

    // 비공개 로그인 경우: 로그 작성자만 스레드 생성 가능
    if (!log.isOwnedBy(user)) {
      throw new UnauthorizedException("비공개 로그에는 작성자만 스레드를 생성할 수 있습니다.");
    }

    return log;
  }

  private Pageable createPageable(ThreadSearchRequest searchRequest) {
    Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
    return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
  }

  private Sort createSort(String sortBy, String sortDirection) {
    Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;

    return switch (sortBy) {
      case "createdAt" -> Sort.by(direction, "createdAt");
      case "viewCount" -> Sort.by(direction, "viewCount");
      default -> Sort.by(Sort.Direction.DESC, "createdAt");
    };
  }

  private Page<Thread> performSearch(ThreadSearchRequest searchRequest, User viewer, Pageable pageable) {
    // 키워드 검색이 있는 경우
    if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
      return viewer != null
          ? threadRepository.searchThreadsByContentWithUser(searchRequest.getKeyword(), viewer.getId(), pageable)
          : threadRepository.searchPublicThreadsByContent(searchRequest.getKeyword(), pageable);
    }

    // 특정 로그의 스레드 검색
    if (searchRequest.getLogId() != null) {
      return viewer != null
          ? threadRepository.findThreadsByLogIdWithUser(searchRequest.getLogId(), viewer.getId(), pageable)
          : threadRepository.findPublicThreadsByLogIdOrderByCreatedAtDesc(searchRequest.getLogId(), pageable);
    }

    // 스레드 타입별 검색
    return switch (searchRequest.getThreadType()) {
      case "independent" -> viewer != null
          ? threadRepository.findIndependentThreadsWithUser(viewer.getId(), pageable)
          : threadRepository.findIndependentPublicThreadsOrderByCreatedAtDesc(pageable);
      case "linked" -> viewer != null
          ? threadRepository.findLinkedThreadsWithUser(viewer.getId(), pageable)
          : threadRepository.findLinkedPublicThreadsOrderByCreatedAtDesc(pageable);
      default -> viewer != null
          ? threadRepository.findPublicOrUserThreadsOrderByCreatedAtDesc(viewer.getId(), pageable)
          : threadRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);
    };
  }

  private void updateThreadFields(Thread thread, ThreadUpdateRequest request, User user) {
    if (request.getContent() != null) {
      thread.updateContent(request.getContent());
    }

    if (request.getIsPublic() != null) {
      thread.updatePublicSetting(request.getIsPublic());
    }

    // 로그 연결 처리
    if (Boolean.TRUE.equals(request.getDisconnectLog())) {
      // 로그 연결 해제 - 독립 스레드로 변경
      thread.disconnectFromLog();
    } else if (request.getLogId() != null) {
      // 새로운 로그로 연결
      Log newLog = validateAndGetLinkedLog(request.getLogId(), user);
      thread.connectToLog(newLog);
    }
  }
}