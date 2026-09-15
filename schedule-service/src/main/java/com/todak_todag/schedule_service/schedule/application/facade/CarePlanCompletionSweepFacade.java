package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletionEventAppender;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 재매칭 미시도로 CarePlanCompleted가 발행되지 못한 케어플랜을 찾아 보정 발행하는 유스케이스 조합
// 트리거(@Scheduled)는 infrastructure/messaging/CarePlanCompletionSweepScheduler에 존재
@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanCompletionSweepFacade {

    // "이 케어플랜에서 더 일어날 일이 없다"고 보기까지 기다리는 기간
    //
    // Internal API의 N회 호출을 피하기 위해, 자체 DB가 가진 일정/매칭시도의 date로 마지막 활동일을 대신 판정
    // 케어플랜 기간이 30일 고정이라 마지막 활동일은 항상 finishDate 이하이며, 유예기간이 그 오차를 덮음
    private static final int GRACE_PERIOD_DAYS = 14;

    // 한 번의 스윕에서 처리할 최대 케어플랜 수 (릴레이와 같은 취지의 안전장치)
    // 여기서 못 딴 대상은 다음 주기에 다시 잡힘
    private static final int BATCH_SIZE = 100;

    private final ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;
    private final CarePlanCompletionEventAppender carePlanCompletionEventAppender;

    // 한 번의 스윕 주기 전체를 처리
    // CarePlanCompletionSweepScheduler(@Scheduled)가 이 메서드를 반복 호출
    public void sweep() {
        List<UUID> targetCarePlanIds = findTargets();

        if (targetCarePlanIds.isEmpty()) {
            return;
        }

        log.info("[Schedule] CarePlanCompleted 보정 스윕 시작 targetCount={}", targetCarePlanIds.size());

        for (UUID carePlanId : targetCarePlanIds) {
            sweepOne(carePlanId);
        }
    }

    // 보정 대상 케어플랜 조회
    //
    // 조건은 두 가지
    //   (1) 마지막 활동일(일정/매칭시도 date의 최댓값)로부터 유예기간이 지났다 = 종료된 케어플랜
    //   (2) 아직 해소되지 않은 매칭 실패가 남아있다 = 재매칭 미시도로 방치된 케어플랜
    // 여기에 페이로드를 채울 마지막 일정이 있어야 하므로 일정 0건 케어플랜은 쿼리에서 함께 제외
    //
    // "CarePlanCompleted가 아직 적재되지 않았을 것"은 여기서 보지 않고 Appender의 alreadyAppended가 락 안에서 판정
    // 실시간 경로와 같은 멱등 코드를 그대로 태우기 위해서이고, 조회 시점과 적재 시점 사이에 실시간 경로가 끼어들 수 있어 락 밖에서 미리 걸러봐야 소용이 없기 때문
    private List<UUID> findTargets() {
        LocalDate lastActivityThreshold = LocalDate.now().minusDays(GRACE_PERIOD_DAYS);

        return serviceMatchingAttemptCommandRepository.findSweepTargetCarePlanIds(
                lastActivityThreshold,
                BATCH_SIZE
        );
    }

    // 케어플랜 1건 처리
    // Appender가 케어플랜마다 자기 트랜잭션을 열기 때문에, 한 건이 실패해도 앞서 처리한 건은 이미 커밋되어 있음
    private void sweepOne(UUID carePlanId) {
        try {
            carePlanCompletionEventAppender.appendForSweep(carePlanId);
        } catch (Exception e) {
            log.error("[Schedule] CarePlanCompleted 보정 스윕 실패 carePlanId={}", carePlanId, e);
        }
    }
}
