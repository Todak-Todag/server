package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.application.service.query.RegionQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegionControllerTest {

    private static final String URI = "/api/v1/regions";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionQueryService regionQueryService;

    @Nested
    @DisplayName("서비스 가능 지역 목록 조회")
    class FindAvailableRegions {

        @Test
        @DisplayName("서비스 가능한 지역 목록을 조회한다")
        void findAvailableRegions_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();

            RegionFindAvailableResult result =
                    new RegionFindAvailableResult(
                            regionId,
                            "전라남도",
                            "고흥군",
                            "4677000000"
                    );

            given(regionQueryService.findAvailableRegions())
                    .willReturn(List.of(result));

            // when & then
            mockMvc.perform(get(URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("서비스 가능 지역 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].regionId")
                            .value(regionId.toString()))
                    .andExpect(jsonPath("$.data.content[0].province")
                            .value("전라남도"))
                    .andExpect(jsonPath("$.data.content[0].district")
                            .value("고흥군"))
                    .andExpect(jsonPath("$.data.content[0].regionCode")
                            .value("4677000000"));
        }

        @Test
        @DisplayName("서비스 가능한 지역이 없으면 빈 목록을 반환한다")
        void findAvailableRegions_empty() throws Exception {
            // given
            given(regionQueryService.findAvailableRegions())
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get(URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("서비스 가능 지역 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }
}