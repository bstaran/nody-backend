package org.nodystudio.nodybackend.service.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCleanupBatchService 테스트")
class UserCleanupBatchServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCleanupBatchService userCleanupBatchService;

    private User expiredUser1;
    private User expiredUser2;

    @BeforeEach
    void setUp() {
        
        // 31일 전에 탈퇴한 사용자
        expiredUser1 = User.builder()
                .id(1L)
                .provider("google")
                .socialId("123456789")
                .email("expired1@example.com")
                .nickname("탈퇴사용자1")
                .role(RoleType.USER)
                .isActive(false)
                .build();
        expiredUser1.deactivateAccount();
        
        // 32일 전에 탈퇴한 사용자 (강제로 deletedAt 설정)
        expiredUser2 = User.builder()
                .id(2L)
                .provider("google")
                .socialId("987654321")
                .email("expired2@example.com")
                .nickname("탈퇴사용자2")
                .role(RoleType.USER)
                .isActive(false)
                .build();
        expiredUser2.deactivateAccount();
    }

    @Test
    @DisplayName("30일 이전에 탈퇴한 사용자 삭제 - 성공")
    void deleteExpiredDeactivatedUsers_success() {
        // given
        List<User> expiredUsers = Arrays.asList(expiredUser1, expiredUser2);
        given(userRepository.findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(expiredUsers);

        // when
        userCleanupBatchService.deleteExpiredDeactivatedUsers();

        // then
        verify(userRepository).findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
        verify(userRepository, times(2)).delete(any(User.class));
    }

    @Test
    @DisplayName("삭제할 만료된 사용자가 없는 경우")
    void deleteExpiredDeactivatedUsers_noExpiredUsers() {
        // given
        given(userRepository.findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        userCleanupBatchService.deleteExpiredDeactivatedUsers();

        // then
        verify(userRepository).findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
        verify(userRepository, times(0)).delete(any(User.class));
    }

    @Test
    @DisplayName("개별 사용자 삭제 중 오류 발생해도 배치 작업 계속 진행")
    void deleteExpiredDeactivatedUsers_partialFailure() {
        // given
        List<User> expiredUsers = Arrays.asList(expiredUser1, expiredUser2);
        given(userRepository.findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(expiredUsers);
        
        // 첫 번째 사용자 삭제 시 예외 발생
        doThrow(new RuntimeException("삭제 중 오류"))
                .when(userRepository).delete(expiredUser1);

        // when
        userCleanupBatchService.deleteExpiredDeactivatedUsers();

        // then
        verify(userRepository).findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
        verify(userRepository, times(2)).delete(any(User.class)); // 두 사용자 모두 삭제 시도
    }

    @Test
    @DisplayName("수동 정리 작업 - 성공")
    void manualCleanupExpiredUsers_success() {
        // given
        List<User> expiredUsers = Arrays.asList(expiredUser1, expiredUser2);
        given(userRepository.findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(expiredUsers);

        // when
        int deletedCount = userCleanupBatchService.manualCleanupExpiredUsers();

        // then
        assertThat(deletedCount).isEqualTo(2);
        verify(userRepository).findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
        verify(userRepository, times(2)).delete(any(User.class));
    }

    @Test
    @DisplayName("수동 정리 작업 - 삭제할 사용자 없음")
    void manualCleanupExpiredUsers_noUsers() {
        // given
        given(userRepository.findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        int deletedCount = userCleanupBatchService.manualCleanupExpiredUsers();

        // then
        assertThat(deletedCount).isEqualTo(0);
        verify(userRepository).findByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
        verify(userRepository, times(0)).delete(any(User.class));
    }

    @Test
    @DisplayName("삭제 예정 사용자 수 조회")
    void countUsersToBeDeleted_success() {
        // given
        long expectedCount = 5L;
        given(userRepository.countByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(expectedCount);

        // when
        long actualCount = userCleanupBatchService.countUsersToBeDeleted(5);

        // then
        assertThat(actualCount).isEqualTo(expectedCount);
        verify(userRepository).countByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("삭제 예정 사용자 수 조회 - 0명")
    void countUsersToBeDeleted_zeroUsers() {
        // given
        given(userRepository.countByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class)))
                .willReturn(0L);

        // when
        long actualCount = userCleanupBatchService.countUsersToBeDeleted(10);

        // then
        assertThat(actualCount).isEqualTo(0L);
        verify(userRepository).countByIsActiveFalseAndDeletedAtBefore(any(LocalDateTime.class));
    }
}