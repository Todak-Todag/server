package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.config.SecurityConfig;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingRegionSearchResult;
import com.todak_todag.provider_service.provider.application.service.query.ServiceOfferingQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceOfferingAdminApiController.class)
@DisplayName("지역별 제공 서비스 목록 조회 API")
class ServiceOfferingAdminApiControllerTest {

    private static final String BASE_URL = "/api/v1/admin/service-offerings/regions";

    private final UUID regionId = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceOfferingQueryService serviceOfferingQueryService;

    private Page<ServiceOfferingRegionSearchResult> page(UUID serviceOfferingId, UUID providerId) {
        return new PageImpl<>(
                List.of(new ServiceOfferingRegionSearchResult(
                        serviceOfferingId, providerId, UUID.randomUUID(), "방문간호")),
                PageRequest.of(0, 10),
                1
        );
    }

    @Test
    @DisplayName("ADMIN이 조회하면 200과 content, pageInfo를 반환한다")
    void searchByRegion_success() throws Exception {
        UUID serviceOfferingId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        given(serviceOfferingQueryService.searchByRegion(any()))
                .willReturn(page(serviceOfferingId, providerId));

        mockMvc.perform(get(BASE_URL + "/{regionId}", regionId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].serviceOfferingId")
                        .value(serviceOfferingId.toString()))
                .andExpect(jsonPath("$.data.content[0].providerId")
                        .value(providerId.toString()))
                .andExpect(jsonPath("$.data.content[0].provideServiceName").value("방문간호"))
                .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"))
                .andExpect(jsonPath("$.data.pageInfo.page").value(0))
                .andExpect(jsonPath("$.data.pageInfo.size").value(10))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
    }

    @Test
    @DisplayName("조회되는 제공 서비스가 없으면 빈 목록을 반환한다")
    void searchByRegion_empty() throws Exception {
        given(serviceOfferingQueryService.searchByRegion(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get(BASE_URL + "/{regionId}", regionId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("담당 지역이 아니면 403을 반환한다")
    void searchByRegion_otherRegion() throws Exception {
        doThrow(new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN))
                .when(serviceOfferingQueryService)
                .searchByRegion(any());

        mockMvc.perform(get(BASE_URL + "/{regionId}", regionId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SERVICE_PROVIDER가 요청하면 403을 반환한다")
    void searchByRegion_serviceProvider() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{regionId}", regionId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MASTER가 조회하면 200을 반환한다")
    void searchByRegion_master() throws Exception {
        given(serviceOfferingQueryService.searchByRegion(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get(BASE_URL + "/{regionId}", regionId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("인증 헤더가 없으면 403을 반환한다")
    void searchByRegion_noAuthHeader() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{regionId}", regionId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("regionId가 UUID 형식이 아니면 400을 반환한다")
    void searchByRegion_invalidRegionId() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{regionId}", "not-a-uuid")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
