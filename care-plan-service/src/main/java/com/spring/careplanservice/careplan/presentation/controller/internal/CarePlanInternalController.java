package com.spring.careplanservice.careplan.presentation.controller.internal;


import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanFindByPreferenceQuery;
import com.spring.careplanservice.careplan.application.query.ServicePreferenceIdsQuery;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceIdsResult;
import com.spring.careplanservice.careplan.application.service.query.CarePlanQueryService;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPreferenceResult;
import com.spring.careplanservice.careplan.presentation.response.CarePlanFindByPatientResponse;
import com.spring.careplanservice.careplan.presentation.response.CarePlanFindByPreferenceResponse;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceIdsResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/service-preferences/{servicePreferenceId}/care-plan")
    public ApiResponse<CarePlanFindByPreferenceResponse> findByServicePreference(
            @PathVariable("servicePreferenceId") UUID servicePreferenceId
    ) {
        CarePlanFindByPreferenceResult carePlanFindByPreferenceResult = carePlanQueryService.findByServicePreference(
                new CarePlanFindByPreferenceQuery(servicePreferenceId)
        );

        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Care Plan 조회 성공",
                CarePlanFindByPreferenceResponse.from(carePlanFindByPreferenceResult)
        );
    }

    @GetMapping("/service-preferences")
    public ApiResponse<ServicePreferenceIdsResponse> findServicePreferenceIds(
            @RequestParam("patientId") UUID patientId
    ) {
        ServicePreferenceIdsResult servicePreferenceIdsResult = carePlanQueryService.findServicePreferenceIds(
                new ServicePreferenceIdsQuery(patientId)
        );

        return ApiResponse.success(
                HttpStatus.OK.value(),
                "서비스 희망 일정 ID 목록 조회 성공",
                ServicePreferenceIdsResponse.from(
                        servicePreferenceIdsResult
                )
        );
    }
}
