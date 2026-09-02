package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.presentation.request.CarePlanServiceSelectRequest;
import com.spring.careplanservice.careplan.presentation.response.CarePlanServiceSelectResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CarePlanServiceController {

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/care-plans/{carePlanId}/services")
    public ResponseEntity<ApiResponse<CarePlanServiceSelectResponse>> selectCarePlanService(
            @AuthenticationPrincipal UserContext user,
            @PathVariable UUID carePlanId,
            @Valid @RequestBody CarePlanServiceSelectRequest request
    ) {

    }
}
