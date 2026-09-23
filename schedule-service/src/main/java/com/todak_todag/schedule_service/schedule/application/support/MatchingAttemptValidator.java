package com.todak_todag.schedule_service.schedule.application.support;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// 재매칭 시도 전용 검증 헬퍼
@Component
public class MatchingAttemptValidator {

    // 매칭에 실패한 건만 재시도할 수 있음
    // status 도메인은 MATCHED/FAILED 두 개뿐이므로 FAILED가 아니면 곧 "이미 매칭에 성공한 경우"
    public void validateRetryable(MatchingAttemptStatus status) {
        if (status != MatchingAttemptStatus.FAILED) {
            throw new BusinessException(ScheduleErrorCode.MATCHING_ATTEMPT_NOT_RETRYABLE);
        }
    }

    // 같은 실패 건으로 재시도가 이미 접수됐는지
    // 이 API는 동기적으로 아무것도 쓰지 않아 status로는 "재시도 중"을 표현할 수 없으므로,
    // 아웃박스에 같은 matchingAttemptId로 ProviderReMatched가 적재됐는지를 기준으로 판별
    public void validateNotAlreadyRequested(boolean alreadyRequested) {
        if (alreadyRequested) {
            throw new BusinessException(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED);
        }
    }

    // 재매칭 희망 날짜는 Care Plan의 start_date~finish_date 범위 내여야 함
    // CarePlanRange가 30일 고정 기간으로 역산한 값 사용
    public void validateRetryDate(LocalDate requestedDate, CarePlanPort.CarePlanRange carePlanRange) {
        if (!carePlanRange.covers(requestedDate)) {
            throw new BusinessException(ScheduleErrorCode.MATCHING_ATTEMPT_RETRY_EXCEEDS_CARE_PLAN_RANGE);
        }
    }
}
