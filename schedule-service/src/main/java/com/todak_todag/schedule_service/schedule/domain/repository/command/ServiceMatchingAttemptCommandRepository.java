package com.todak_todag.schedule_service.schedule.domain.repository.command;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ServiceMatchingAttemptCommandRepository {

    ServiceMatchingAttempt save(ServiceMatchingAttempt serviceMatchingAttempt);

    // 단건 조회 — 소프트 삭제된 기록은 제외
    // 커맨드 트랜잭션 안에서 대상 매칭 시도를 다시 읽기 위한 용도
    Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId);

    // 해당 서비스 희망 일정(servicePreferenceId)을 성사시킨 가장 최근 매칭 시도 1건
    Optional<ServiceMatchingAttempt> findLatestMatched(UUID servicePreferenceId);

    // 케어플랜에 아직 해소되지 않은 매칭 실패가 몇 건인지 — CarePlanCompleted 발행 조건 판단용
    // "미해소" = FAILED 이력이 있는데 그 희망 일정으로 생성된 일정 레코드가 아직 하나도 없는 경우
    long countUnresolvedFailed(UUID carePlanId);

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
