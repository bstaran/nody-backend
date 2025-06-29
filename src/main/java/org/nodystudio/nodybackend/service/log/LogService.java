package org.nodystudio.nodybackend.service.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.log.LogCreateRequest;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.log.LogSearchRequest;
import org.nodystudio.nodybackend.dto.log.LogUpdateRequest;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.nodystudio.nodybackend.util.LocationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 위치 기반 로그 관리 서비스
 * 
 * <p>
 * 지리적 위치 정보를 활용한 로그 생성, 조회, 검색 기능을 제공합니다.
 * 사용자 권한에 따라 공개/비공개 로그에 대한 접근을 제어하며,
 * Haversine 공식을 사용한 반경 기반 검색을 지원합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

  private final LogRepository logRepository;
  private final UserRepository userRepository;

  /**
   * 새로운 로그를 생성합니다.
   * 
   * @param request   로그 생성 요청 정보
   * @param userEmail 로그 작성자의 이메일
   * @return 생성된 로그 정보
   * @throws IllegalArgumentException 위치 좌표가 유효하지 않은 경우
   * @throws UserNotFoundException    사용자를 찾을 수 없는 경우
   */
  @Transactional
  public LogResponse createLog(LogCreateRequest request, String userEmail) {
    log.info("로그 생성 요청 - 사용자: {}, 위치: ({}, {})",
        userEmail, request.getLatitude(), request.getLongitude());

    User user = findUserByEmail(userEmail);

    if (request.getLatitude() != null && request.getLongitude() != null) {
      LocationUtils.validateCoordinates(request.getLatitude(), request.getLongitude());
    }

    Log logEntity = Log.builder()
        .user(user)
        .content(request.getContent())
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .address(request.getAddress())
        .mediaUrls(request.getMediaUrls())
        .isPublic(request.getIsPublic())
        .build();

    Log savedLog = logRepository.save(logEntity);
    log.info("로그 생성 완료 - ID: {}", savedLog.getId());

    return LogResponse.from(savedLog);
  }

  /**
   * 로그를 조회하고 조회수를 증가시킵니다.
   * 
   * <p>
   * 사용자 권한에 따라 접근 가능한 로그를 제한합니다:
   * <ul>
   * <li>로그인 사용자: 공개 로그 + 본인의 비공개 로그</li>
   * <li>비로그인 사용자: 공개 로그만</li>
   * </ul>
   * 
   * @param logId     조회할 로그 ID
   * @param userEmail 조회 요청자의 이메일 (null 가능)
   * @return 로그 상세 정보
   * @throws IllegalArgumentException 로그를 찾을 수 없거나 접근 권한이 없는 경우
   */
  @Transactional
  public LogResponse getLog(Long logId, String userEmail) {
    log.info("로그 조회 요청 - ID: {}, 사용자: {}", logId, userEmail);

    User viewer = userEmail != null ? findUserByEmail(userEmail) : null;

    Log logEntity = findViewableLog(logId, viewer);
    logEntity.incrementViewCount();

    log.info("로그 조회 완료 - ID: {}, 조회수: {}", logEntity.getId(), logEntity.getViewCount());
    return LogResponse.from(logEntity);
  }

  /**
   * 위치 기반 로그를 검색합니다.
   * 
   * <p>
   * 위치 정보가 제공된 경우 지정된 반경 내의 로그를 검색하고,
   * 위치 정보가 없는 경우 전체 로그를 조회합니다.
   * 사용자 권한에 따라 공개/비공개 로그 접근을 제어합니다.
   * </p>
   * 
   * @param searchRequest 검색 조건 (위치, 반경, 페이징 정보)
   * @param userEmail     검색 요청자의 이메일 (null 가능)
   * @return 검색된 로그 목록 (페이징 처리됨)
   * @throws IllegalArgumentException 위치 좌표가 유효하지 않은 경우
   */
  public Page<LogResponse> searchLogs(LogSearchRequest searchRequest, String userEmail) {
    log.info("로그 검색 요청 - 위치: ({}, {}), 반경: {}km, 사용자: {}",
        searchRequest.getLatitude(), searchRequest.getLongitude(),
        searchRequest.getRadiusKm(), userEmail);

    User viewer = userEmail != null ? findUserByEmail(userEmail) : null;
    Pageable pageable = createPageable(searchRequest);

    Page<Log> logs = hasLocationInfo(searchRequest)
        ? searchByLocation(searchRequest, viewer, pageable)
        : searchAllLogs(pageable);

    log.info("로그 검색 완료 - 총 {}건", logs.getTotalElements());
    return logs.map(LogResponse::from);
  }

  /**
   * 로그를 수정합니다.
   * 
   * <p>
   * 로그 작성자만 수정할 수 있으며, 제공된 필드만 업데이트됩니다.
   * null이 아닌 필드만 변경되는 부분 업데이트 방식을 사용합니다.
   * </p>
   * 
   * @param logId     수정할 로그 ID
   * @param request   수정할 정보
   * @param userEmail 수정 요청자의 이메일
   * @return 수정된 로그 정보
   * @throws IllegalArgumentException 권한이 없거나 로그를 찾을 수 없는 경우
   * @throws IllegalArgumentException 위치 좌표가 유효하지 않은 경우
   */
  @Transactional
  public LogResponse updateLog(Long logId, LogUpdateRequest request, String userEmail) {
    log.info("로그 수정 요청 - ID: {}, 사용자: {}", logId, userEmail);

    User user = findUserByEmail(userEmail);
    Log logEntity = findUserOwnedLog(logId, user.getId());

    validateLocationIfPresent(request);
    updateLogFields(logEntity, request);

    log.info("로그 수정 완료 - ID: {}", logEntity.getId());
    return LogResponse.from(logEntity);
  }

  /**
   * 로그를 삭제합니다.
   * 
   * <p>
   * 로그 작성자만 삭제할 수 있습니다.
   * </p>
   * 
   * @param logId     삭제할 로그 ID
   * @param userEmail 삭제 요청자의 이메일
   * @throws IllegalArgumentException 권한이 없거나 로그를 찾을 수 없는 경우
   */
  @Transactional
  public void deleteLog(Long logId, String userEmail) {
    log.info("로그 삭제 요청 - ID: {}, 사용자: {}", logId, userEmail);

    User user = findUserByEmail(userEmail);
    Log logEntity = findUserOwnedLog(logId, user.getId());

    logRepository.delete(logEntity);
    log.info("로그 삭제 완료 - ID: {}", logId);
  }

  /**
   * 검색 조건에 따라 페이징 객체를 생성합니다.
   */
  private Pageable createPageable(LogSearchRequest searchRequest) {
    Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
    return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
  }

  /**
   * 정렬 기준과 방향에 따라 Sort 객체를 생성합니다.
   */
  private Sort createSort(String sortBy, String sortDirection) {
    Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;

    return switch (sortBy) {
      case "createdAt" -> Sort.by(direction, "createdAt");
      case "viewCount" -> Sort.by(direction, "viewCount");
      case "distance" -> Sort.by(direction, "createdAt"); // 거리 정렬은 쿼리에서 처리
      default -> Sort.by(Sort.Direction.DESC, "createdAt");
    };
  }

  /**
   * 이메일로 사용자를 조회합니다.
   * 
   * @param email 조회할 사용자 이메일
   * @return 조회된 사용자
   * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
   */
  private User findUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));
  }

  /**
   * 사용자 권한에 따라 조회 가능한 로그를 찾습니다.
   */
  private Log findViewableLog(Long logId, User viewer) {
    if (viewer != null) {
      return logRepository.findViewableLogByIdAndUserId(logId, viewer.getId())
          .orElseThrow(() -> new IllegalArgumentException("로그를 찾을 수 없습니다."));
    } else {
      return logRepository.findByIdAndIsPublicTrue(logId)
          .orElseThrow(() -> new IllegalArgumentException("로그를 찾을 수 없습니다."));
    }
  }

  /**
   * 검색 요청에 위치 정보가 포함되어 있는지 확인합니다.
   */
  private boolean hasLocationInfo(LogSearchRequest searchRequest) {
    return searchRequest.getLatitude() != null && searchRequest.getLongitude() != null;
  }

  /**
   * 위치 기반 로그 검색을 수행합니다.
   */
  private Page<Log> searchByLocation(LogSearchRequest searchRequest, User viewer, Pageable pageable) {
    LocationUtils.validateCoordinates(searchRequest.getLatitude(), searchRequest.getLongitude());

    if (viewer != null) {
      return logRepository.findLogsByLocationNearWithUser(
          searchRequest.getLatitude(),
          searchRequest.getLongitude(),
          searchRequest.getRadiusKm(),
          viewer.getId(),
          pageable);
    } else {
      return logRepository.findPublicLogsByLocationNear(
          searchRequest.getLatitude(),
          searchRequest.getLongitude(),
          searchRequest.getRadiusKm(),
          pageable);
    }
  }

  /**
   * 전체 로그 검색을 수행합니다.
   */
  private Page<Log> searchAllLogs(Pageable pageable) {
    return logRepository.findAll(pageable);
  }

  /**
   * 사용자가 소유한 로그를 찾습니다.
   */
  private Log findUserOwnedLog(Long logId, Long userId) {
    return logRepository.findByIdAndUserId(logId, userId)
        .orElseThrow(() -> new IllegalArgumentException("수정 권한이 없거나 로그를 찾을 수 없습니다."));
  }

  /**
   * 수정 요청에 위치 정보가 있는 경우 유효성을 검증합니다.
   */
  private void validateLocationIfPresent(LogUpdateRequest request) {
    if (request.getLatitude() != null && request.getLongitude() != null) {
      LocationUtils.validateCoordinates(request.getLatitude(), request.getLongitude());
    }
  }

  /**
   * 로그 필드들을 업데이트합니다.
   */
  private void updateLogFields(Log logEntity, LogUpdateRequest request) {
    if (request.getContent() != null) {
      logEntity.updateContent(request.getContent());
    }

    if (request.getLatitude() != null || request.getLongitude() != null || request.getAddress() != null) {
      logEntity.updateLocation(request.getLatitude(), request.getLongitude(), request.getAddress());
    }

    if (request.getIsPublic() != null) {
      logEntity.updatePublicSetting(request.getIsPublic());
    }

    if (request.getMediaUrls() != null) {
      logEntity.updateMediaUrls(request.getMediaUrls());
    }
  }
}