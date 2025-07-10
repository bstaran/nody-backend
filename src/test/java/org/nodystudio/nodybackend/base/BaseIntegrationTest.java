package org.nodystudio.nodybackend.base;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트를 위한 베이스 클래스 Spring Context 캐싱을 최적화하고 일관된 테스트 환경을 제공합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class BaseIntegrationTest {
  // 공통 테스트 설정과 유틸리티 메서드들을 여기에 추가
}
