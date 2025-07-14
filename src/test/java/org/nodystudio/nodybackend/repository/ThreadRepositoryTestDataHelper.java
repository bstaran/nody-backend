package org.nodystudio.nodybackend.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * ThreadRepository 테스트들을 위한 공통 테스트 데이터 생성 유틸리티입니다.
 *
 * <p>이 클래스는 Builder 패턴을 사용하여 테스트에 필요한 User, Log, Thread 엔티티를
 * 유연하고 일관된 방식으로 생성할 수 있도록 도와줍니다.</p>
 *
 * <h3>주요 특징:</h3>
 * <ul>
 *   <li>플루언트 API를 통한 직관적인 데이터 생성</li>
 *   <li>자동 persist와 영속성 컨텍스트 관리</li>
 *   <li>커스터마이징 가능한 엔티티 필드</li>
 *   <li>자동 타임스탬프 관리</li>
 * </ul>
 *
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * // 기본 사용법
 * TestDataContainer testData = new TestDataBuilder(entityManager)
 *     .withTimestamps(LocalDateTime.of(2024, 1, 1, 10, 0, 0), 5)
 *     .withCustomUser(u -> u.email("test@example.com").nickname("testUser"))
 *     .withCustomLog(l -> l.content("테스트 로그"))
 *     .addThread(t -> t.content("테스트 스레드").isPublic(true))
 *     .finalizeWithTimestamps();
 *
 * // 고급 사용법 - 중간 데이터 접근
 * TestDataBuilder builder = new TestDataBuilder(entityManager)
 *     .withCustomUser(u -> u.email("user@test.com"))
 *     .withCustomLog(l -> l.content("로그"))
 *     .buildUsersAndLogs();
 *
 * Log log = builder.getCurrentData().logs.get(0);
 * TestDataContainer data = builder
 *     .addThread(t -> t.content("스레드").log(log))
 *     .finalizeWithTimestamps();
 * }</pre>
 *
 * @author Generated
 * @see TestDataBuilder
 * @see TestDataContainer
 */
public class ThreadRepositoryTestDataHelper {

  /**
   * 스레드의 생성일시를 업데이트하는 공통 메서드입니다.
   *
   * <p>JPA Auditing으로 인해 자동으로 설정된 생성일시를 테스트 요구사항에 맞게 수정합니다.
   * 주로 시간 순서에 따른 정렬 테스트에서 사용됩니다.</p>
   *
   * @param entityManager JPA 테스트용 EntityManager
   * @param threadId      업데이트할 스레드 ID
   * @param createdAt     새로운 생성일시
   *
   *                      <h3>사용 예시:</h3>
   *                      <pre>{@code
   *                                                                // 스레드들의 생성일시를 5분 간격으로 설정
   *                                                                LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
   *                                                                for (int i = 0; i < threads.size(); i++) {
   *                                                                    updateThreadCreatedAt(entityManager, threads.get(i).getId(),
   *                                                                                         baseTime.plusMinutes(i * 5));
   *                                                                }
   *                                                                }</pre>
   */
  public static void updateThreadCreatedAt(TestEntityManager entityManager, Long threadId,
      LocalDateTime createdAt) {
    entityManager.getEntityManager()
        .createQuery("UPDATE Thread t SET t.createdAt = :createdAt WHERE t.id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", threadId)
        .executeUpdate();
  }

  /**
   * 테스트 데이터를 담는 간단한 컨테이너 클래스입니다.
   *
   * <p>생성된 테스트 엔티티들을 타입별로 분류하여 저장하며,
   * 테스트에서 생성된 데이터에 쉽게 접근할 수 있도록 도와줍니다.</p>
   *
   * <h3>사용 예시:</h3>
   * <pre>{@code
   * TestDataContainer data = builder.finalizeWithTimestamps();
   *
   * // 첫 번째 사용자 가져오기
   * User firstUser = data.users.get(0);
   *
   * // 모든 공개 스레드 필터링
   * List<Thread> publicThreads = data.threads.stream()
   *     .filter(Thread::getIsPublic)
   *     .collect(Collectors.toList());
   *
   * // 특정 로그에 연결된 스레드 찾기
   * Log targetLog = data.logs.get(0);
   * List<Thread> logThreads = data.threads.stream()
   *     .filter(t -> Objects.equals(t.getLog(), targetLog))
   *     .collect(Collectors.toList());
   * }</pre>
   */
  public static class TestDataContainer {

