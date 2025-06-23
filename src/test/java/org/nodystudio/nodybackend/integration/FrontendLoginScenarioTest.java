package org.nodystudio.nodybackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.base.BaseIntegrationTest;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.TokenRefreshRequestDto;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.nodystudio.nodybackend.security.jwt.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 프론트엔드 로그인 기능의 핵심 시나리오 검증 테스트
 * 
 * 이 테스트는 프론트엔드에서 자주 발생하는 로그인 관련 요청들이
 * 백엔드 서버에서 올바르게 처리되는지 검증합니다.
 */
@DisplayName("프론트엔드 로그인 핵심 시나리오 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class FrontendLoginScenarioTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TokenProvider tokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  private User testUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    testUser = User.builder()
        .provider("google")
        .socialId("google_12345")
        .email("user@example.com")
        .nickname("테스트유저")
        .isActive(true)
        .build();
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("프론트엔드 로그인 버튼 클릭 → OAuth2 인증 서버 리다이렉트")
  void whenUserClicksLoginButton_shouldRedirectToOAuth2Server() throws Exception {
    // when: 사용자가 프론트엔드에서 "구글로 로그인" 버튼 클릭
    MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andReturn();

    // then: 구글 OAuth2 인증 페이지로 리다이렉트
    String location = result.getResponse().getHeader("Location");
    assertThat(location).contains("accounts.google.com/o/oauth2/v2/auth");
    assertThat(location).contains("client_id=");
    assertThat(location).contains("response_type=code");
    assertThat(location).contains("scope=profile%20email");
  }

  @Test
  @DisplayName("프론트엔드 토큰 갱신 요청 → 새로운 토큰 발급")
  void whenAccessTokenExpires_shouldRefreshTokenSuccessfully() throws Exception {
    // given: 로그인된 사용자의 refresh token
    User savedUser = userRepository.saveAndFlush(testUser);
    String refreshToken = tokenProvider.createRefreshToken(savedUser);
    LocalDateTime expiry = tokenProvider.getRefreshTokenExpiry();

    savedUser.updateRefreshToken(refreshToken, expiry);
    userRepository.saveAndFlush(savedUser);

    TokenRefreshRequestDto request = new TokenRefreshRequestDto(refreshToken);

    // when: 프론트엔드에서 토큰 갱신 요청
    MvcResult result = mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("AUTH_S003"))
        .andExpect(jsonPath("$.data.grantType").value("Bearer"))
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists())
        .andReturn();

    // then: 새로운 토큰들이 정상적으로 발급됨
    String responseBody = result.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.get("data");

    String newAccessToken = (String) data.get("accessToken");
    String newRefreshToken = (String) data.get("refreshToken");

    assertThat(newAccessToken).isNotNull().isNotEmpty();
    assertThat(newRefreshToken).isNotNull().isNotEmpty();
    assertThat(newRefreshToken).isNotEqualTo(refreshToken);
  }

  @Test
  @DisplayName("잘못된 토큰으로 요청 → 적절한 에러 응답")
  void whenInvalidTokenSent_shouldReturnProperErrorResponse() throws Exception {
    // given: 잘못된 토큰
    TokenRefreshRequestDto request = new TokenRefreshRequestDto("invalid.jwt.token");

    // when: 잘못된 토큰으로 갱신 요청
    MvcResult result = mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.code").value("A005"))
        .andReturn();

    // then: 프론트엔드에서 처리할 수 있는 에러 구조
    String responseBody = result.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

    assertThat(response.get("status")).isEqualTo(401);
    assertThat(response.get("code")).isEqualTo("A005");
    assertThat(response.get("message")).isNotNull();
  }

  @Test
  @DisplayName("프론트엔드 장시간 사용 → 토큰 자동 갱신")
  void whenLongTermUsage_shouldHandleMultipleTokenRefreshes() throws Exception {
    // given: 초기 로그인 상태
    User savedUser = userRepository.saveAndFlush(testUser);
    String currentToken = tokenProvider.createRefreshToken(savedUser);
    LocalDateTime expiry = tokenProvider.getRefreshTokenExpiry();

    savedUser.updateRefreshToken(currentToken, expiry);
    userRepository.saveAndFlush(savedUser);

    // when & then: 여러 번 토큰 갱신 (장시간 사용 시뮬레이션)
    for (int i = 1; i <= 3; i++) {
      TokenRefreshRequestDto request = new TokenRefreshRequestDto(currentToken);

      MvcResult result = mockMvc.perform(post("/api/auth/refresh")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andReturn();

      // 새 토큰 추출
      String responseBody = result.getResponse().getContentAsString();
      @SuppressWarnings("unchecked")
      Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) response.get("data");

      String newToken = (String) data.get("refreshToken");
      assertThat(newToken).isNotEqualTo(currentToken);

      currentToken = newToken;
    }
  }

  @Test
  @DisplayName("보안 시나리오: 동일 토큰 재사용 시도 차단")
  void whenTokenReusedConcurrently_shouldPreventSecurityBreach() throws Exception {
    // given: 유효한 refresh token
    User savedUser = userRepository.saveAndFlush(testUser);
    String refreshToken = tokenProvider.createRefreshToken(savedUser);
    LocalDateTime expiry = tokenProvider.getRefreshTokenExpiry();

    savedUser.updateRefreshToken(refreshToken, expiry);
    userRepository.saveAndFlush(savedUser);

    TokenRefreshRequestDto request = new TokenRefreshRequestDto(refreshToken);

    // when: 첫 번째 갱신 성공
    mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // then: 동일한 토큰으로 두 번째 시도 시 차단
    mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("A005"));
  }

  @Test
  @DisplayName("실제 사용자 시나리오: 로그인 → 사용 → 토큰 갱신 → 재사용")
  void realWorldUserScenario_shouldWorkEndToEnd() throws Exception {
    // 1단계: OAuth2 로그인 시작
    MvcResult loginResult = mockMvc.perform(get("/oauth2/authorization/google"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    String oauthUrl = loginResult.getResponse().getHeader("Location");
    assertThat(oauthUrl).contains("accounts.google.com");

    // 2단계: 로그인 완료 후 토큰 보유 상태 시뮬레이션
    User savedUser = userRepository.saveAndFlush(testUser);
    String initialToken = tokenProvider.createRefreshToken(savedUser);
    LocalDateTime expiry = tokenProvider.getRefreshTokenExpiry();

    savedUser.updateRefreshToken(initialToken, expiry);
    userRepository.saveAndFlush(savedUser);

    // 3단계: 일정 시간 후 토큰 갱신
    TokenRefreshRequestDto refreshRequest = new TokenRefreshRequestDto(initialToken);

    MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(refreshRequest)))
        .andExpect(status().isOk())
        .andReturn();

    // 4단계: 새 토큰으로 재갱신 가능 확인
    String refreshBody = refreshResult.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> refreshResponse = objectMapper.readValue(refreshBody, Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> refreshData = (Map<String, Object>) refreshResponse.get("data");

    String newRefreshToken = (String) refreshData.get("refreshToken");
    TokenRefreshRequestDto secondRequest = new TokenRefreshRequestDto(newRefreshToken);

    mockMvc.perform(post("/api/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(secondRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists());
  }
}
