package com.todak_todag.schedule_service.schedule.application.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletedEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletionEventAppender;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// CarePlanCompleted 실시간 트리거 동시 처리 통합 테스트
//
// 같은 케어플랜의 마지막 두 일정이 서로 다른 트랜잭션에서 거의 동시에 결말날 때,
// 양쪽이 서로를 "아직 미완료"로 읽어 아무도 발행하지 않거나(조기 반환) 양쪽 모두 발행하는 일이 없는지 검증
// 스윕 vs 실시간 조합은 CarePlanCompletionSweepIntegrationTest가 담당하며, 여기서는 실시간 vs 실시간만 다룸
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
        ProviderReMatchEventPayloadSerializer.class,
        ScheduleOutboxCommandService.class,
        ServiceScheduleValidator.class,
        CarePlanCompletionEventAppender.class,
        ServiceResultCommandService.class,
        ServiceScheduleCommandService.class,
        CarePlanCompletionConcurrencyIntegrationTest.ObjectMapperTestConfig.class
})
class CarePlanCompletionConcurrencyIntegrationTest extends PostgresTestSupport {

    // 두 스레드가 끝나기를 기다리는 상한 — 락이 풀리지 않으면 여기서 걸림
    private static final int AWAIT_TIMEOUT_SECONDS = 30;

