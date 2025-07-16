package org.nodystudio.nodybackend.controller.like;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.like.LikeRequest;
import org.nodystudio.nodybackend.dto.like.LikeStatusResponse;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.service.like.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeController 테스트")
class LikeControllerTest {

  @Mock
  private LikeService likeService;

  @InjectMocks
  private LikeController likeController;

  /**
   * 테스트용 Authentication 객체 생성
   */
  private Authentication createMockAuthentication() {
    return new UsernamePasswordAuthenticationToken("test@example.com", "password");
  }

  @Nested
  @DisplayName("좋아요 토글 API 테스트")
  class ToggleLikeApiTest {

    @Test
    @DisplayName("스레드 좋아요 토글이 성공한다")
    void toggleLike_WithValidThreadRequest_ShouldReturnSuccess() {
      // Given
      LikeRequest request = LikeRequest.builder()
          .targetType(TargetType.THREAD)
          .targetId(1L)
          .build();

      LikeStatusResponse response = LikeStatusResponse.of(true, 1L, TargetType.THREAD, 1L);

      given(likeService.toggleLike(any(LikeRequest.class), eq("test@example.com")))
          .willReturn(response);

      Authentication authentication = createMockAuthentication();

      // When
      ResponseEntity<ApiResponse<LikeStatusResponse>> result = likeController.toggleLike(request, authentication);

      // Then
      assertEquals(200, result.getStatusCode().value());
      assertNotNull(result.getBody());
      ApiResponse<LikeStatusResponse> body = result.getBody();
      assertEquals(200, body.getStatus());
      assertEquals("LIKE_S001", body.getCode());
      assertEquals("좋아요 토글이 성공적으로 처리되었습니다.", body.getMessage());
      assertEquals(true, body.getData().getIsLiked());
      assertEquals(1L, body.getData().getLikeCount());
      assertEquals(TargetType.THREAD, body.getData().getTargetType());
      assertEquals(1L, body.getData().getTargetId());
    }

    @Test
    @DisplayName("로그 좋아요 토글이 성공한다")
    void toggleLike_WithValidLogRequest_ShouldReturnSuccess() {
      // Given
      LikeRequest request = LikeRequest.builder()
          .targetType(TargetType.LOG)
          .targetId(1L)
          .build();

      LikeStatusResponse response = LikeStatusResponse.of(true, 1L, TargetType.LOG, 1L);

      given(likeService.toggleLike(any(LikeRequest.class), eq("test@example.com")))
          .willReturn(response);

      Authentication authentication = createMockAuthentication();

      // When
      ResponseEntity<ApiResponse<LikeStatusResponse>> result = likeController.toggleLike(request, authentication);

      // Then
      assertEquals(200, result.getStatusCode().value());
      assertNotNull(result.getBody());
      ApiResponse<LikeStatusResponse> body = result.getBody();
      assertEquals(200, body.getStatus());
      assertEquals("LIKE_S001", body.getCode());
      assertEquals(true, body.getData().getIsLiked());
      assertEquals(1L, body.getData().getLikeCount());
      assertEquals(TargetType.LOG, body.getData().getTargetType());
      assertEquals(1L, body.getData().getTargetId());
    }

