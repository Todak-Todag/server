package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchFailedEvent;
import com.todak_todag.schedule_service.schedule.application.event.ProviderMatchedEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceMatchingAttemptCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceScheduleCommandRepositoryImpl;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 매칭 이벤트 중복 수신 방어 통합 테스트
// 테스트 DB는 flyway가 꺼져 있고 ddl-auto=create-drop이라, V2의 부분 유니크 인덱스를 여기서 직접 만들어 검증
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaConfig.class,
        ServiceScheduleCommandRepositoryImpl.class,
        ServiceMatchingAttemptCommandRepositoryImpl.class,
        ServiceMatchingCommandService.class
})
class ServiceMatchingDuplicateEventConcurrencyIntegrationTest extends PostgresTestSupport {

    // V2__add_service_matching_attempt_unique_index.sql과 동일해야 함
    private static final String MATCHED_UNIQUE_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_p_service_matching_attempts_matched
                ON schedule_schema.p_service_matching_attempts (service_preference_id, service_offering_id, date, matched_at)
                WHERE status = 'MATCHED' AND deleted_at IS NULL
            """;

    private static final String FAILED_UNIQUE_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_p_service_matching_attempts_failed
                ON schedule_schema.p_service_matching_attempts (service_preference_id, date, failed_at)
                WHERE status = 'FAILED' AND deleted_at IS NULL
            """;

