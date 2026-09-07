package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.MatchingAttemptRetryCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.query.MatchingAttemptSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceMatchingAttemptResult;
import com.todak_todag.schedule_service.schedule.application.service.command.ServiceMatchingAttemptCommandService;
import com.todak_todag.schedule_service.schedule.application.service.query.ServiceMatchingAttemptQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceMatchingAttemptFacade {

    private final CarePlanPort carePlanPort;
    private final ServiceMatchingAttemptQueryService serviceMatchingAttemptQueryService;
    private final ServiceMatchingAttemptCommandService serviceMatchingAttemptCommandService;

    // 재매칭 시도 유스케이스 조합
    // 기능 범위: 검증 + ProviderReMatched 이벤트 발행
    public MatchingAttemptRetryResult retry(MatchingAttemptRetryCommand retryCommand) {

        // 존재 확인 겸 servicePreferenceId 확보 — QueryService는 조회만, 존재 여부 판단은 Facade 책임
        ServiceMatchingAttemptResult attempt = serviceMatchingAttemptQueryService.findById(retryCommand.matchingAttemptId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

        // 소유자(patientId)와 일정 범위(finishDate) 조회
        CarePlanPort.CarePlanRange carePlanRange = carePlanPort.findCarePlanRange(attempt.servicePreferenceId());

        return serviceMatchingAttemptCommandService.retry(retryCommand, carePlanRange);
    }

    // 매칭 시도 내역 목록 조회 유스케이스 조합
    public Page<MatchingAttemptSearchResult> search(MatchingAttemptSearchQuery searchQuery) {

        // 재매칭은 확정된 Care Plan에서만 의미가 있으므로 CONFIRMED가 아니면 조회 대상 없음
        // 상대 API는 UNDER_REVIEW/미존재일 때 예외를 던지는데, 이 예외는 감추지 않고 그대로 전파
        CarePlanPort.CarePlanSummary carePlan = carePlanPort.findCarePlanByPatient(searchQuery.userId());

        if (!carePlan.isConfirmed()) {
            log.info(
                    "[Schedule] Care Plan이 확정 상태가 아니어서 매칭 시도 내역을 조회하지 않습니다 carePlanId={} status={}",
                    carePlan.carePlanId(), carePlan.status()
            );
            return Page.empty(searchQuery.pageable());
        }

        List<UUID> servicePreferenceIds = carePlanPort.findServicePreferenceIds(searchQuery.userId());

        // 담당하는 servicePreferenceId가 하나도 없으면 DB 조회 없이 바로 빈 페이지를 반환
        if (servicePreferenceIds.isEmpty()) {
            return Page.empty(searchQuery.pageable());
        }

        return serviceMatchingAttemptQueryService.search(
                servicePreferenceIds,
                searchQuery.status(),
                searchQuery.excludeAlreadyScheduled(),
                searchQuery.pageable()
        );
    }
}
