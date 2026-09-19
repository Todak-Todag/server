package com.todak_todag.schedule_service.schedule.application.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletedEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletionEventAppender;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.CarePlanCompletionLockRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.CarePlanServiceResultCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ScheduleOutboxEventCommandRepositoryImpl;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// CarePlanCompleted 보정 스윕 통합 테스트
//
// 검증 대상은 "재매칭 미시도로 방치된 케어플랜을 실제 DB 상태로부터 찾아내 발행까지 이어지는가"
// 브로커 발행은 릴레이의 책임이라 여기서는 아웃박스 적재까지만 확인
//
// 클래스 전체를 비트랜잭션으로 두는 이유: advisory lock과 유니크 제약이 실제 트랜잭션 경계에서만 의미가 있고,
// 동시성 테스트가 서로 다른 트랜잭션 두 개를 필요로 하기 때문
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaConfig.class,
        ServiceScheduleCommandRepositoryImpl.class,
        ServiceMatchingAttemptCommandRepositoryImpl.class,
        CarePlanServiceResultCommandRepositoryImpl.class,
        ScheduleOutboxEventCommandRepositoryImpl.class,
        CarePlanCompletionLockRepositoryImpl.class,
        CarePlanCompletedEventPayloadSerializer.class,
        ScheduleOutboxCommandService.class,
        CarePlanCompletionEventAppender.class,
        CarePlanCompletionSweepFacade.class,
        CarePlanCompletionSweepIntegrationTest.ObjectMapperTestConfig.class
})
class CarePlanCompletionSweepIntegrationTest extends PostgresTestSupport {

    // CarePlanCompletionSweepFacade.GRACE_PERIOD_DAYS와 동일해야함
    private static final int GRACE_PERIOD_DAYS = 14;

