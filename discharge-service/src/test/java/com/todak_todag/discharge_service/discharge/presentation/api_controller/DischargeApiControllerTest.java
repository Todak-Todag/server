package com.todak_todag.discharge_service.discharge.presentation.api_controller;

import com.todak_todag.discharge_service.discharge.application.service.command.DischargeCommandService;
import com.todak_todag.discharge_service.discharge.application.service.query.DischargeQueryService;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import com.todak_todag.discharge_service.discharge.application.result.DischargeFindResult;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.global.common.UserRole;
import com.todak_todag.discharge_service.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DischargeApiController.class)
@Import(SecurityConfig.class)
class DischargeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DischargeCommandService dischargeCommandService;

    @MockitoBean
    private DischargeQueryService dischargeQueryService;

    @Test
    void 퇴원건_생성에_성공한다() throws Exception {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(dischargeCommandService.createDischarge(any()))
                .thenReturn(new DischargeCreateResult(dischargeId));

        String requestBody = """
                {
                  "patientId": "%s",
                  "hospitalName": "Test Hospital",
                  "scheduledDate": "%s"
                }
                """.formatted(
                patientId,
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(
                        post("/api/v1/discharges")
                                .header(
                                        "X-User-Id",
                                        UUID.randomUUID().toString()
                                )
                                .header(
                                        "X-User-Role",
                                        "HOSPITAL_STAFF"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.dischargeId")
                        .value(dischargeId.toString()));
    }

    @Test
    void 병원_담당자가_아니면_퇴원건을_생성할_수_없다() throws Exception {
        String requestBody = """
                {
                  "patientId": "%s",
                  "hospitalName": "Test Hospital",
                  "scheduledDate": "%s"
                }
                """.formatted(
                UUID.randomUUID(),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(
                        post("/api/v1/discharges")
                                .header(
                                        "X-User-Id",
                                        UUID.randomUUID().toString()
                                )
                                .header(
                                        "X-User-Role",
                                        "PATIENT"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void 퇴원_예정일은_미래_날짜여야_한다() throws Exception {
        String requestBody = """
                {
                  "patientId": "%s",
                  "hospitalName": "Test Hospital",
                  "scheduledDate": "%s"
                }
                """.formatted(
                UUID.randomUUID(),
                LocalDate.now()
        );

        mockMvc.perform(
                        post("/api/v1/discharges")
                                .header(
                                        "X-User-Id",
                                        UUID.randomUUID().toString()
                                )
                                .header(
                                        "X-User-Role",
                                        "HOSPITAL_STAFF"
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("COMMON_INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details.scheduledDate")
                        .exists());
    }

    @Test
    void 환자가_퇴원건_단건_조회에_성공한다() throws Exception {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        LocalDate scheduledDate =
                LocalDate.of(2026, 9, 1);

        Instant createdAt =
                Instant.parse("2026-08-20T01:00:00Z");

        when(
                dischargeQueryService.findDischarge(
                        eq(dischargeId),
                        eq(patientId),
                        eq(UserRole.PATIENT)
                )
        )
                .thenReturn(
                        new DischargeFindResult(
                                dischargeId,
                                patientId,
                                hospitalStaffId,
                                "Test Hospital",
                                DischargeStatus.SCHEDULED,
                                scheduledDate,
                                null,
                                createdAt
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/discharges/{dischargeId}",
                                dischargeId
                        )
                                .header(
                                        "X-User-Id",
                                        patientId.toString()
                                )
                                .header(
                                        "X-User-Role",
                                        "PATIENT"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dischargeId")
                        .value(dischargeId.toString()))
                .andExpect(jsonPath("$.data.patientId")
                        .value(patientId.toString()))
                .andExpect(jsonPath("$.data.hospitalStaffId")
                        .value(hospitalStaffId.toString()))
                .andExpect(jsonPath("$.data.hospitalName")
                        .value("Test Hospital"))
                .andExpect(jsonPath("$.data.status")
                        .value("SCHEDULED"))
                .andExpect(jsonPath("$.data.scheduledDate")
                        .value(scheduledDate.toString()))
                .andExpect(jsonPath("$.data.actualDate")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.createdAt")
                        .value(createdAt.toString()));
    }

    @Test
    void 병원_담당자가_퇴원건_단건_조회에_성공한다() throws Exception {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID hospitalStaffId = UUID.randomUUID();

        LocalDate scheduledDate =
                LocalDate.of(2026, 9, 1);

        Instant createdAt =
                Instant.parse("2026-08-20T01:00:00Z");

        when(
                dischargeQueryService.findDischarge(
                        eq(dischargeId),
                        eq(hospitalStaffId),
                        eq(UserRole.HOSPITAL_STAFF)
                )
        )
                .thenReturn(
                        new DischargeFindResult(
                                dischargeId,
                                patientId,
                                hospitalStaffId,
                                "Test Hospital",
                                DischargeStatus.SCHEDULED,
                                scheduledDate,
                                null,
                                createdAt
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/v1/discharges/{dischargeId}",
                                dischargeId
                        )
                                .header(
                                        "X-User-Id",
                                        hospitalStaffId.toString()
                                )
                                .header(
                                        "X-User-Role",
                                        "HOSPITAL_STAFF"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dischargeId")
                        .value(dischargeId.toString()))
                .andExpect(jsonPath("$.data.patientId")
                        .value(patientId.toString()))
                .andExpect(jsonPath("$.data.hospitalStaffId")
                        .value(hospitalStaffId.toString()))
                .andExpect(jsonPath("$.data.hospitalName")
                        .value("Test Hospital"))
                .andExpect(jsonPath("$.data.status")
                        .value("SCHEDULED"))
                .andExpect(jsonPath("$.data.scheduledDate")
                        .value(scheduledDate.toString()))
                .andExpect(jsonPath("$.data.actualDate")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.createdAt")
                        .value(createdAt.toString()));
    }

    @Test
    void 허용되지_않은_역할은_퇴원건을_조회할_수_없다() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/discharges/{dischargeId}",
                                UUID.randomUUID()
                        )
                                .header(
                                        "X-User-Id",
                                        UUID.randomUUID().toString()
                                )
                                .header(
                                        "X-User-Role",
                                        "SOCIAL_WORKER"
                                )
                )
                .andExpect(status().isForbidden());
    }
}