    // V1__init.sql의 ux_schedule_outbox_events_care_plan_completed와 동일해야 함
    // 테스트 DB는 flyway가 꺼져 있고 ddl-auto=create-drop이라 마이그레이션이 적용되지 않으므로 여기서 직접 만듦
    private static final String PARTIAL_UNIQUE_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_schedule_outbox_events_care_plan_completed
                ON schedule_schema.p_schedule_outbox_events (aggregate_id)
                WHERE event_type = 'CarePlanCompleted'
            """;

    // 아웃박스 슬라이스에는 Jackson 자동 설정이 없어 직렬화기용 ObjectMapper를 직접 등록
    // ProviderReMatched 페이로드에 LocalDate가 있어 운영과 같은 ISO 문자열로 나오도록 시간 모듈까지 맞춤
    @TestConfiguration
    static class ObjectMapperTestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    @Autowired
    private ServiceResultCommandService serviceResultCommandService;

    @Autowired
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Autowired
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    // 클래스가 비트랜잭션이라 네이티브 쿼리는 이 템플릿으로 직접 트랜잭션을 열어 실행
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
    @DisplayName("같은 케어플랜의 마지막 두 일정에 수행 결과가 동시에 등록돼도 CarePlanCompleted는 한 번만 적재된다")
    void 마지막_두_일정의_결과가_동시에_등록돼도_한_번만_적재된다() throws InterruptedException {
        // given
        // 서로 다른 희망 일정에서 나온 두 일정이 모두 COMPLETED이지만 결과가 없는 상태 —
        // 각각 "이 일정만 결과가 등록되면 케어플랜 완료"인 마지막 후보
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlier = completedSchedule(carePlanId, 2);
        ServiceSchedule later = completedSchedule(carePlanId, 3);

        UUID earlierProviderId = UUID.randomUUID();
        UUID laterProviderId = UUID.randomUUID();

        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // when
        executor.submit(() -> runAfter(start, done, failures, () ->
                serviceResultCommandService.register(registerCommand(earlier.getId(), earlierProviderId), earlierProviderId)));
        executor.submit(() -> runAfter(start, done, failures, () ->
                serviceResultCommandService.register(registerCommand(later.getId(), laterProviderId), laterProviderId)));

        start.countDown();
        boolean finishedInTime = done.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(finishedInTime).isTrue();

        // 락에 걸려 대기한 쪽도 예외 없이 자기 트랜잭션을 커밋했는지 — 결과 2건이 모두 남아야 함
        // 한쪽이 유니크 제약에 걸려 롤백됐다면 결과가 1건만 남아 여기서 걸림
        assertThat(failures).isEmpty();
        assertThat(springDataCarePlanServiceResultRepository.findAll()).hasSize(2);

        // 페이로드 기준 일정은 finished_at이 가장 늦은 later — 그 일정에 등록된 결과 ID가 실려야 함
        List<ScheduleOutboxEvent> appended = appendedCompletedEvents(carePlanId);
        assertThat(appended).hasSize(1);
        assertThat(appended.getFirst().getPayload()).isEqualTo(
                "{\"carePlanId\":\"" + carePlanId + "\","
                        + "\"serviceResultId\":\"" + resultIdOf(later) + "\",\"status\":\"COMPLETED\"}"
        );
    }

    @Test
    @DisplayName("마지막 두 일정이 수행 결과 등록과 일정 취소로 동시에 결말나도 CarePlanCompleted는 한 번만 적재된다")
    void 결과_등록과_일정_취소가_동시에_일어나도_한_번만_적재된다() throws InterruptedException {
        // given
        // 트리거가 서로 다른 커맨드(07번 결과 등록 / 04번 일정 취소)인 조합 —
        // 두 경로가 같은 케어플랜의 마지막 남은 일정을 각각 끝내는 상황
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule completed = completedSchedule(carePlanId, 2);
        ServiceSchedule scheduled = scheduledSchedule(carePlanId, 3);

        UUID providerId = UUID.randomUUID();
        CarePlanPort.CarePlanRange carePlanRange =
                new CarePlanPort.CarePlanRange(carePlanId, LocalDate.now().plusDays(10), patientId);

        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // when
        executor.submit(() -> runAfter(start, done, failures, () ->
                serviceResultCommandService.register(registerCommand(completed.getId(), providerId), providerId)));
        executor.submit(() -> runAfter(start, done, failures, () ->
                serviceScheduleCommandService.cancel(
                        new ServiceScheduleCancelCommand(scheduled.getId(), "개인 사정으로 취소합니다", patientId),
                        carePlanRange
                )));

        start.countDown();
        boolean finishedInTime = done.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(finishedInTime).isTrue();
        assertThat(failures).isEmpty();

        // 두 트랜잭션 모두 자기 상태 변경을 커밋했는지
        assertThat(springDataCarePlanServiceResultRepository.findAll()).hasSize(1);
        assertThat(springDataServiceScheduleRepository.findById(scheduled.getId()).orElseThrow().getStatus())
                .isEqualTo(ScheduleStatus.CANCELED);

        // 페이로드 기준 일정은 finished_at이 더 늦은 취소 일정 — 수행된 적이 없어 serviceResultId는 null
        List<ScheduleOutboxEvent> appended = appendedCompletedEvents(carePlanId);
        assertThat(appended).hasSize(1);
        assertThat(appended.getFirst().getPayload()).isEqualTo(
                "{\"carePlanId\":\"" + carePlanId + "\",\"serviceResultId\":null,\"status\":\"CANCELED\"}"
        );
    }

    // 두 스레드가 최대한 같은 시점에 출발하도록 맞춰주고, 예외는 수집하되 래치는 반드시 내림
    private void runAfter(CountDownLatch start, CountDownLatch done, ConcurrentLinkedQueue<Throwable> failures, Runnable task) {
        try {
            start.await();
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            failures.add(e);
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

    private UUID resultIdOf(ServiceSchedule schedule) {
        return springDataCarePlanServiceResultRepository
                .findByServiceScheduleIdAndDeletedAtIsNull(schedule.getId())
                .map(CarePlanServiceResult::getServiceResultId)
                .orElseThrow();
    }

    // 수행 완료 처리까지 끝났지만 결과는 아직 등록되지 않은 일정 — 결과가 등록돼야 비로소 "끝난" 일정이 됨
    private ServiceSchedule completedSchedule(UUID carePlanId, int daysFromNow) {
        ServiceSchedule schedule = futureSchedule(carePlanId, daysFromNow);
        schedule.complete();

        return springDataServiceScheduleRepository.save(schedule);
    }

    // 아직 예정 상태인 일정 — 서비스 일정 취소의 대상
    private ServiceSchedule scheduledSchedule(UUID carePlanId, int daysFromNow) {
        return springDataServiceScheduleRepository.save(futureSchedule(carePlanId, daysFromNow));
    }

    // 취소 마감(시작 24시간 전)에 걸리지 않도록 이틀 이상 뒤로 잡음
    // daysFromNow가 클수록 finished_at이 늦어 "마지막 일정" 선정에서 뒤로 옴
    private ServiceSchedule futureSchedule(UUID carePlanId, int daysFromNow) {
        LocalDate date = LocalDate.now().plusDays(daysFromNow);

        return ServiceSchedule.confirm(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
    }

    private ServiceResultRegisterCommand registerCommand(UUID serviceScheduleId, UUID requesterId) {
        return new ServiceResultRegisterCommand(
                serviceScheduleId,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                null,
                requesterId
        );
    }
}
