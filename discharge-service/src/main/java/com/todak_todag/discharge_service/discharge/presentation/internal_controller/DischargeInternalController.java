package com.todak_todag.discharge_service.discharge.presentation.internal_controller;

import com.todak_todag.discharge_service.discharge.application.service.query.DischargeQueryService;
import com.todak_todag.discharge_service.discharge.application.result.DischargeInternalFindResult;
import com.todak_todag.discharge_service.discharge.presentation.response.DischargeInternalFindResponse;
import com.todak_todag.discharge_service.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/discharges")
public class DischargeInternalController {

    private final DischargeQueryService dischargeQueryService;

    @GetMapping("/{dischargeId}")
    public ApiResponse<DischargeInternalFindResponse> findById(
            @PathVariable UUID dischargeId
    ) {
        DischargeInternalFindResult result =
                dischargeQueryService.findById(dischargeId);

        return ApiResponse.success(
                HttpStatus.OK.value(),
                "퇴원 건 조회 성공",
                DischargeInternalFindResponse.from(result)
        );
    }
}