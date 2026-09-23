package com.todak_todag.schedule_service.schedule.application.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletedEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletionEventAppender;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.CarePlanCompletionLockRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.CarePlanServiceResultCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ScheduleOutboxEventCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceMatchingAttemptCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceScheduleCommandRepositoryImpl;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

// 서비스 일정 변경 동시 요청 통합 테스트
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
        CarePlanCompletionEventAppender.class,
        ServiceScheduleValidator.class,
        ServiceScheduleCommandService.class,
        ServiceScheduleRescheduleConcurrencyIntegrationTest.ObjectMapperTestConfig.class
})
class ServiceScheduleRescheduleConcurrencyIntegrationTest extends PostgresTestSupport {

    // 아웃박스 슬라이스에는 Jackson 자동 설정이 없어 직렬화기용 ObjectMapper를 직접 등록
    // 페이로드에 LocalDate가 있어 운영과 같은 ISO 문자열로 나오도록 시간 모듈까지 맞춤
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
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Autowired
    private SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Autowired
    private SpringDataScheduleOutboxEventRepository springDataScheduleOutboxEventRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    // 클래스가 비트랜잭션이라 각 스레드가 이 템플릿으로 자기 트랜잭션을 열게 됨
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clear() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);

        springDataScheduleOutboxEventRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();
        springDataServiceMatchingAttemptRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 일정에 변경 요청이 동시에 두 번 들어와도 하나만 접수되고 RESCHEDULING은 1건만 생긴다")
    void 동시에_두_번_변경_요청하면_하나만_접수된다() throws InterruptedException {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);
        recordMatchedAttempt(schedule);

        LocalDate requestedDate = schedule.getDate().plusDays(1);

        ConcurrentLinkedQueue<ScheduleStatus> accepted = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Throwable> rejected = new ConcurrentLinkedQueue<>();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // when
        // 두 스레드가 각자 트랜잭션을 열고 같은 일정에 동시에 변경을 요청
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> runAfter(start, done, accepted, rejected, () ->
                    reschedule(schedule, requestedDate, carePlanId, patientId)));
        }

        start.countDown();
        boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(finishedInTime).isTrue();

        // 한 쪽만 접수되고, 늦게 락을 잡은 쪽은 RESCHEDULING을 읽어 기존 400으로 거절
        assertThat(accepted).containsExactly(ScheduleStatus.RESCHEDULING);
        assertThat(rejected).hasSize(1);
        assertThat(rejected.peek())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING);

        // 매칭 결과 수신 경로(findRescheduling)가 보게 될 값 — 2건이면 영구 DLQ로 가던 자리
        assertThat(reschedulingSchedules(schedule)).hasSize(1);
        assertThat(reMatchEvents(schedule)).hasSize(1);
    }

    @Test
    @DisplayName("단독 변경 요청은 기존대로 RESCHEDULING으로 접수되고 ProviderReMatched가 적재된다")
    void 단독_변경_요청은_정상_접수된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceSchedule schedule = scheduledSchedule(carePlanId, 5);
        recordMatchedAttempt(schedule);

        LocalDate requestedDate = schedule.getDate().plusDays(1);

        // when
        ServiceScheduleRescheduleResult result = transactionTemplate.execute(status ->
                reschedule(schedule, requestedDate, carePlanId, patientId));

        // then
        assertThat(result).isNotNull();
        assertThat(result.serviceScheduleId()).isEqualTo(schedule.getId());
        assertThat(result.status()).isEqualTo(ScheduleStatus.RESCHEDULING);

        assertThat(springDataServiceScheduleRepository.findById(schedule.getId()))
                .get()
                .extracting(ServiceSchedule::getStatus)
                .isEqualTo(ScheduleStatus.RESCHEDULING);

        List<ScheduleOutboxEvent> events = reMatchEvents(schedule);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getPayload()).contains("\"date\":\"" + requestedDate + "\"");
    }

    // 두 스레드가 최대한 같은 시점에 출발하도록 맞추고, 접수/거절 결과를 나눠 담음
    private void runAfter(
            CountDownLatch start,
            CountDownLatch done,
            ConcurrentLinkedQueue<ScheduleStatus> accepted,
            ConcurrentLinkedQueue<Throwable> rejected,
            Supplier<ServiceScheduleRescheduleResult> task
    ) {
        try {
            start.await();
            accepted.add(transactionTemplate.execute(status -> task.get()).status());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            rejected.add(e);
        } finally {
            done.countDown();
        }
    }

    private ServiceScheduleRescheduleResult reschedule(
            ServiceSchedule schedule,
            LocalDate requestedDate,
            UUID carePlanId,
            UUID patientId
    ) {
        return serviceScheduleCommandService.reschedule(
                new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId),
                new CarePlanPort.CarePlanRange(carePlanId, schedule.getDate().plusDays(20), patientId)
        );
    }

    // 매칭 결과 수신 경로와 같은 기준으로 RESCHEDULING 일정을 조회
    private List<ServiceSchedule> reschedulingSchedules(ServiceSchedule schedule) {
        return springDataServiceScheduleRepository.findByServicePreferenceIdAndStatusAndDeletedAtIsNull(
                schedule.getServicePreferenceId(),
                ScheduleStatus.RESCHEDULING
        );
    }

    // 이 일정으로 적재된 ProviderReMatched 이벤트
    private List<ScheduleOutboxEvent> reMatchEvents(ServiceSchedule schedule) {
        return springDataScheduleOutboxEventRepository.findAll().stream()
                .filter(event -> event.getEventType().equals(ProviderReMatchEventPort.EVENT_TYPE))
                .filter(event -> event.getAggregateId().equals(schedule.getId()))
                .toList();
    }

    // 변경 가능한(시작 24시간 이후) SCHEDULED 일정
    private ServiceSchedule scheduledSchedule(UUID carePlanId, int plusDays) {
        LocalDate date = LocalDate.now().plusDays(plusDays);

        return springDataServiceScheduleRepository.save(
                ServiceSchedule.confirm(
                        carePlanId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        date,
                        date.atTime(9, 0),
                        date.atTime(10, 0)
                )
        );
    }

    // 이벤트 페이로드의 regionId/provideServiceId 출처가 되는 매칭 시도 기록
    private void recordMatchedAttempt(ServiceSchedule schedule) {
        springDataServiceMatchingAttemptRepository.save(
                ServiceMatchingAttempt.record(
                        schedule.getCarePlanId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        schedule.getServicePreferenceId(),
                        schedule.getServiceOfferingId(),
                        schedule.getDate(),
                        null,
                        MatchingAttemptStatus.MATCHED,
                        null,
                        Instant.now(),
                        null
                )
        );
    }
}
