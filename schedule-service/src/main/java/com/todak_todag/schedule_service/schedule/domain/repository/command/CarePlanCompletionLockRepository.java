package com.todak_todag.schedule_service.schedule.domain.repository.command;

import java.util.UUID;

// 케어플랜 단위 완료 판정을 직렬화하기 위한 락
// 완료 판정은 "조회 → 판단 → 적재"라서 락 없이는 같은 케어플랜의 마지막 두 일정이 동시에 끝났을 때
// 양쪽 모두 서로를 미완료로 보고 빠져나가 CarePlanCompleted가 영영 적재되지 않을 수 있음
public interface CarePlanCompletionLockRepository {

    // 현재 트랜잭션이 끝날 때까지 해당 케어플랜에 대한 락을 점유 (이미 점유 중이면 해제될 때까지 대기)
    void lockForCompletionCheck(UUID carePlanId);
}