    /**
     * 생성된 사용자 목록 (생성 순서대로 정렬)
     */
    public final List<User> users = new ArrayList<>();

    /**
     * 생성된 로그 목록 (생성 순서대로 정렬)
     */
    public final List<Log> logs = new ArrayList<>();

    /**
     * 생성된 스레드 목록 (생성 순서대로 정렬, 타임스탬프 적용됨)
     */
    public final List<Thread> threads = new ArrayList<>();
  }

  /**
   * Builder 패턴을 사용한 테스트 데이터 생성기입니다.
   *
   * <p>플루언트 API를 통해 User, Log, Thread 엔티티를 단계적으로 생성하고
   * 자동으로 데이터베이스에 persist합니다. 각 엔티티는 커스터마이저 함수를 통해 테스트 요구사항에 맞게 설정할 수 있습니다.</p>
   *
   * <h3>워크플로우:</h3>
   * <ol>
   *   <li><strong>설정 단계</strong>: {@code withTimestamps()}, {@code withCustomUser()}, {@code withCustomLog()}</li>
   *   <li><strong>빌드 단계</strong>: {@code buildUsersAndLogs()}</li>
   *   <li><strong>스레드 추가</strong>: {@code addThread()}</li>
   *   <li><strong>완료 단계</strong>: {@code finalizeWithTimestamps()}</li>
   * </ol>
   *
   * <h3>사용 패턴:</h3>
   * <pre>{@code
   * TestDataBuilder builder = new TestDataBuilder(entityManager)
   *     .withTimestamps(baseTime, 5)
   *     .withCustomUser(u -> u.email("user@test.com"))
   *     .withCustomLog(l -> l.content("로그").user(user))
   *     .buildUsersAndLogs();
   *
   * Log log = builder.getCurrentData().logs.get(0);
   * TestDataContainer data = builder
   *     .addThread(t -> t.content("로그 연결 스레드").log(log).user(user))
   *     .addThread(t -> t.content("독립 스레드").user(user))
   *     .finalizeWithTimestamps();
   * }</pre>
   *
   * <h3>자동 설정되는 기본값:</h3>
   * <ul>
   *   <li><strong>User</strong>: provider="google", role=USER</li>
   *   <li><strong>Log</strong>: latitude=37.5665, longitude=126.9780 (사용자는 명시적 지정 필수)</li>
   *   <li><strong>Thread</strong>: 사용자는 명시적 지정 필수, 타임스탬프 자동 적용</li>
   * </ul>
   *
   * @see #withTimestamps(LocalDateTime, int)
   * @see #addThread(Function)
   * @see #finalizeWithTimestamps()
   */
  public static class TestDataBuilder {

    private final TestEntityManager entityManager;
    // 커스터마이저 함수들
    private final List<Function<User.UserBuilder, User.UserBuilder>> userCustomizers = new ArrayList<>();
    private final List<Function<Log.LogBuilder, Log.LogBuilder>> logCustomizers = new ArrayList<>();
    private LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
    private int intervalMinutes = 5;
    // 빌드 과정에서 생성된 데이터 (addThread 메소드에서 사용)
    private TestDataContainer currentData = null;

    /**
     * TestDataBuilder를 생성합니다.
     *
     * @param entityManager JPA 테스트용 EntityManager
     */
    public TestDataBuilder(TestEntityManager entityManager) {
      this.entityManager = entityManager;
    }

    /**
     * 현재까지 생성된 데이터 컨테이너를 반환합니다.
     *
     * <p>이 메소드는 {@code buildUsersAndLogs()} 호출 후에만 유효한 데이터를 반환합니다.
     * 주로 중간 단계에서 생성된 User나 Log 엔티티에 접근할 때 사용됩니다.</p>
     *
     * @return 현재까지 생성된 데이터를 담은 컨테이너, 아직 빌드하지 않은 경우 null
     * @see #buildUsersAndLogs()
     */
    public TestDataContainer getCurrentData() {
      return currentData;
    }

