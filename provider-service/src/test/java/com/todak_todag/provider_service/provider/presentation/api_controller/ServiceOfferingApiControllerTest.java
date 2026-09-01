package com.todak_todag.provider_service.provider.presentation.api_controller;

import com.todak_todag.provider_service.provider.application.command_service.ServiceOfferingCommandService;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceOfferingApiController.class)
@DisplayName("제공 서비스 등록 API")
class ServiceOfferingApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceOfferingCommandService serviceOfferingCommandService;

    private String body(String provideServiceId) {
        return """
                { "provideServiceId": "%s" }
                """.formatted(provideServiceId);
    }

    @Test
    @DisplayName("등록에 성공하면 201과 serviceOfferingId를 반환한다")
    void create_success() throws Exception {
        UUID serviceOfferingId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        given(serviceOfferingCommandService.create(any()))
                .willReturn(new ServiceOfferingResult.Create(serviceOfferingId, providerId, Instant.now()));

        mockMvc.perform(post("/api/v1/service-offerings")
                        .header("X-User-Id", providerId.toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.serviceOfferingId").value(serviceOfferingId.toString()));
    }

    @Test
    @DisplayName("SERVICE_PROVIDER가 아니면 403")
    void create_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/service-offerings")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID().toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("provideServiceId가 없으면 400")
    void create_invalidParameter() throws Exception {
        mockMvc.perform(post("/api/v1/service-offerings")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}