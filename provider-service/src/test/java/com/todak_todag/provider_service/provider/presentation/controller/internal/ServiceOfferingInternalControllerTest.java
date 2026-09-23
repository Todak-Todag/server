package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.global.security.InternalHeader;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingIdsResult;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.hasSize;

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

    @Nested
    @DisplayName("제공자별 제공 서비스 목록 조회")
    class FindIdsByProvider {

        private final UUID providerId = UUID.randomUUID();

        @Test
        @DisplayName("유효한 내부 키로 호출하면 200과 serviceOfferingId 목록을 반환한다")
        void findIdsByProvider_success() throws Exception {
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();

            given(serviceOfferingQueryService.findIdsByProvider(any()))
                    .willReturn(new ServiceOfferingIdsResult(List.of(first, second)));

            mockMvc.perform(get(BASE_URL)
                            .param("providerId", providerId.toString())
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("제공 서비스 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[0]").value(first.toString()))
                    .andExpect(jsonPath("$.data.content[1]").value(second.toString()));
        }

        @Test
        @DisplayName("보유한 제공 서비스가 없으면 200과 빈 배열을 반환한다")
        void findIdsByProvider_empty() throws Exception {
            given(serviceOfferingQueryService.findIdsByProvider(any()))
                    .willReturn(new ServiceOfferingIdsResult(List.of()));

            mockMvc.perform(get(BASE_URL)
                            .param("providerId", providerId.toString())
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("내부 키가 없으면 401")
        void findIdsByProvider_missingKey() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("providerId", providerId.toString()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
        }

        @Test
        @DisplayName("providerId가 없으면 400")
        void findIdsByProvider_missingParam() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("providerId가 UUID 형식이 아니면 400")
        void findIdsByProvider_invalidId() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("providerId", "not-a-uuid")
                            .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }
}