    /**
     * 스레드 생성일시 설정을 구성합니다.
     *
     * <p>이 설정은 {@code finalizeWithTimestamps()} 호출 시 스레드들의 생성일시를
     * 자동으로 업데이트하는 데 사용됩니다. 각 스레드는 baseTime에서 시작하여 intervalMinutes 간격으로 생성일이 설정됩니다.</p>
     *
     * @param baseTime        첫 번째 스레드의 생성일시
     * @param intervalMinutes 스레드 간 생성일시 간격(분)
     * @return 이 빌더 인스턴스
     *
     * <h3>사용 예시:</h3>
     * <pre>{@code
     * // 2024년 1월 1일 10시부터 5분 간격으로 설정
     * builder.withTimestamps(LocalDateTime.of(2024, 1, 1, 10, 0, 0), 5);
     *
     * // 생성되는 스레드들의 타임스탬프:
     * // 첫 번째: 2024-01-01 10:00:00
     * // 두 번째: 2024-01-01 10:05:00
     * // 세 번째: 2024-01-01 10:10:00
     * }</pre>
     */
    public TestDataBuilder withTimestamps(LocalDateTime baseTime, int intervalMinutes) {
      this.baseTime = baseTime;
      this.intervalMinutes = intervalMinutes;
      return this;
    }

    /**
     * 커스텀 사용자를 추가합니다.
     *
     * <p>사용자별 고유 필드(email, nickname, socialId)는 반드시 설정해야 하며,
     * provider와 role은 자동으로 기본값이 설정됩니다.</p>
     *
     * @param customizer 사용자 빌더를 커스터마이징하는 함수
     * @return 이 빌더 인스턴스
     *
     * <h3>자동 설정되는 필드:</h3>
     * <ul>
     *   <li>provider: "google"</li>
     *   <li>role: RoleType.USER</li>
     * </ul>
     *
     * <h3>사용 예시:</h3>
     * <pre>{@code
     * builder
     *     .withCustomUser(u -> u
     *         .email("user1@example.com")
     *         .nickname("user1")
     *         .socialId("123456"))
     *     .withCustomUser(u -> u
     *         .email("admin@example.com")
     *         .nickname("admin")
     *         .socialId("999999")
     *         .role(RoleType.ADMIN)); // 기본값 오버라이드
     * }</pre>
     */
    public TestDataBuilder withCustomUser(Function<User.UserBuilder, User.UserBuilder> customizer) {
      this.userCustomizers.add(customizer);
      return this;
    }

    /**
     * 커스텀 로그를 추가합니다.
     *
     * <p>로그의 content와 user는 반드시 설정해야 하며, 위치 정보만 자동으로 기본값이 설정됩니다.</p>
     *
     * @param customizer 로그 빌더를 커스터마이징하는 함수
     * @return 이 빌더 인스턴스
     *
     * <h3>자동 설정되는 필드:</h3>
     * <ul>
     *   <li>latitude: 37.5665 (서울 시청)</li>
     *   <li>longitude: 126.9780 (서울 시청)</li>
     * </ul>
     *
     * <h3>명시적 지정 필수 필드:</h3>
     * <ul>
     *   <li>user: 로그 작성자 (반드시 지정)</li>
     * </ul>
     *
     * <h3>사용 예시:</h3>
     * <pre>{@code
     * builder
     *     .withCustomLog(l -> l
     *         .content("첫 번째 로그")
     *         .user(user1))
     *     .withCustomLog(l -> l
     *         .content("두 번째 로그")
     *         .user(user2));
     * }</pre>
     */
    public TestDataBuilder withCustomLog(Function<Log.LogBuilder, Log.LogBuilder> customizer) {
      this.logCustomizers.add(customizer);
      return this;
    }


    /**
     * 사용자와 로그를 먼저 생성하고 데이터 컨테이너를 초기화합니다.
     *
     * <p>이 메소드는 {@code addThread()} 메소드를 사용하기 전에 반드시 호출해야 합니다.
     * 사용자와 로그를 먼저 생성하여 스레드 생성 시 참조할 수 있도록 합니다.</p>
     *
     * <p>중복 호출해도 안전하며, 이미 생성된 경우 아무 작업을 수행하지 않습니다.</p>
     *
     * @return 이 빌더 인스턴스
     *
     * <h3>사용 예시:</h3>
     * <pre>{@code
     * TestDataBuilder builder = new TestDataBuilder(entityManager)
     *     .withCustomUser(u -> u.email("user@test.com"))
     *     .withCustomLog(l -> l.content("로그"))
     *     .buildUsersAndLogs();
     *
     * // 생성된 데이터에 접근
     * Log log = builder.getCurrentData().logs.get(0);
     * User user = builder.getCurrentData().users.get(0);
     *
     * // 스레드 추가
     * TestDataContainer data = builder
     *     .addThread(t -> t.content("스레드").log(log))
     *     .finalizeWithTimestamps();
     * }</pre>
     */
    public TestDataBuilder buildUsersAndLogs() {
      if (currentData == null) {
        currentData = new TestDataContainer();
      }

      // 사용자들 생성 - 아직 생성되지 않은 것만
      int existingUserCount = currentData.users.size();
      for (int i = existingUserCount; i < userCustomizers.size(); i++) {
        Function<User.UserBuilder, User.UserBuilder> customizer = userCustomizers.get(i);
        User.UserBuilder builder = User.builder()
            .provider(OAuthProvider.GOOGLE)
            .role(RoleType.USER);

        User user = customizer.apply(builder).build();
        entityManager.persistAndFlush(user);
        currentData.users.add(user);
      }

      // 로그들 생성 - 아직 생성되지 않은 것만
      int existingLogCount = currentData.logs.size();
      for (int i = existingLogCount; i < logCustomizers.size(); i++) {
        Function<Log.LogBuilder, Log.LogBuilder> customizer = logCustomizers.get(i);
        Log.LogBuilder builder = Log.builder()
            .latitude(new BigDecimal("37.5665"))
            .longitude(new BigDecimal("126.9780"));

        Log log = customizer.apply(builder).build();
        entityManager.persistAndFlush(log);
        currentData.logs.add(log);
      }

      return this;
    }

