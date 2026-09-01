package com.todak_todag.discharge_service.discharge.presentation.api_controller;

import com.todak_todag.discharge_service.discharge.application.command_service.DischargeCommandService;
import com.todak_todag.discharge_service.discharge.application.result.DischargeCreateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DischargeApiController.class)
class DischargeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DischargeCommandService dischargeCommandService;

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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_FORBIDDEN"));
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
}