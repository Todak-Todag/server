package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.config.SecurityConfig;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.facade.ServiceOfferingFacade;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceOfferingApiController.class)
@DisplayName("제공 서비스 API")
class ServiceOfferingApiControllerTest {

    private static final String BASE_URL = "/api/v1/service-offerings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceOfferingFacade serviceOfferingFacade;

    @MockitoBean
    private ServiceOfferingQueryService serviceOfferingQueryService;

    @Nested
    @DisplayName("등록")
    class Create {

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

            given(serviceOfferingFacade.create(any()))
                    .willReturn(new ServiceOfferingCreateResult(serviceOfferingId, providerId, Instant.now()));

            mockMvc.perform(post(BASE_URL)
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
            mockMvc.perform(post(BASE_URL)
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
            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        private final UUID serviceOfferingId = UUID.randomUUID();

        @Test
        @DisplayName("삭제에 성공하면 204를 반환한다")
        void delete_success() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("ADMIN도 삭제할 수 있다")
        void delete_admin() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("SERVICE_PROVIDER도 ADMIN도 아니면 403")
        void delete_forbidden() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("확정된 일정이 있으면 409")
        void delete_scheduleExists() throws Exception {
            doThrow(new BusinessException(ProviderErrorCode.SERVICE_OFFERING_SCHEDULE_EXISTS))
                    .when(serviceOfferingFacade).delete(Mockito.any());

            mockMvc.perform(delete(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("SERVICE_OFFERING_SCHEDULE_EXISTS"));
        }

        @Test
        @DisplayName("존재하지 않으면 404")
        void delete_notFound() throws Exception {
            doThrow(new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND))
                    .when(serviceOfferingFacade).delete(Mockito.any());

            mockMvc.perform(delete(BASE_URL + "/{serviceOfferingId}", serviceOfferingId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SERVICE_OFFERING_NOT_FOUND"));
        }

        @Test
        @DisplayName("serviceOfferingId가 UUID 형식이 아니면 400")
        void delete_invalidId() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{serviceOfferingId}", "not-a-uuid")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    @Nested
    @DisplayName("목록 조회")
    class Search {

        private Page<ServiceOfferingSearchResult> page(UUID serviceOfferingId, UUID provideServiceId) {
            return new PageImpl<>(
                    List.of(new ServiceOfferingSearchResult(
                            serviceOfferingId, provideServiceId, "방문간호", Instant.now())),
                    PageRequest.of(0, 10),
                    1
            );
        }

        @Test
        @DisplayName("조회에 성공하면 200과 content, pageInfo를 반환한다")
        void search_success() throws Exception {
            UUID serviceOfferingId = UUID.randomUUID();
            UUID provideServiceId = UUID.randomUUID();

            given(serviceOfferingQueryService.search(any()))
                    .willReturn(page(serviceOfferingId, provideServiceId));

            mockMvc.perform(get(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].serviceOfferingId")
                            .value(serviceOfferingId.toString()))
                    .andExpect(jsonPath("$.data.content[0].provideServiceName").value("방문간호"))
                    .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"))
                    .andExpect(jsonPath("$.data.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.size").value(10))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
        }

        @Test
        @DisplayName("ADMIN도 조회할 수 있다")
        void search_admin() throws Exception {
            given(serviceOfferingQueryService.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get(BASE_URL)
                            .param("providerId", UUID.randomUUID().toString())
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("SERVICE_PROVIDER도 ADMIN도 아니면 403")
        void search_forbidden() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("providerId가 UUID 형식이 아니면 400")
        void search_invalidProviderId() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("providerId", "not-a-uuid")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
