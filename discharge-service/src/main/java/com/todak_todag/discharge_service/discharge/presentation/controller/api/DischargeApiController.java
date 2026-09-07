package com.todak_todag.discharge_service.discharge.presentation.controller.api;

import com.todak_todag.discharge_service.discharge.application.command.DischargeCreateCommand;
import com.todak_todag.discharge_service.discharge.application.command.DischargeUpdateCommand;
import com.todak_todag.discharge_service.discharge.application.query.DischargeSearchQuery;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeFindResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeSearchResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeUpdateResult;
import com.todak_todag.discharge_service.discharge.application.service.command.DischargeCommandService;
import com.todak_todag.discharge_service.discharge.application.service.query.DischargeQueryService;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeCreateRequest;
import com.todak_todag.discharge_service.discharge.presentation.request.DischargeUpdateRequest;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeCreateResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeFindResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeSearchResponse;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeUpdateResponse;
import com.todak_todag.discharge_service.global.common.PageableFactory;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import com.todak_todag.discharge_service.global.response.PageResponse;
import com.todak_todag.discharge_service.global.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discharges")
public class DischargeApiController {

    private final DischargeCommandService dischargeCommandService;
    private final DischargeQueryService dischargeQueryService;

    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<DischargeCreateResponse>> createDischarge(
            @AuthenticationPrincipal UserContext user,
            @Valid @RequestBody DischargeCreateRequest request
    ) {

        DischargeCreateCommand command =
                new DischargeCreateCommand(
                        user.getUserId(),
                        request.patientId(),
                        request.hospitalName(),
                        request.scheduledDate()
                );

        DischargeCreateResult result =
                dischargeCommandService.createDischarge(command);

        DischargeCreateResponse response =
                DischargeCreateResponse.from(result);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "퇴원건이 생성되었습니다.",
                                response
                        )
                );
    }

    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DischargeSearchResponse>>> searchDischarges(
            @AuthenticationPrincipal UserContext user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scheduledDate
    ) {

        Pageable pageable =
                PageableFactory.of(
                        page,
                        size,
                        sort
                );

        DischargeStatus statusFilter =
                DischargeStatus.fromFilter(status);

        DischargeSearchQuery query =
                new DischargeSearchQuery(
                        user.getUserId(),
                        statusFilter,
                        scheduledDate,
                        pageable
                );

        Page<DischargeSearchResult> result =
                dischargeQueryService.searchDischarges(query);

        List<DischargeSearchResponse> content =
                result.getContent()
                        .stream()
                        .map(DischargeSearchResponse::from)
                        .toList();

        PageResponse<DischargeSearchResponse> response =
                PageResponse.of(
                        result,
                        content
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "퇴원건 목록 조회 성공",
                        response
                )
        );
    }

    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PatchMapping("/{dischargeId}")
    public ResponseEntity<ApiResponse<DischargeUpdateResponse>> updateDischarge(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID dischargeId,
            @Valid @RequestBody DischargeUpdateRequest request
    ) {

        DischargeUpdateCommand command =
                new DischargeUpdateCommand(
                        dischargeId,
                        user.getUserId(),
                        request.status(),
                        request.scheduledDate()
                );

        DischargeUpdateResult result =
                dischargeCommandService.updateDischarge(command);

        DischargeUpdateResponse response =
                DischargeUpdateResponse.from(result);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "퇴원건이 수정되었습니다.",
                        response
                )
        );
    }

    @PreAuthorize("hasAnyRole('HOSPITAL_STAFF', 'PATIENT')")
    @GetMapping("/{dischargeId}")
    public ResponseEntity<ApiResponse<DischargeFindResponse>> findDischarge(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID dischargeId
    ) {

        DischargeFindResult result =
                dischargeQueryService.findDischarge(
                        dischargeId,
                        user.getUserId(),
                        user.getRole()
                );

        DischargeFindResponse response =
                DischargeFindResponse.from(result);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "퇴원건 조회에 성공했습니다.",
                        response
                )
        );
    }
}