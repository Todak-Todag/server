package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceMatchingAttemptCommandRepository {

    ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt);

    // 단건 조회 — 소프트 삭제된 기록은 제외, 로우에 쓰기 락
    // 커맨드 트랜잭션 안에서 대상 매칭 시도를 다시 읽기 위한 용도
    // "재시도 접수 여부 확인 → 아웃박스 적재"처럼 읽은 값을 근거로 쓰는 구간에서, 같은 매칭 시도에 대한 동시 요청을 직렬화하기 위해 사용
    Optional<ServiceMatchingAttempt> findByIdForUpdate(UUID matchingAttemptId);

    // 해당 서비스 희망 일정(servicePreferenceId)을 성사시킨 가장 최근 매칭 시도 1건
    Optional<ServiceMatchingAttempt> findLatestMatched(UUID servicePreferenceId);

    // 케어플랜에 아직 해소되지 않은 매칭 실패가 몇 건인지 — CarePlanCompleted 발행 조건 판단용
    // "미해소" = FAILED 이력이 있는데 그 희망 일정으로 생성된 일정 레코드가 아직 하나도 없는 경우
    long countUnresolvedFailed(UUID carePlanId);

    // 위 countUnresolvedFailed와 같은 대상을 실체로 조회 — 보정 스윕이 EXPIRED로 종결 처리할 대상
    List<ServiceMatchingAttempt> findUnresolvedFailed(UUID carePlanId);

    // 보정 스윕 대상 케어플랜 ID — 마지막 활동일(일정/매칭시도 date의 최댓값)이 lastActivityThreshold 이하이면서
    // 미해소 FAILED가 남아있는 케어플랜. 한 번에 가져올 상한을 limit으로 걸게 됨
    List<UUID> findSweepTargetCarePlanIds(LocalDate lastActivityThreshold, int limit);

    // 동일한 ProviderMatched를 이미 기록했는지 (중복 수신 방어용)
    // 같은 매칭 결과를 가리키는 값들의 조합을 대체 키로 사용
    boolean existsMatched(
            UUID servicePreferenceId,
            UUID serviceOfferingId,
            LocalDate date,
            Instant matchedAt
    );

    // 동일한 ProviderMatchFailed를 이미 기록했는지 (중복 수신 방어용)
    // 실패 페이로드에는 serviceOfferingId가 없으므로 failedAt을 대체 키에 포함
    boolean existsFailed(
            UUID servicePreferenceId,
            LocalDate date,
            Instant failedAt
    );
}
