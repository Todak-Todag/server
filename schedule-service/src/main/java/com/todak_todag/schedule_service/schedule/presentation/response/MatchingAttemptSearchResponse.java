package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// 15번 문서 Response 표와 필드/순서를 일치시킴
//
// servicePreferenceId/provideServiceId를 포함하는 이유:
//   한 요청자가 여러 servicePreferenceId를 가질 수 있어 한 응답에 여러 희망 일정의 시도 내역이 섞여 나온다.
//   클라이언트가 어느 희망 일정/제공 서비스의 실패인지 구분하려면 이 두 ID가 필요하다.
//   01번은 "내부 필터링용 ID는 노출하지 않는다"로 제거했으나, 15번 문서는 표에 명시적으로 포함하고 있다.
//
// failedAt/matchedAt은 Entity의 Instant를 그대로 노출한다 (감사 필드와 동일한 취급).
public record MatchingAttemptSearchResponse(
        UUID matchingAttemptId,
        UUID servicePreferenceId,
        UUID provideServiceId,
        LocalDate date,
        String preferredTimeSlot,
        String status,
        String failureReason,
        Instant failedAt,
        Instant matchedAt
) {

    public static MatchingAttemptSearchResponse from(MatchingAttemptSearchResult result) {
        return new MatchingAttemptSearchResponse(
                result.matchingAttemptId(),
                result.servicePreferenceId(),
                result.provideServiceId(),
                result.date(),
                result.preferredTimeSlot() == null ? null : result.preferredTimeSlot().name(),
                result.status().name(),
                result.failureReason(),
                result.failedAt(),
                result.matchedAt()
        );
    }
}
