package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.common.PageableFactory;
import com.todak_todag.schedule_service.global.response.ApiResponse;
import com.todak_todag.schedule_service.global.response.PageResponse;
import com.todak_todag.schedule_service.global.security.UserContext;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceMatchingAttemptFacade;
import com.todak_todag.schedule_service.schedule.application.query.MatchingAttemptSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptRetryResult;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.presentation.request.MatchingAttemptRetryRequest;
import com.todak_todag.schedule_service.schedule.presentation.response.MatchingAttemptRetryResponse;
import com.todak_todag.schedule_service.schedule.presentation.response.MatchingAttemptSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Controller는 헤더를 직접 읽지 않고 @AuthenticationPrincipal UserContext user로만 주입받음
@RestController
@RequestMapping("/api/v1/matching-attempts")
@RequiredArgsConstructor
@Validated
public class MatchingAttemptApiController implements MatchingAttemptApiSpec {

    private final ServiceMatchingAttemptFacade serviceMatchingAttemptFacade;

    // [외부 API] 매칭 실패 내역 조회 — 퇴원 예정자 전용
    @Override
    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PageResponse<MatchingAttemptSearchResponse>>> search(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) MatchingAttemptStatus status,
            @AuthenticationPrincipal UserContext user
    ) {
        // page/size/sort 보정은 PageableFactory에 위임 (size는 10/30/50만 허용, 그 외는 10)
        Pageable pageable = PageableFactory.of(page, size, sort);

        // status 미지정 시 기본값 FAILED 보정은 Query 팩토리가 담당
        MatchingAttemptSearchQuery searchQuery = MatchingAttemptSearchQuery.of(
                user.getUserId(),
                status,
                pageable
        );

        Page<MatchingAttemptSearchResult> result = serviceMatchingAttemptFacade.search(searchQuery);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.ok(
                                "매칭 실패 내역 조회 성공",
                                PageResponse.of(result, MatchingAttemptSearchResponse::from)
                        )
                );
    }

    // [외부 API] 재매칭 시도 — 퇴원 예정자 전용
    // 동기적으로는 아무것도 쓰지 않고 ProviderReMatched 이벤트만 발행하므로 202로 응답
    @Override
    @PostMapping("/{matchingAttemptId}/retry")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<MatchingAttemptRetryResponse>> retry(
            @PathVariable UUID matchingAttemptId,
            @Valid @RequestBody MatchingAttemptRetryRequest request,
            @AuthenticationPrincipal UserContext user
    ) {
        MatchingAttemptRetryResult result = serviceMatchingAttemptFacade.retry(
                request.toCommand(matchingAttemptId, user.getUserId())
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        ApiResponse.accepted(
                                "재매칭 시도 접수 성공",
                                MatchingAttemptRetryResponse.from(result)
                        )
                );
    }
}
