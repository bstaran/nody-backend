package org.nodystudio.nodybackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.base.BaseIntegrationTest;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("사용자 API 통합 테스트")
class UserApiIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = userRepository.save(User.builder()
        .provider("google")
        .socialId("123456789")
        .email("test@example.com")
        .nickname("테스트닉네임")
        .role(RoleType.USER)
        .isActive(true)
        .build());
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 - 성공")
  void getCurrentUser_success() throws Exception {
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUser, null,
        testUser.getRoles());

    mockMvc.perform(get("/api/user/me")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS_S001"))
        .andExpect(jsonPath("$.data.email").value("test@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("테스트닉네임"));
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 - 인증되지 않은 사용자")
  void getCurrentUser_notAuthenticated() throws Exception {
    mockMvc.perform(get("/api/user/me")
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("A007"));
  }

  @Test
  @DisplayName("닉네임 변경 - 성공")
  void updateNickname_success() throws Exception {
    // given
    String newNickname = "새로운닉네임";
    UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto(newNickname);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUser, null,
        testUser.getRoles());

    // when & then
    mockMvc.perform(put("/api/user/nickname")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS_S001"))
        .andExpect(jsonPath("$.data.nickname").value(newNickname));

    User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(updatedUser.getNickname()).isEqualTo(newNickname);
  }

  @Test
  @DisplayName("닉네임 변경 - 유효성 검증 실패 (빈 닉네임)")
  void updateNickname_validationFail_blankNickname() throws Exception {
    // given
    UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto("");
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUser, null,
        testUser.getRoles());

    // when & then
    mockMvc.perform(put("/api/user/nickname")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("C001"));
  }

  @Test
  @DisplayName("닉네임 변경 - 인증되지 않은 사용자")
  void updateNickname_notAuthenticated() throws Exception {
    // given
    UpdateNicknameRequestDto requestDto = new UpdateNicknameRequestDto("새로운닉네임");

    // when & then
    mockMvc.perform(put("/api/user/nickname")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("A007"));
  }

  @Test
  @DisplayName("계정 탈퇴 - 성공")
  void deactivateAccount_success() throws Exception {
    // given
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUser, null,
        testUser.getRoles());

    // when & then
    mockMvc.perform(delete("/api/user/me")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS_S001"))
        .andExpect(jsonPath("$.data").doesNotExist());

    User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(updatedUser.getIsActive()).isFalse();
    assertThat(updatedUser.getDeletedAt()).isNotNull();
    assertThat(updatedUser.getRefreshToken()).isNull();
  }

  @Test
  @DisplayName("계정 탈퇴 - 인증되지 않은 사용자")
  void deactivateAccount_notAuthenticated() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/user/me")
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("A007"));
  }

  @Test
  @DisplayName("계정 탈퇴 - 이미 탈퇴한 계정")
  void deactivateAccount_alreadyDeactivated() throws Exception {
    // given - 먼저 계정을 탈퇴시킴
    testUser.deactivateAccount();
    userRepository.save(testUser);

    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(testUser, null,
        testUser.getRoles());

    // when & then - 탈퇴한 계정으로는 API 접근 불가 (활성 사용자만 조회)
    mockMvc.perform(delete("/api/user/me")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("U005"));
  }
}