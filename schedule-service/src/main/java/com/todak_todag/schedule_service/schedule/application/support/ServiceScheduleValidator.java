package com.todak_todag.schedule_service.schedule.application.support;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ServiceScheduleValidator {

    // 일정 시작 24시간 전까지만 변경 가능
    private static final long RESCHEDULE_DEADLINE_HOURS = 24;

    // 일정 시작 24시간 전까지만 취소 가능
    private static final long CANCEL_DEADLINE_HOURS = 24;

    // 일정 시작 24시간 전까지만 취소 가능
    public void validateCancelDeadline(LocalDateTime startedAt) {
        if (LocalDateTime.now().plusHours(CANCEL_DEADLINE_HOURS).isAfter(startedAt)) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED);
        }
    }

    // 본인에게 배정된 일정만 변경/취소 가능
    // Care Plan Internal API 응답의 patientId와 요청자(UserContext) userId를 비교하는 방식으로만 판별
    public void validateOwnership(UUID requesterId, UUID patientId) {
        if (!requesterId.equals(patientId)) {
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }
    }

    // 일정 시작 24시간 전까지만 변경 가능
    public void validateDeadline(LocalDateTime startedAt) {
        if (LocalDateTime.now().plusHours(RESCHEDULE_DEADLINE_HOURS).isAfter(startedAt)) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_DELAY_DEADLINE_EXCEEDED);
        }
    }

    // 기존 일정 날짜 D 기준 하루 앞당기기(D-1) 또는 하루 미루기(D+1)만 허용
    public void validateRescheduleDate(LocalDate currentDate, LocalDate requestedDate, LocalDate finishDate) {
        if (requestedDate.equals(currentDate.minusDays(1))) {
            // 하루 앞당기기: 변경일이 오늘(당일)인 경우 불가
            if (requestedDate.equals(LocalDate.now())) {
                throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_TO_TODAY_NOT_ALLOWED);
            }
            return;
        }

        if (requestedDate.equals(currentDate.plusDays(1))) {
            // 하루 미루기: Care Plan의 일정 범위(finishDate)를 초과할 수 없음
            if (requestedDate.isAfter(finishDate)) {
                throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_EXCEEDS_CARE_PLAN_RANGE);
            }
            return;
        }

        throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_RESCHEDULE_DATE);
    }

    // 본인이 배정된 서비스 제공자인지 검증
    public void validateAssignedProvider(UUID requesterId, UUID assignedProviderId) {
        if (!requesterId.equals(assignedProviderId)) {
            throw new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
        }
    }

    // 요청 시점이 해당 일정의 finishedAt 이후여야만 완료/부도 처리 가능
    public void validateCompletionDeadline(LocalDateTime finishedAt) {
        if (!LocalDateTime.now().isAfter(finishedAt)) {
            throw new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_STATUS_UPDATE_TOO_EARLY);
        }
    }
}
