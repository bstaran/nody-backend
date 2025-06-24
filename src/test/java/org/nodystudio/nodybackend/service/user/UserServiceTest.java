package org.nodystudio.nodybackend.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final Long TEST_USER_ID = 1L;
    private final String TEST_USER_ID_STRING = "1";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .provider("google")
                .socialId("123456789")
                .email("test@example.com")
                .nickname("기존닉네임")
                .role(RoleType.USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("사용자 정보 조회 - 성공")
    void getCurrentUser_success() {
        // given
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // when
        UserDetailResponseDto result = userService.getCurrentUser(TEST_USER_ID_STRING);

        // then
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getNickname()).isEqualTo(testUser.getNickname());
        verify(userRepository).findById(TEST_USER_ID);
    }

    @Test
    @DisplayName("사용자 정보 조회 - 사용자 없음")
    void getCurrentUser_userNotFound() {
        // given
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser(TEST_USER_ID_STRING))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자 ID '1'로 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("닉네임 변경 - 성공")
    void updateNickname_success() {
        // given
        String newNickname = "새로운닉네임";
        UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto(newNickname);
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // when
        UserDetailResponseDto result = userService.updateNickname(TEST_USER_ID_STRING, requestDto);

        // then
        assertThat(result.getNickname()).isEqualTo(newNickname);
        assertThat(testUser.getNickname()).isEqualTo(newNickname);
        verify(userRepository).findById(TEST_USER_ID);
    }

    @Test
    @DisplayName("닉네임 변경 - 사용자 없음")
    void updateNickname_userNotFound() {
        // given
        UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto("새로운닉네임");
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(TEST_USER_ID_STRING, requestDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자 ID '1'로 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("닉네임 변경 - 빈 닉네임으로 변경 시도")
    void updateNickname_blankNickname() {
        // given
        UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto("");
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(TEST_USER_ID_STRING, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("닉네임은 공백일 수 없습니다.");
    }

    @Test
    @DisplayName("사용자 조회 - null userId")
    void getCurrentUser_nullUserId() {
        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("사용자 조회 - 빈 userId")
    void getCurrentUser_emptyUserId() {
        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("사용자 조회 - 공백 userId")
    void getCurrentUser_blankUserId() {
        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다.");
    }

    @Test
    @DisplayName("사용자 조회 - 숫자가 아닌 userId")
    void getCurrentUser_invalidUserId() {
        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 사용자 ID 형식입니다: abc");
    }

    @Test
    @DisplayName("사용자 조회 - 음수 userId")
    void getCurrentUser_negativeUserId() {
        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser("-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 양수여야 합니다: -1");
    }

    @Test
    @DisplayName("사용자 조회 - 0인 userId")
    void getCurrentUser_zeroUserId() {
        // when & then
        assertThatThrownBy(() -> userService.getCurrentUser("0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 양수여야 합니다: 0");
    }

    @Test
    @DisplayName("닉네임 변경 - 유효하지 않은 userId")
    void updateNickname_invalidUserId() {
        // given
        UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto("새닉네임");

        // when & then
        assertThatThrownBy(() -> userService.updateNickname("invalid", requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 사용자 ID 형식입니다: invalid");
    }
}