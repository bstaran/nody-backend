package org.nodystudio.nodybackend.controller.like;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.controller.like.docs.LikeApiDocs;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.code.SuccessCode;
import org.nodystudio.nodybackend.dto.like.LikeRequest;
import org.nodystudio.nodybackend.dto.like.LikeStatusResponse;
import org.nodystudio.nodybackend.service.like.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좋아요 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController implements LikeApiDocs {

  private final LikeService likeService;

  /**
   * 좋아요를 토글합니다. (좋아요 추가/취소)
   *
   * @param request        좋아요 요청 정보
   * @param authentication 인증된 사용자 정보
   * @return 좋아요 상태 정보
   */
  @PostMapping
  public ResponseEntity<ApiResponse<LikeStatusResponse>> toggleLike(
      @Valid @RequestBody LikeRequest request,
      Authentication authentication) {

    // 인증 객체가 null인 경우 처리
    if (authentication == null) {
      throw new IllegalArgumentException("인증 정보가 필요합니다.");
    }

    String userEmail = authentication.getName();

    log.info("좋아요 토글 요청 - email: {}, targetType: {}, targetId: {}",
        userEmail, request.getTargetType(), request.getTargetId());

    LikeStatusResponse response = likeService.toggleLike(request, userEmail);

    log.info("좋아요 토글 완료 - email: {}, isLiked: {}, likeCount: {}",
        userEmail, response.getIsLiked(), response.getLikeCount());

    return ResponseEntity
        .status(SuccessCode.LIKE_TOGGLE_SUCCESS.getStatus())
        .body(ApiResponse.success(SuccessCode.LIKE_TOGGLE_SUCCESS, response));
  }

  /**
   * 좋아요 상태를 조회합니다.
   *
   * @param targetType     대상 타입
   * @param targetId       대상 ID
   * @param authentication 인증된 사용자 정보 (선택사항)
   * @return 좋아요 상태 정보
   */
  @GetMapping("/status")
  public ResponseEntity<ApiResponse<LikeStatusResponse>> getLikeStatus(
      @RequestParam TargetType targetType,
      @RequestParam Long targetId,
      Authentication authentication) {

    String userEmail = authentication != null ? authentication.getName() : null;

    log.info("좋아요 상태 조회 요청 - targetType: {}, targetId: {}, userEmail: {}",
        targetType, targetId, userEmail);

    LikeStatusResponse response = likeService.getLikeStatus(targetType, targetId, userEmail);

    log.info("좋아요 상태 조회 완료 - targetType: {}, targetId: {}, isLiked: {}, likeCount: {}",
        targetType, targetId, response.getIsLiked(), response.getLikeCount());

    return ResponseEntity
        .status(SuccessCode.LIKE_STATUS_RETRIEVED.getStatus())
        .body(ApiResponse.success(SuccessCode.LIKE_STATUS_RETRIEVED, response));
  }
}