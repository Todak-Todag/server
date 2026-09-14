package com.todak_todag.schedule_service.schedule.infrastructure.messaging;

import com.todak_todag.schedule_service.schedule.application.facade.CarePlanCompletionSweepFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// CarePlanCompleted 보정 스윕의 트리거
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "schedule.care-plan.completion-sweep.enabled", havingValue = "true", matchIfMissing = true)
public class CarePlanCompletionSweepScheduler {

    private final CarePlanCompletionSweepFacade carePlanCompletionSweepFacade;

    // 주기가 fixedDelay가 아니라 cron인 이유는 판정 기준이 날짜 단위(마지막 활동일 + 유예기간)이기 때문
    // 하루에 한 번이면 충분하고, 트래픽이 적은 새벽에 돌림
    @Scheduled(cron = "${schedule.care-plan.completion-sweep.cron:0 0 4 * * *}")
    public void sweep() {
        carePlanCompletionSweepFacade.sweep();
    }
}