    /**
     * 스레드를 생성하고 즉시 데이터베이스에 persist합니다.
     *
     * <p>이 메소드는 각 스레드를 생성하자마자 즉시 데이터베이스에 저장하므로,
     * 다음 스레드를 생성할 때 이전 스레드를 참조할 수 있습니다.</p>
     *
     * <p>사용자는 반드시 명시적으로 지정해야 합니다.</p>
     *
     * @param customizer 스레드 빌더를 커스터마이징하는 함수
     * @return 이 빌더 인스턴스
     *
     * <h3>사용 예시:</h3>
     * <pre>{@code
     * Log log = builder.getCurrentData().logs.get(0);
     *
     * builder
     *     .addThread(t -> t
     *         .content("로그 연결 스레드")
     *         .isPublic(true)
     *         .viewCount(10L)
     *         .log(log))  // 로그에 연결
     *     .addThread(t -> t
     *         .content("독립 스레드")
     *         .isPublic(false)
     *         .viewCount(0L));  // 로그 연결 없음
     * }</pre>
     * @see #buildUsersAndLogs()
     */
    public TestDataBuilder addThread(
        Function<Thread.ThreadBuilder, Thread.ThreadBuilder> customizer) {
      if (currentData == null) {
        buildUsersAndLogs();
      }

      Thread.ThreadBuilder builder = Thread.builder();
      Thread thread = customizer.apply(builder).build();

      entityManager.persistAndFlush(thread);
      currentData.threads.add(thread);

      return this;
    }


    /**
     * 생성일 업데이트와 영속성 컨텍스트 갱신을 포함한 최종 빌드를 수행합니다.
     *
     * <p>이 메소드는 다음 작업을 순차적으로 수행합니다:</p>
     * <ol>
     *   <li>사용자와 로그가 아직 생성되지 않았다면 생성</li>
     *   <li>모든 스레드의 생성일시를 {@code withTimestamps()}로 설정된 값에 따라 업데이트</li>
     *   <li>영속성 컨텍스트를 지워서 이후 조회 시 업데이트된 데이터를 반영</li>
     * </ol>
     *
     * <p>이 메소드 호출 후에는 새로운 스레드를 추가할 수 없습니다.</p>
     *
     * @return 완성된 테스트 데이터 컨테이너
     *
     * <h3>사용 예시:</h3>
     * <pre>{@code
     * TestDataContainer data = builder
     *     .withTimestamps(LocalDateTime.of(2024, 1, 1, 10, 0, 0), 5)
     *     .addThread(t -> t.content("스레드 1"))  // 10:00:00
     *     .addThread(t -> t.content("스레드 2"))  // 10:05:00
     *     .addThread(t -> t.content("스레드 3"))  // 10:10:00
     *     .finalizeWithTimestamps();
     *
     * // 이제 data.threads의 각 스레드는 올바른 생성일시를 가짐
     * }</pre>
     */
    public TestDataContainer finalizeWithTimestamps() {
      if (currentData == null) {
        buildUsersAndLogs();
      }

      for (int i = 0; i < currentData.threads.size(); i++) {
        LocalDateTime threadCreatedAt = baseTime.plusMinutes((long) i * intervalMinutes);
        updateThreadCreatedAt(entityManager, currentData.threads.get(i).getId(), threadCreatedAt);
      }

      entityManager.clear();

      return currentData;
    }

  }


}