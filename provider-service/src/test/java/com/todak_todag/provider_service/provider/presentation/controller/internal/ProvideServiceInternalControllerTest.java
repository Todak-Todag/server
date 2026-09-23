package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.security.InternalHeader;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceInfoResult;
import com.todak_todag.provider_service.provider.application.service.query.ProvideServiceQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("서비스 종류 내부 API")
class ProvideServiceInternalControllerTest {

    private static final String BASE_URL = "/internal/v1/provide-services";
    private static final String VALID_KEY = "test-internal-api-key";

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProvideServiceQueryService provideServiceQueryService;

    @Test
    @DisplayName("유효한 내부 키로 호출하면 200과 서비스 종류 목록을 반환한다")
    void findAllByIds_success() throws Exception {
        given(provideServiceQueryService.findAllByIds(any()))
                .willReturn(List.of(
                        new ProvideServiceInfoResult(first, "방문간호", "간호사가 가정을 방문해 간호 서비스를 제공합니다."),
                        new ProvideServiceInfoResult(second, "가사지원", "청소, 세탁 등 일상 가사 활동을 지원합니다.")
                ));

        mockMvc.perform(get(BASE_URL)
                        .param("provideServiceIds", first + "," + second)
                        .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("서비스 종류 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].provideServiceId").value(first.toString()))
                .andExpect(jsonPath("$.data.content[0].name").value("방문간호"))
                .andExpect(jsonPath("$.data.content[0].content").value("간호사가 가정을 방문해 간호 서비스를 제공합니다."))
                .andExpect(jsonPath("$.data.content[1].name").value("가사지원"));
    }

    @Test
    @DisplayName("존재하지 않는 ID만 전달하면 200과 빈 배열을 반환한다")
    void findAllByIds_empty() throws Exception {
        given(provideServiceQueryService.findAllByIds(any()))
                .willReturn(List.of());

        mockMvc.perform(get(BASE_URL)
                        .param("provideServiceIds", first.toString())
                        .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("내부 키가 없으면 401")
    void findAllByIds_missingKey() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("provideServiceIds", first.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
    }

    @Test
    @DisplayName("내부 키가 일치하지 않으면 401")
    void findAllByIds_invalidKey() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("provideServiceIds", first.toString())
                        .header(InternalHeader.INTERNAL_KEY, "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_INTERNAL_REQUEST"));
    }

    @Test
    @DisplayName("provideServiceIds가 없으면 400")
    void findAllByIds_missingParam() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("provideServiceIds가 비어 있으면 400")
    void findAllByIds_emptyParam() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("provideServiceIds", "")
                        .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("provideServiceIds가 UUID 형식이 아니면 400")
    void findAllByIds_invalidId() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("provideServiceIds", "not-a-uuid")
                        .header(InternalHeader.INTERNAL_KEY, VALID_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}