package com.spring.careplanservice.careplan.application.facade;

import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
import com.spring.careplanservice.careplan.application.service.command.CarePlanOutboxCommandService;
import com.spring.careplanservice.careplan.application.service.query.CarePlanOutboxQueryService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.SpringDataCarePlanOutboxEventRepository;
import com.spring.careplanservice.careplan.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// care-plan.outbox.relay.enabled=false 로 백그라운드 Relay 스케줄러를 꺼서,
// 이 테스트가 직접 claim()/relay()/revertStuckProcessing()을 호출하는 순서와
// 실제 스케줄러 스레드가 같은 row를 동시에 건드리며 생기는 우발적 경합을 배제한다.
// (다중 인스턴스 경합 자체는 claim_concurrentClaim_onlyOneInstanceWins에서 의도적으로 재현한다)
@SpringBootTest(properties = "care-plan.outbox.relay.enabled=false")
class CarePlanOutboxConcurrencyIntegrationTest extends IntegrationTestSupport {
    /*
    12번(Outbox 운영 안정성) 검증
    1) claim()의 낙관적 락(@Version)이 다중 인스턴스 동시 선점을 실제로 막는지
    2) 오래 방치된 PROCESSING 이벤트를 findStuckProcessing()/revertStuckProcessing()으로
       복구한 뒤 다시 발행까지 이어지는지
    3) stuck 복구 시 재조회 시점에 여전히 stuck 상태인지 재검증해
       그 사이 재선점된 이벤트를 잘못 되돌리지 않는지
    를 실제 Postgres(Testcontainers)로 검증한다.
    */

    @Autowired
    private SpringDataCarePlanOutboxEventRepository springDataRepository;

    @Autowired
    private CarePlanOutboxCommandService carePlanOutboxCommandService;

    @Autowired
    private CarePlanOutboxQueryService carePlanOutboxQueryService;

    @Autowired
    private CarePlanOutboxRelayFacade carePlanOutboxRelayFacade;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CarePlanCompletedEventPort carePlanCompletedEventPort;

    @Test
    @DisplayName("두 인스턴스가 같은 PENDING 이벤트를 동시에 선점하려 하면 한쪽만 성공하고 다른 쪽은 낙관적 락 예외가 발생한다")
    void claim_concurrentClaim_onlyOneInstanceWins() {
        UUID outboxEventId = saveNewPendingEvent();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // 두 인스턴스가 거의 동시에 같은 row를 조회했다고 가정 — 서로 다른 트랜잭션에서
        // 각각 조회하므로 두 복사본 모두 version=0인 상태로 detach 된다.
        CarePlanOutboxEvent instanceACopy = txTemplate.execute(status ->
                springDataRepository.findById(outboxEventId).orElseThrow()
        );

        CarePlanOutboxEvent instanceBCopy = txTemplate.execute(status ->
                springDataRepository.findById(outboxEventId).orElseThrow()
        );

        // 인스턴스 A가 먼저 선점(PROCESSING)에 성공해 커밋 -> version이 증가한다.
        txTemplate.executeWithoutResult(status -> {
            instanceACopy.startProcessing();
            springDataRepository.save(instanceACopy);
        });

        // 인스턴스 B는 오래된(version=0) 복사본을 들고 있으므로,
        // 뒤늦게 같은 선점을 시도하면 낙관적 락 충돌이 발생해야 한다.
        assertThatThrownBy(() ->
                txTemplate.executeWithoutResult(status -> {
                    instanceBCopy.startProcessing();
                    springDataRepository.save(instanceBCopy);
                })
        ).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        CarePlanOutboxEvent finalState = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(finalState.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PROCESSING);
    }

    @Test
    @DisplayName("3회 연속 발행 실패하면 FAILED로 전환되고, 그 이후에는 자동 재시도 대상에서 제외된다")
    void recordFailure_maxRetry_becomesFailed_andExcludedFromFurtherRelay() {
        UUID outboxEventId = saveNewPendingEvent();

        // 3회 연속 실패시켜 FAILED 상태로 만든다.
        carePlanOutboxCommandService.recordFailure(outboxEventId, "1차 실패");
        carePlanOutboxCommandService.recordFailure(outboxEventId, "2차 실패");
        carePlanOutboxCommandService.recordFailure(outboxEventId, "3차 실패");

        CarePlanOutboxEvent failedEvent = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(failedEvent.getStatus()).isEqualTo(CarePlanOutboxEventStatus.FAILED);
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getLastErrorMessage()).isEqualTo("3차 실패");

