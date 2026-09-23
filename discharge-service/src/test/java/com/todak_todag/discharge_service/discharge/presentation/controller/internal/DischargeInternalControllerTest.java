package com.todak_todag.discharge_service.discharge.presentation.controller.internal;

import com.todak_todag.discharge_service.discharge.application.result.DischargeInternalFindResult;
import com.todak_todag.discharge_service.discharge.application.service.query.DischargeQueryService;
import com.todak_todag.discharge_service.global.config.SecurityConfig;
import com.todak_todag.discharge_service.global.config.WebMvcConfig;
import com.todak_todag.discharge_service.global.security.InternalApiKeyInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DischargeInternalController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfig.class,
        InternalApiKeyInterceptor.class
})
class DischargeInternalControllerTest {

    private static final String INTERNAL_API_KEY =
            "test-internal-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DischargeQueryService dischargeQueryService;

    @Test
    void 올바른_내부_API_Key로_퇴원건_조회에_성공한다() throws Exception {
        UUID dischargeId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        LocalDate actualDate =
                LocalDate.of(2026, 9, 7);

        when(
                dischargeQueryService.findById(
                        eq(dischargeId)
                )
        )
                .thenReturn(
                        new DischargeInternalFindResult(
                                dischargeId,
                                patientId,
                                actualDate
                        )
                );

        mockMvc.perform(
                        get(
                                "/internal/v1/discharges/{dischargeId}",
                                dischargeId
                        )
                                .header(
                                        "X-Internal-Api-Key",
                                        INTERNAL_API_KEY
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success")
                        .value(true))
                .andExpect(jsonPath("$.code")
                        .value(200))
                .andExpect(jsonPath("$.data.dischargeId")
                        .value(dischargeId.toString()))
                .andExpect(jsonPath("$.data.patientId")
                        .value(patientId.toString()))
                .andExpect(jsonPath("$.data.actualDate")
                        .value(actualDate.toString()));
    }

    @Test
    void 내부_API_Key가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(
                        get(
                                "/internal/v1/discharges/{dischargeId}",
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_UNAUTHORIZED_INTERNAL_REQUEST"));
    }

    @Test
    void 잘못된_내부_API_Key이면_401을_반환한다() throws Exception {
        mockMvc.perform(
                        get(
                                "/internal/v1/discharges/{dischargeId}",
                                UUID.randomUUID()
                        )
                                .header(
                                        "X-Internal-Api-Key",
                                        "wrong-api-key"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_UNAUTHORIZED_INTERNAL_REQUEST"));
    }
}