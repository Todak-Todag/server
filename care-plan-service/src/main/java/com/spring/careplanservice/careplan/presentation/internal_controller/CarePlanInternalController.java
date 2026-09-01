package com.spring.careplanservice.careplan.presentation.internal_controller;


import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.query_service.CarePlanQueryService;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.presentation.response.CarePlanFindByPatientResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1")
public class CarePlanInternalController {
    private final CarePlanQueryService carePlanQueryService;

    @GetMapping("/care-plans/{patientId}")
    public ApiResponse<CarePlanFindByPatientResponse> findByPatient(
            @PathVariable("patientId") UUID patientId
    ) {
        CarePlanFindByPatientResult carePlanFindByPatientResult = carePlanQueryService.findByPatient(
                new CarePlanFindByPatientQuery(patientId)
        );

        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Care Plan 조회 성공",
                CarePlanFindByPatientResponse.from(carePlanFindByPatientResult)
        );
    }
}