        // FAILED는 수동 재처리 수단이 없으므로, 이후 relay()를 호출해도
        // findPending() 대상에서 계속 제외되어 상태가 그대로 유지되어야 한다.
        carePlanOutboxRelayFacade.relay();

        CarePlanOutboxEvent stillFailed = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(stillFailed.getStatus()).isEqualTo(CarePlanOutboxEventStatus.FAILED);
    }

    @Test
    @DisplayName("오래 방치된 PROCESSING 이벤트는 findStuckProcessing/revertStuckProcessing으로 PENDING으로 복구된다")
    void findStuckProcessing_thenRevert_recoversStuckRow() {
        UUID outboxEventId = saveNewPendingEvent();

        boolean claimed = carePlanOutboxCommandService.claim(outboxEventId);
        assertThat(claimed).isTrue();

        // 죽은 인스턴스가 선점한 채 오래 방치된 상황을 흉내내기 위해
        // updated_at을 JPA Auditing을 거치지 않고 직접 과거로 되돌린다.
        Instant longAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        jdbcTemplate.update(
                "UPDATE care_plan_schema.p_care_plan_outbox_events SET updated_at = ? WHERE outbox_event_id = ?",
                java.sql.Timestamp.from(longAgo),
                outboxEventId
        );

        Instant threshold = Instant.now().minus(1, ChronoUnit.MINUTES);

        var stuckEvents = carePlanOutboxQueryService.findStuckProcessing(threshold, 100);

        assertThat(stuckEvents)
                .anyMatch(result -> result.outboxEventId().equals(outboxEventId));

        // revert 시 findStuckProcessing()에 사용한 것과 동일한 threshold를 전달해야
        // "지금도 여전히 stuck 상태인지"를 재검증할 수 있다.
        carePlanOutboxCommandService.revertStuckProcessing(outboxEventId, threshold);

        CarePlanOutboxEvent recovered = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);

        // 복구된 이벤트는 다음 relay()에서 정상적으로 다시 발행되어야 한다.
        carePlanOutboxRelayFacade.relay();

        CarePlanOutboxEvent sentEvent = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(sentEvent.getStatus()).isEqualTo(CarePlanOutboxEventStatus.SENT);
    }

    @Test
    @DisplayName("복구(revert) 직전에 다른 인스턴스가 이미 재선점했다면(최신 updatedAt) 되돌리지 않는다 — stuck 복구 race condition 방지")
    void revertStuckProcessing_reclaimedInTheMeantime_doesNotRevert() {
        UUID outboxEventId = saveNewPendingEvent();

        boolean claimed = carePlanOutboxCommandService.claim(outboxEventId);
        assertThat(claimed).isTrue();

        // Relay 스케줄러가 findStuckProcessing() 시점에 계산했던 threshold라고 가정한다.
        // (실제로는 1분 전이지만, 테스트에서는 "과거 시점"이기만 하면 충분하다)
        Instant thresholdComputedDuringFind = Instant.now().minusSeconds(30);

        // findStuckProcessing()과 revertStuckProcessing() 호출 사이에,
        // 다른 인스턴스가 이 row를 정상적으로 복구(PENDING)한 뒤 즉시 재선점(PROCESSING)했다고 가정한다.
        // -> updated_at이 "방금"으로 갱신된다.
        jdbcTemplate.update(
                "UPDATE care_plan_schema.p_care_plan_outbox_events SET status = 'PENDING' WHERE outbox_event_id = ?",
                outboxEventId
        );
        boolean reclaimedByOtherInstance = carePlanOutboxCommandService.claim(outboxEventId);
        assertThat(reclaimedByOtherInstance).isTrue();

        CarePlanOutboxEvent afterReclaim = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(afterReclaim.getUpdatedAt()).isAfter(thresholdComputedDuringFind);

        // 원래 스레드가 (과거에 계산해 둔) threshold로 뒤늦게 복구를 시도한다.
        // updatedAt이 threshold보다 최신이므로 이번엔 절대 되돌려서는 안 된다.
        carePlanOutboxCommandService.revertStuckProcessing(outboxEventId, thresholdComputedDuringFind);

        CarePlanOutboxEvent stillProcessing = springDataRepository.findById(outboxEventId).orElseThrow();
        assertThat(stillProcessing.getStatus())
                .as("다른 인스턴스가 이미 재선점해 정상 처리 중인 이벤트를 되돌리면 안 된다")
                .isEqualTo(CarePlanOutboxEventStatus.PROCESSING);
    }

    private UUID saveNewPendingEvent() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        return springDataRepository.save(event).getId();
    }
}