    // V1__init.sql의 ux_schedule_outbox_events_care_plan_completed와 동일해야 함
    // 테스트 DB는 flyway가 꺼져 있고 ddl-auto=create-drop이라 마이그레이션이 적용되지 않으므로 여기서 직접 만듦
    private static final String PARTIAL_UNIQUE_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_outbox_events_care_plan_completed
                ON schedule_schema.p_schedule_outbox_events (aggregate_id)
                WHERE event_type = 'CarePlanCompleted'
            """;

    // 아웃박스 슬라이스에는 Jackson 자동 설정이 없어 직렬화기용 ObjectMapper를 직접 등록
    @TestConfiguration
    static class ObjectMapperTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private CarePlanCompletionSweepFacade carePlanCompletionSweepFacade;

    @Autowired
    private CarePlanCompletionEventAppender carePlanCompletionEventAppender;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Autowired
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Autowired
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // 클래스가 비트랜잭션이라 네이티브 쿼리는 이 템플릿으로 직접 트랜잭션을 열어 실행한다
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clear() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.executeWithoutResult(status ->
                entityManager.createNativeQuery(PARTIAL_UNIQUE_INDEX).executeUpdate());

        springDataScheduleOutboxEventRepository.deleteAll();
        springDataCarePlanServiceResultRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();
        springDataServiceMatchingAttemptRepository.deleteAll();
    }

    @Test
    @DisplayName("재매칭 없이 방치된 매칭 실패가 있어도, 마지막 활동일로부터 유예기간이 지나면 스윕이 CarePlanCompleted를 적재한다")
    void 방치된_매칭_실패가_있어도_유예기간이_지나면_스윕이_적재한다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        abandonedFailure(carePlanId, GRACE_PERIOD_DAYS + 10);
        ServiceSchedule finished = finishedSchedule(carePlanId, GRACE_PERIOD_DAYS + 5);
        CarePlanServiceResult result = registerResult(finished);

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        List<ScheduleOutboxEvent> appended = appendedCompletedEvents(carePlanId);
        assertThat(appended).hasSize(1);
        assertThat(appended.getFirst().getPayload()).isEqualTo(
                "{\"carePlanId\":\"" + carePlanId + "\","
                        + "\"serviceResultId\":\"" + result.getServiceResultId() + "\",\"status\":\"COMPLETED\"}"
        );
    }

    @Test
    @DisplayName("스윕이 발행한 케어플랜의 방치된 FAILED는 EXPIRED로 종결되어 다음 스윕에서 다시 잡히지 않는다")
    void 스윕이_발행하면_방치된_FAILED가_EXPIRED로_종결된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceMatchingAttempt abandoned = abandonedFailure(carePlanId, GRACE_PERIOD_DAYS + 10);
        registerResult(finishedSchedule(carePlanId, GRACE_PERIOD_DAYS + 5));

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        ServiceMatchingAttempt reloaded =
                springDataServiceMatchingAttemptRepository.findById(abandoned.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MatchingAttemptStatus.EXPIRED);
        assertThat(springDataServiceMatchingAttemptRepository.findSweepTargetCarePlanIds(
                MatchingAttemptStatus.FAILED,
                LocalDate.now().minusDays(GRACE_PERIOD_DAYS),
                org.springframework.data.domain.PageRequest.of(0, 100)
        )).isEmpty();
    }

    @Test
    @DisplayName("마지막 활동일로부터 유예기간이 아직 지나지 않은 케어플랜은 스윕 대상에서 제외된다")
    void 유예기간이_지나지_않으면_스윕_대상이_아니다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        abandonedFailure(carePlanId, GRACE_PERIOD_DAYS - 1);
        registerResult(finishedSchedule(carePlanId, GRACE_PERIOD_DAYS - 1));

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        assertThat(appendedCompletedEvents(carePlanId)).isEmpty();
    }

    @Test
    @DisplayName("유예기간이 지났어도 아직 진행 중인 일정이 남아있으면 스윕이 발행하지 않는다")
    void 진행_중_일정이_남아있으면_스윕이_발행하지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        abandonedFailure(carePlanId, GRACE_PERIOD_DAYS + 10);
        registerResult(finishedSchedule(carePlanId, GRACE_PERIOD_DAYS + 5));
        pastDatedSchedule(carePlanId, GRACE_PERIOD_DAYS + 3);

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        assertThat(appendedCompletedEvents(carePlanId)).isEmpty();
    }

    @Test
    @DisplayName("이미 CarePlanCompleted가 발행된 케어플랜은 스윕이 중복 적재하지 않는다")
    void 이미_발행된_케어플랜은_스윕이_중복_적재하지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        abandonedFailure(carePlanId, GRACE_PERIOD_DAYS + 10);
        registerResult(finishedSchedule(carePlanId, GRACE_PERIOD_DAYS + 5));

        carePlanCompletionSweepFacade.sweep();
        assertThat(appendedCompletedEvents(carePlanId)).hasSize(1);

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        assertThat(appendedCompletedEvents(carePlanId)).hasSize(1);
    }

    @Test
    @DisplayName("스윕과 실시간 트리거가 같은 케어플랜을 동시에 처리해도 CarePlanCompleted는 한 번만 적재된다")
    void 스윕과_실시간_트리거가_동시에_처리해도_한_번만_적재된다() throws InterruptedException {
        // given
        // 두 경로 모두 "발행하겠다"고 판단하는 상태로 둔다 — 미해소 FAILED가 없어야 실시간 경로도 완료로 보므로,
        // 스윕이 대상을 고른 직후 사용자가 마지막 결과를 등록해 실시간 판정이 함께 도는 상황에 해당
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule finished = finishedSchedule(carePlanId, GRACE_PERIOD_DAYS + 5);
        registerResult(finished);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // when
        executor.submit(() -> runAfter(start, done, () ->
                transactionTemplate.executeWithoutResult(status ->
                        carePlanCompletionEventAppender.appendForSweep(carePlanId))));
        executor.submit(() -> runAfter(start, done, () ->
                transactionTemplate.executeWithoutResult(status ->
                        carePlanCompletionEventAppender.appendIfCarePlanCompleted(
                                springDataServiceScheduleRepository.findById(finished.getId()).orElseThrow()))));

        start.countDown();
        boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(finishedInTime).isTrue();
        assertThat(appendedCompletedEvents(carePlanId)).hasSize(1);
    }

    // 두 스레드가 최대한 같은 시점에 출발하도록 맞춰주고, 예외가 나도 래치는 반드시 내림
    private void runAfter(CountDownLatch start, CountDownLatch done, Runnable task) {
        try {
            start.await();
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // 중복 적재를 막다 발생한 예외는 정상 — 최종 적재 건수로 판정
        } finally {
            done.countDown();
        }
    }

    // 이 케어플랜으로 적재된 CarePlanCompleted 이벤트
    private List<ScheduleOutboxEvent> appendedCompletedEvents(UUID carePlanId) {
        return springDataScheduleOutboxEventRepository.findAll().stream()
                .filter(event -> event.getEventType().equals(CarePlanCompletedEventPort.EVENT_TYPE))
                .filter(event -> event.getAggregateId().equals(carePlanId))
                .toList();
    }

    // 재매칭이 끝내 시도되지 않은 초기 매칭 실패 — 일정 레코드가 생기지 않은 희망 일정
    private ServiceMatchingAttempt abandonedFailure(UUID carePlanId, int daysAgo) {
        return springDataServiceMatchingAttemptRepository.save(
                ServiceMatchingAttempt.record(
                        carePlanId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        LocalDate.now().minusDays(daysAgo),
                        PreferredTimeSlot.MORNING,
                        MatchingAttemptStatus.FAILED,
                        "NO_AVAILABLE_PROVIDER",
                        null,
                        Instant.now()
                )
        );
    }

    // 과거 날짜의 SCHEDULED 일정
    private ServiceSchedule pastDatedSchedule(UUID carePlanId, int daysAgo) {
        ServiceSchedule schedule = springDataServiceScheduleRepository.save(futureSchedule(carePlanId));

        return rewriteDateToPast(schedule, daysAgo);
    }

    // 과거 날짜로 수행 완료된 일정
    private ServiceSchedule finishedSchedule(UUID carePlanId, int daysAgo) {
        ServiceSchedule schedule = futureSchedule(carePlanId);
        schedule.complete();

        return rewriteDateToPast(springDataServiceScheduleRepository.save(schedule), daysAgo);
    }

    // ServiceSchedule.confirm은 미래 날짜만 허용하므로 일단 미래로 만듦
    private ServiceSchedule futureSchedule(UUID carePlanId) {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        return ServiceSchedule.confirm(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                futureDate,
                futureDate.atTime(9, 0),
                futureDate.atTime(10, 0)
        );
    }

    // 저장이 모두 끝난 뒤 date만 과거로 되돌린다 — 엔티티를 다시 save하면 미래 날짜로 덮이므로 반드시 마지막에 진행
    private ServiceSchedule rewriteDateToPast(ServiceSchedule schedule, int daysAgo) {
        transactionTemplate.executeWithoutResult(status ->
                entityManager.createNativeQuery(
                                "update schedule_schema.p_service_schedules set date = :date where service_schedule_id = :id")
                        .setParameter("date", LocalDate.now().minusDays(daysAgo))
                        .setParameter("id", schedule.getId())
                        .executeUpdate());

        return schedule;
    }

    // 수행 결과 등록 — 이게 있어야 COMPLETED 일정이 "끝난" 것으로 판정
    private CarePlanServiceResult registerResult(ServiceSchedule schedule) {
        return springDataCarePlanServiceResultRepository.save(
                CarePlanServiceResult.record(
                        schedule.getId(),
                        LocalDateTime.now().minusHours(2),
                        LocalDateTime.now().minusHours(1),
                        null
                )
        );
    }
}
