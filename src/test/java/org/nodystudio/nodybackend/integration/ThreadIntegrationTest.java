package org.nodystudio.nodybackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.nodystudio.nodybackend.base.BaseIntegrationTest;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadUpdateRequest;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.repository.UserRepository;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Thread 통합 테스트")
class ThreadIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LogRepository logRepository;

  @Autowired
  private ThreadRepository threadRepository;

  private User testUser;
  private User otherUser;
  private Log testLog;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = userRepository.save(User.builder()
        .provider("google")
        .socialId("123456789")
        .email("test@example.com")
        .nickname("테스트유저")
        .role(RoleType.USER)
        .isActive(true)
        .build());

    otherUser = userRepository.save(User.builder()
        .provider("google")
        .socialId("987654321")
        .email("other@example.com")
        .nickname("다른유저")
        .role(RoleType.USER)
        .isActive(true)
        .build());

    // 테스트 로그 생성
    testLog = logRepository.save(Log.builder()
        .user(testUser)
        .content("테스트 로그 내용")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .address("서울특별시 중구")
        .isPublic(true)
        .build());
  }

  @Test
  @DisplayName("독립 스레드 생성 성공")
  void createIndependentThread_Success() throws Exception {
    // given
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("독립 스레드 내용")
        .isPublic(true)
        .build();

    // when & then
    mockMvc.perform(post("/api/threads")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.data.content").value("독립 스레드 내용"))
        .andExpect(jsonPath("$.data.isPublic").value(true))
        .andExpect(jsonPath("$.data.isIndependent").value(true));
  }

  @Test
  @DisplayName("로그 연결 스레드 생성 성공")
  void createLogLinkedThread_Success() throws Exception {
    // given
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    ThreadCreateRequest request = ThreadCreateRequest.builder()
        .content("로그 연결 스레드 내용")
        .isPublic(true)
        .logId(testLog.getId())
        .build();

    // when & then
    mockMvc.perform(post("/api/threads")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.data.content").value("로그 연결 스레드 내용"))
        .andExpect(jsonPath("$.data.isLinkedToLog").value(true))
        .andExpect(jsonPath("$.data.log.id").value(testLog.getId()));
  }

  @Test
  @DisplayName("스레드 단건 조회 성공")
  void getThread_Success() throws Exception {
    // given
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    // 스레드 생성
    ThreadCreateRequest createRequest = ThreadCreateRequest.builder()
        .content("조회용 스레드 내용")
        .isPublic(true)
        .build();

    mockMvc.perform(post("/api/threads")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    // 생성된 스레드 ID 추출
    Thread savedThread = threadRepository.findAll().get(0);
    Long threadId = savedThread.getId();

    // when & then
    mockMvc.perform(get("/api/threads/" + threadId)
        .with(authentication(authentication)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.data.content").value("조회용 스레드 내용"));
  }

  @Test
  @DisplayName("인증 없이 공개 스레드 조회 성공")
  void getPublicThread_WithoutAuth_Success() throws Exception {
    // given - 공개 스레드 생성
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    ThreadCreateRequest createRequest = ThreadCreateRequest.builder()
        .content("공개 스레드 내용")
        .isPublic(true)
        .build();

    mockMvc.perform(post("/api/threads")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    Thread savedThread = threadRepository.findAll().get(0);
    Long threadId = savedThread.getId();

    // when & then - 인증 없이 조회
    mockMvc.perform(get("/api/threads/" + threadId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.data.content").value("공개 스레드 내용"));
  }

  @Test
  @DisplayName("스레드 수정 성공")
  void updateThread_Success() throws Exception {
    // given
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    // 스레드 생성
    ThreadCreateRequest createRequest = ThreadCreateRequest.builder()
        .content("수정 전 내용")
        .isPublic(true)
        .build();

    mockMvc.perform(post("/api/threads")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    Thread savedThread = threadRepository.findAll().get(0);
    Long threadId = savedThread.getId();

    // 수정 요청
    ThreadUpdateRequest updateRequest = ThreadUpdateRequest.builder()
        .content("수정 후 내용")
        .isPublic(false)
        .build();

    // when & then
    mockMvc.perform(put("/api/threads/" + threadId)
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").exists())
        .andExpect(jsonPath("$.data.content").value("수정 후 내용"))
        .andExpect(jsonPath("$.data.isPublic").value(false));
  }

  @Test
  @DisplayName("스레드 삭제 성공")
  void deleteThread_Success() throws Exception {
    // given
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    // 스레드 생성
    ThreadCreateRequest createRequest = ThreadCreateRequest.builder()
        .content("삭제할 스레드 내용")
        .isPublic(true)
        .build();

    mockMvc.perform(post("/api/threads")
        .with(authentication(authentication))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    Thread savedThread = threadRepository.findAll().get(0);
    Long threadId = savedThread.getId();

    // when & then
    mockMvc.perform(delete("/api/threads/" + threadId)
        .with(authentication(authentication)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").exists());
  }

  @Test
  @DisplayName("다른 사용자의 스레드 수정 시도 - 실패")
  void updateOtherUserThread_Forbidden() throws Exception {
    // given - testUser가 스레드 생성
    UsernamePasswordAuthenticationToken testUserAuth = new UsernamePasswordAuthenticationToken(
        testUser, null, testUser.getRoles());

    ThreadCreateRequest createRequest = ThreadCreateRequest.builder()
        .content("다른 사용자 스레드 내용")
        .isPublic(true)
        .build();

    mockMvc.perform(post("/api/threads")
        .with(authentication(testUserAuth))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    Thread savedThread = threadRepository.findAll().get(0);
    Long threadId = savedThread.getId();

    // when & then - otherUser가 수정 시도
    UsernamePasswordAuthenticationToken otherUserAuth = new UsernamePasswordAuthenticationToken(
        otherUser, null, otherUser.getRoles());

    ThreadUpdateRequest updateRequest = ThreadUpdateRequest.builder()
        .content("악의적 수정")
        .build();

    mockMvc.perform(put("/api/threads/" + threadId)
        .with(authentication(otherUserAuth))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
        .andDo(print())
        .andExpect(status().isNotFound());
  }
}