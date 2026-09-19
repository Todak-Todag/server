package com.todak_todag.schedule_service.schedule.application.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.MatchingAttemptRetryCommand;
import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;
import com.todak_todag.schedule_service.schedule.application.support.MatchingAttemptValidator;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleOutboxEvent;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataScheduleOutboxEventRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ScheduleOutboxEventCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceMatchingAttemptCommandRepositoryImpl;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 재매칭 시도(16번) 동시 요청 통합 테스트
// 이중 클릭으로 ProviderReMatched가 2건 적재되던 자리를 대상 로우 쓰기 락으로 막았는지 검증
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaConfig.class,
        ServiceMatchingAttemptCommandRepositoryImpl.class,
        ScheduleOutboxEventCommandRepositoryImpl.class,
        ProviderReMatchEventPayloadSerializer.class,
        ScheduleOutboxCommandService.class,
        ServiceScheduleValidator.class,
        MatchingAttemptValidator.class,
        ServiceMatchingAttemptCommandService.class,
        MatchingAttemptRetryConcurrencyIntegrationTest.ObjectMapperTestConfig.class
})
class MatchingAttemptRetryConcurrencyIntegrationTest extends PostgresTestSupport {

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
    private ServiceMatchingAttemptCommandService serviceMatchingAttemptCommandService;

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
        springDataServiceMatchingAttemptRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 매칭 시도로 재매칭 요청이 동시에 두 번 들어와도 ProviderReMatched는 1건만 적재된다")
    void 동시에_두_번_재매칭_요청하면_하나만_접수된다() throws InterruptedException {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceMatchingAttempt attempt = failedAttempt(carePlanId);

        LocalDate requestedDate = LocalDate.now().plusDays(3);

        ConcurrentLinkedQueue<UUID> accepted = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Throwable> rejected = new ConcurrentLinkedQueue<>();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // when
        // 두 스레드가 각자 트랜잭션을 열고 같은 매칭 시도에 동일 파라미터로 동시에 재매칭을 요청 (이중 클릭)
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> runAfter(start, done, accepted, rejected, () ->
                    retry(attempt, requestedDate, PreferredTimeSlot.MORNING, carePlanId, patientId)));
        }

        start.countDown();
        boolean finishedInTime = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(finishedInTime).isTrue();

        // 한 쪽만 접수되고, 늦게 락을 잡은 쪽은 이미 적재된 아웃박스를 읽어 기존 409로 거절
        assertThat(accepted).containsExactly(attempt.getId());
        assertThat(rejected).hasSize(1);
        assertThat(rejected.peek())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED);

        // 릴레이가 발행하게 될 값 — 2건이면 provider-service가 같은 건을 두 번 재매칭하던 자리
        assertThat(reMatchEvents(attempt.getId())).hasSize(1);
    }

    @Test
    @DisplayName("간격을 두고 같은 매칭 시도로 다시 요청하면 파라미터가 달라도 409로 거절된다")
    void 순차로_다시_요청해도_한_번만_접수된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceMatchingAttempt attempt = failedAttempt(carePlanId);

        transactionTemplate.execute(status ->
                retry(attempt, LocalDate.now().plusDays(3), PreferredTimeSlot.MORNING, carePlanId, patientId));

        // when & then
        // 재시도는 matchingAttemptId당 1회 — 같은 대상으로의 두 번째 요청은 시간 간격·파라미터와 무관하게 409
        assertThatThrownBy(() -> transactionTemplate.execute(status ->
                retry(attempt, LocalDate.now().plusDays(7), PreferredTimeSlot.AFTERNOON, carePlanId, patientId)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED);

        assertThat(reMatchEvents(attempt.getId())).hasSize(1);
    }

    @Test
    @DisplayName("서로 다른 실패 건은 각기 다른 사유로 재매칭해도 각각 접수된다")
    void 다른_매칭_시도는_각각_접수된다() {
        // given
        // 재시도가 또 실패하면 새 FAILED 이력이 생기고, 정상적인 재시도는 그 새 matchingAttemptId로 들어옴
        UUID carePlanId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        ServiceMatchingAttempt first = failedAttempt(carePlanId);
        ServiceMatchingAttempt second = failedAttempt(carePlanId);

        // when
        MatchingAttemptRetryResult firstResult = transactionTemplate.execute(status ->
                retry(first, LocalDate.now().plusDays(3), PreferredTimeSlot.MORNING, carePlanId, patientId));

        MatchingAttemptRetryResult secondResult = transactionTemplate.execute(status ->
                retry(second, LocalDate.now().plusDays(9), PreferredTimeSlot.AFTERNOON, carePlanId, patientId));

        // then
        assertThat(firstResult.matchingAttemptId()).isEqualTo(first.getId());
        assertThat(secondResult.matchingAttemptId()).isEqualTo(second.getId());

        // 락은 대상 매칭 시도 로우에만 걸리므로 서로 다른 실패 건끼리는 막히지 않음
        assertThat(reMatchEvents(first.getId())).hasSize(1);
        assertThat(reMatchEvents(second.getId())).hasSize(1);

        List<ScheduleOutboxEvent> events = reMatchEvents(first.getId());
        assertThat(events.getFirst().getPayload()).contains("\"date\":\"" + LocalDate.now().plusDays(3) + "\"");
    }

    // 두 스레드가 최대한 같은 시점에 출발하도록 맞추고, 접수/거절 결과를 나눠 담음
    private void runAfter(
            CountDownLatch start,
            CountDownLatch done,
            ConcurrentLinkedQueue<UUID> accepted,
            ConcurrentLinkedQueue<Throwable> rejected,
            Supplier<MatchingAttemptRetryResult> task
    ) {
        try {
            start.await();
            accepted.add(transactionTemplate.execute(status -> task.get()).matchingAttemptId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            rejected.add(e);
        } finally {
            done.countDown();
        }
    }

    private MatchingAttemptRetryResult retry(
            ServiceMatchingAttempt attempt,
            LocalDate requestedDate,
            PreferredTimeSlot preferredTimeSlot,
            UUID carePlanId,
            UUID patientId
    ) {
        return serviceMatchingAttemptCommandService.retry(
                new MatchingAttemptRetryCommand(attempt.getId(), requestedDate, preferredTimeSlot, patientId),
                new CarePlanPort.CarePlanRange(carePlanId, LocalDate.now().plusDays(20), patientId)
        );
    }

    // 이 매칭 시도로 적재된 ProviderReMatched 이벤트
    private List<ScheduleOutboxEvent> reMatchEvents(UUID matchingAttemptId) {
        return springDataScheduleOutboxEventRepository.findAll().stream()
                .filter(event -> event.getEventType().equals(ProviderReMatchEventPort.EVENT_TYPE))
                .filter(event -> event.getAggregateId().equals(matchingAttemptId))
                .toList();
    }

    // 재시도 대상이 되는 초기 매칭 실패 이력 (일정 레코드는 생기지 않음 — 14번)
    private ServiceMatchingAttempt failedAttempt(UUID carePlanId) {
        return springDataServiceMatchingAttemptRepository.save(
                ServiceMatchingAttempt.record(
                        carePlanId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        LocalDate.now().plusDays(2),
                        PreferredTimeSlot.MORNING,
                        MatchingAttemptStatus.FAILED,
                        "제공 가능한 서비스 제공자가 없습니다.",
                        null,
                        Instant.now()
                )
        );
    }
}
