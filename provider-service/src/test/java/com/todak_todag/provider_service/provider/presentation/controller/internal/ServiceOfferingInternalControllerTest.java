package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.global.security.InternalHeader;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingProviderResult;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("제공 서비스 내부 API")
class ServiceOfferingInternalControllerTest {

    private static final String BASE_URL = "/internal/v1/service-offerings";
    private static final String VALID_KEY = "test-internal-api-key";

    private final UUID serviceOfferingId = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceOfferingQueryService serviceOfferingQueryService;

    @Nested
    @DisplayName("제공자 조회")
    class FindProvider {

        @Test
        @DisplayName("유효한 내부 키로 호출하면 200과 providerId를 반환한다")
        void findProvider_success() throws Exception {
            UUID providerId = UUID.randomUUID();

            given(serviceOfferingQueryService.findProvider(any()))
                    .willReturn(new ServiceOfferingProviderResult(providerId));

            mockMvc.perform(get(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("제공 서비스 제공자 조회 성공"))
                    .andExpect(jsonPath("$.data.providerId").value(providerId.toString()));
        }

        @Test
        @DisplayName("내부 키가 없으면 401")
        void findProvider_missingKey() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{serviceOfferingId}", serviceOfferingId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
        }

        @Test
        @DisplayName("내부 키가 일치하지 않으면 401")
        void findProvider_invalidKey() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header(InternalHeader.INTERNAL_KEY, "wrong-key"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
        }

        @Test
        @DisplayName("존재하지 않으면 404")
        void findProvider_notFound() throws Exception {
            given(serviceOfferingQueryService.findProvider(any()))
                    .willThrow(new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SERVICE_OFFERING_NOT_FOUND"));
        }

        @Test
        @DisplayName("serviceOfferingId가 UUID 형식이 아니면 400")
        void findProvider_invalidId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{serviceOfferingId}", "not-a-uuid")
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }
}