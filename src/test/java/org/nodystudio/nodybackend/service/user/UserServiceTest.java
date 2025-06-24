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
}