    @Autowired
    private ServiceMatchingCommandService serviceMatchingCommandService;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // 클래스가 비트랜잭션이라 각 스레드가 이 템플릿으로 자기 트랜잭션을 열게 됨
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);

        springDataServiceScheduleRepository.deleteAll();
        springDataServiceMatchingAttemptRepository.deleteAll();

        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery(MATCHED_UNIQUE_INDEX).executeUpdate();
            entityManager.createNativeQuery(FAILED_UNIQUE_INDEX).executeUpdate();
        });
    }

    @Test
    @DisplayName("완전히 동일한 ProviderMatched가 동시에 두 번 수신돼도 이력과 일정이 하나씩만 생긴다")
    void 동일한_매칭_이벤트를_동시에_수신해도_한_번만_반영된다() throws InterruptedException {
        // given
        // 두 스레드가 같은 이벤트 인스턴스를 쓰므로 matchedAt까지 완전히 동일
        ProviderMatchedEvent event = matchedEvent(UUID.randomUUID(), UUID.randomUUID(), 5, Instant.now());

        // when
        List<Throwable> failures = runConcurrently(() -> serviceMatchingCommandService.applyMatched(event));

        // then
        // 늦게 도착한 쪽은 alreadyApplied로 skip되거나(직렬화된 경우) 유니크 인덱스에 막혀 롤백되며,
        // 어느 쪽이든 최종 상태는 이력 1건 + 일정 1건이어야 함
        assertThat(failures).hasSizeLessThanOrEqualTo(1);
        assertThat(attemptsOf(event.servicePreferenceId())).hasSize(1);
        assertThat(schedulesOf(event.servicePreferenceId())).hasSize(1);
    }

    @Test
    @DisplayName("완전히 동일한 ProviderMatchFailed가 동시에 두 번 수신돼도 실패 이력이 한 건만 생긴다")
    void 동일한_매칭_실패_이벤트를_동시에_수신해도_한_번만_반영된다() throws InterruptedException {
        // given
        ProviderMatchFailedEvent event = matchFailedEvent(UUID.randomUUID(), 5, Instant.now());

        // when
        List<Throwable> failures = runConcurrently(() -> serviceMatchingCommandService.applyMatchFailed(event));

        // then
        assertThat(failures).hasSizeLessThanOrEqualTo(1);
        assertThat(attemptsOf(event.servicePreferenceId())).hasSize(1);
    }

    @Test
    @DisplayName("마이크로초 미만으로만 다른 재전송은 이미 중복으로 걸러진다")
    void 마이크로초_미만_차이는_중복으로_판정된다() {
        // given
        // matched_at은 TIMESTAMPTZ(마이크로초)라, Instant의 나노초 자리는 DB에 닿기 전에 잘려나감
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        Instant matchedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        ProviderMatchedEvent first = matchedEvent(servicePreferenceId, serviceOfferingId, 5, matchedAt);
        ProviderMatchedEvent resent = matchedEvent(servicePreferenceId, serviceOfferingId, 5, matchedAt.plusNanos(1));

        // when
        transactionTemplate.executeWithoutResult(status -> serviceMatchingCommandService.applyMatched(first));
        transactionTemplate.executeWithoutResult(status -> serviceMatchingCommandService.applyMatched(resent));

        // then
        assertThat(attemptsOf(servicePreferenceId)).hasSize(1);
        assertThat(schedulesOf(servicePreferenceId)).hasSize(1);
    }

    @Test
    @DisplayName("마이크로초 이상 다른 재전송은 별개 매칭으로 처리된다 — eventId가 없어 남아있는 한계")
    void 타임스탬프가_다르면_별개의_매칭으로_처리된다() {
        // given
        // 논리적으로는 같은 매칭 결과인데 matchedAt이 1밀리초 어긋난 경우
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        Instant matchedAt = Instant.now();

        ProviderMatchedEvent first = matchedEvent(servicePreferenceId, serviceOfferingId, 5, matchedAt);
        ProviderMatchedEvent resent = matchedEvent(servicePreferenceId, serviceOfferingId, 5, matchedAt.plusMillis(1));

        // when
        transactionTemplate.executeWithoutResult(status -> serviceMatchingCommandService.applyMatched(first));
        transactionTemplate.executeWithoutResult(status -> serviceMatchingCommandService.applyMatched(resent));

        // then
        // 현재 동작을 고정하는 특성화 테스트 — 통과가 곧 "막고 있다"는 뜻이 아님
        // 대체 키에 matchedAt이 들어가는 한 이 경로는 수신 측 단독으로 닫을 수 없고, 정상 재매칭과도 구별되지 않음
        // provider-service가 페이로드에 eventId를 실어주면 그 값으로 판정하도록 바꾸고 이 테스트는 "중복이면 1건"으로 뒤집혀야 함
        //
        // 실제 재전송 경로(아웃박스 릴레이 재시도/브로커 재전달/DLQ 재투입)는 provider-service가
        // 저장된 페이로드를 그대로 재발행하므로 matchedAt이 바뀌지 않아, 이 시나리오에 닿지 않음
        assertThat(attemptsOf(servicePreferenceId)).hasSize(2);
        assertThat(schedulesOf(servicePreferenceId)).hasSize(2);
    }

    @Test
    @DisplayName("같은 희망 일정이 실제로 재매칭되면 새 이력과 새 일정이 정상 생성된다")
    void 정상_재매칭은_여전히_반영된다() {
        // given
        // 초기 매칭 — 이력 1건 + SCHEDULED 일정 1건
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        ProviderMatchedEvent initial = matchedEvent(servicePreferenceId, serviceOfferingId, 5, Instant.now());

        transactionTemplate.executeWithoutResult(status -> serviceMatchingCommandService.applyMatched(initial));

        // 사용자가 일정 변경을 요청해 기존 일정이 RESCHEDULING으로 넘어간 상태
        ServiceSchedule original = schedulesOf(servicePreferenceId).getFirst();
        transactionTemplate.executeWithoutResult(status -> {
            ServiceSchedule managed = springDataServiceScheduleRepository.findById(original.getId()).orElseThrow();
            managed.rescheduling();
            springDataServiceScheduleRepository.save(managed);
        });

        // when
        // provider가 매칭을 다시 돌려 다른 날짜·다른 matchedAt으로 성사시킨 결과
        ProviderMatchedEvent rematched =
                matchedEvent(servicePreferenceId, serviceOfferingId, 7, initial.matchedAt().plusSeconds(60));

        transactionTemplate.executeWithoutResult(status -> serviceMatchingCommandService.applyMatched(rematched));

        // then
        // 중복으로 오판되지 않고 새 이력이 쌓이고, 기존 일정은 CHANGED로 마감된 뒤 새 일정이 생김
        assertThat(attemptsOf(servicePreferenceId)).hasSize(2);

        List<ServiceSchedule> schedules = schedulesOf(servicePreferenceId);
        assertThat(schedules).hasSize(2);
        assertThat(schedules)
                .extracting(ServiceSchedule::getStatus)
                .containsExactlyInAnyOrder(ScheduleStatus.CHANGED, ScheduleStatus.SCHEDULED);
        assertThat(schedules)
                .filteredOn(schedule -> schedule.getStatus() == ScheduleStatus.SCHEDULED)
                .extracting(ServiceSchedule::getDate)
                .containsExactly(rematched.date());
    }

    // 두 스레드가 최대한 같은 시점에 출발하도록 맞추고, 실패한 쪽의 예외를 모음
    private List<Throwable> runConcurrently(Runnable task) throws InterruptedException {
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    transactionTemplate.executeWithoutResult(status -> task.run());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    failures.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(finishedInTime).isTrue();

        return List.copyOf(failures);
    }

    private ProviderMatchedEvent matchedEvent(
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            int plusDays,
            Instant matchedAt
    ) {
        LocalDate date = LocalDate.now().plusDays(plusDays);

        return new ProviderMatchedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                serviceOfferingId,
                date,
                date.atTime(9, 0),
                matchedAt
        );
    }

    private ProviderMatchFailedEvent matchFailedEvent(UUID servicePreferenceId, int plusDays, Instant failedAt) {
        return new ProviderMatchFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                LocalDate.now().plusDays(plusDays),
                null,
                "NO_AVAILABLE_PROVIDER",
                failedAt
        );
    }

    private List<ServiceMatchingAttempt> attemptsOf(UUID servicePreferenceId) {
        return springDataServiceMatchingAttemptRepository.findAll().stream()
                .filter(attempt -> attempt.getServicePreferenceId().equals(servicePreferenceId))
                .filter(attempt -> attempt.getStatus() != MatchingAttemptStatus.EXPIRED)
                .toList();
    }

    private List<ServiceSchedule> schedulesOf(UUID servicePreferenceId) {
        return springDataServiceScheduleRepository.findAll().stream()
                .filter(schedule -> schedule.getServicePreferenceId().equals(servicePreferenceId))
                .toList();
    }
}