    @Test
    @DisplayName("존재하지 않는 대상으로 404 에러를 반환한다")
    void toggleLike_WithNonExistentTarget_ShouldReturnNotFound() {
      // Given
      LikeRequest request = LikeRequest.builder()
          .targetType(TargetType.THREAD)
          .targetId(999L)
          .build();

      given(likeService.toggleLike(any(LikeRequest.class), eq("test@example.com")))
          .willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다. ID: 999"));

      Authentication authentication = createMockAuthentication();

      // When & Then
      assertThrows(ResourceNotFoundException.class, () -> {
        likeController.toggleLike(request, authentication);
      });
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 404 에러를 반환한다")
    void toggleLike_WithNonExistentUser_ShouldReturnNotFound() {
      // Given
      LikeRequest request = LikeRequest.builder()
          .targetType(TargetType.THREAD)
          .targetId(1L)
          .build();

      given(likeService.toggleLike(any(LikeRequest.class), eq("test@example.com")))
          .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다. Email: test@example.com"));

      Authentication authentication = createMockAuthentication();

      // When & Then
      assertThrows(UserNotFoundException.class, () -> {
        likeController.toggleLike(request, authentication);
      });
    }

    @Test
    @DisplayName("인증 정보가 없으면 IllegalArgumentException을 반환한다")
    void toggleLike_WithoutAuthentication_ShouldThrowException() {
      // Given
      LikeRequest request = LikeRequest.builder()
          .targetType(TargetType.THREAD)
          .targetId(1L)
          .build();

      // When & Then
      assertThrows(IllegalArgumentException.class, () -> {
        likeController.toggleLike(request, null);
      });
    }
  }

  @Nested
  @DisplayName("좋아요 상태 조회 API 테스트")
  class GetLikeStatusApiTest {

    @Test
    @DisplayName("인증된 사용자의 좋아요 상태 조회가 성공한다")
    void getLikeStatus_WithAuthenticatedUser_ShouldReturnSuccess() {
      // Given
      LikeStatusResponse response = LikeStatusResponse.of(true, 5L, TargetType.THREAD, 1L);

      given(likeService.getLikeStatus(TargetType.THREAD, 1L, "test@example.com"))
          .willReturn(response);

      Authentication authentication = createMockAuthentication();

      // When
      ResponseEntity<ApiResponse<LikeStatusResponse>> result = likeController.getLikeStatus(
          TargetType.THREAD, 1L, authentication);

      // Then
      assertEquals(200, result.getStatusCode().value());
      assertNotNull(result.getBody());
      ApiResponse<LikeStatusResponse> body = result.getBody();
      assertEquals(200, body.getStatus());
      assertEquals("LIKE_S002", body.getCode());
      assertEquals("좋아요 상태가 성공적으로 조회되었습니다.", body.getMessage());
      assertEquals(true, body.getData().getIsLiked());
      assertEquals(5L, body.getData().getLikeCount());
      assertEquals(TargetType.THREAD, body.getData().getTargetType());
      assertEquals(1L, body.getData().getTargetId());
    }

    @Test
    @DisplayName("익명 사용자의 좋아요 상태 조회가 성공한다")
    void getLikeStatus_WithAnonymousUser_ShouldReturnSuccess() {
      // Given
      LikeStatusResponse response = LikeStatusResponse.of(false, 3L, TargetType.LOG, 1L);

      given(likeService.getLikeStatus(TargetType.LOG, 1L, null))
          .willReturn(response);

      // When
      ResponseEntity<ApiResponse<LikeStatusResponse>> result = likeController.getLikeStatus(
          TargetType.LOG, 1L, null);

      // Then
      assertEquals(200, result.getStatusCode().value());
      assertNotNull(result.getBody());
      ApiResponse<LikeStatusResponse> body = result.getBody();
      assertEquals(200, body.getStatus());
      assertEquals("LIKE_S002", body.getCode());
      assertEquals(false, body.getData().getIsLiked());
      assertEquals(3L, body.getData().getLikeCount());
      assertEquals(TargetType.LOG, body.getData().getTargetType());
      assertEquals(1L, body.getData().getTargetId());
    }

    @Test
    @DisplayName("존재하지 않는 대상으로 404 에러를 반환한다")
    void getLikeStatus_WithNonExistentTarget_ShouldReturnNotFound() {
      // Given
      given(likeService.getLikeStatus(TargetType.THREAD, 999L, null))
          .willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다. ID: 999"));

      // When & Then
      assertThrows(ResourceNotFoundException.class, () -> {
        likeController.getLikeStatus(TargetType.THREAD, 999L, null);
      });
    }
  }
}