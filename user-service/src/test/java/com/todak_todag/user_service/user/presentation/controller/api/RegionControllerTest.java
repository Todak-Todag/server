package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.application.result.RegionCreateResult;
import com.todak_todag.user_service.user.application.result.RegionFindAdminResult;
import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.application.result.RegionFindDetailResult;
import com.todak_todag.user_service.user.application.service.command.RegionCommandService;
import com.todak_todag.user_service.user.application.service.query.RegionQueryService;
import com.todak_todag.user_service.user.presentation.request.RegionCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegionControllerTest {

    private static final String URI = "/api/v1/regions";
    private static final String ADMIN_URI = "/api/v1/admin/regions";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegionQueryService regionQueryService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegionCommandService regionCommandService;

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

    @Nested
    @DisplayName("관리자 지역 목록 조회")
    @WithMockUser(roles = "MASTER")
    class FindAdminRegions {

        @Test
        @DisplayName("조회 조건에 맞는 지역 목록을 페이징하여 조회한다")
        void findAdminRegions_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();

            RegionFindAdminQuery query = new RegionFindAdminQuery(
                    0,
                    10,
                    "전라남도",
                    "고흥군",
                    "4677000000",
                    true
            );

            RegionFindAdminResult result = new RegionFindAdminResult(
                    regionId,
                    "전라남도",
                    "고흥군",
                    "4677000000",
                    true
            );

            Page<RegionFindAdminResult> page = new PageImpl<>(
                    List.of(result),
                    PageRequest.of(0, 10),
                    1
            );

            given(regionQueryService.findAdminRegions(query))
                    .willReturn(page);

            // when & then
            mockMvc.perform(
                            get(ADMIN_URI)
                                    .param("page", "0")
                                    .param("size", "10")
                                    .param("province", "전라남도")
                                    .param("district", "고흥군")
                                    .param("regionCode", "4677000000")
                                    .param("isActive", "true")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("지역 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].regionId")
                            .value(regionId.toString()))
                    .andExpect(jsonPath("$.data.content[0].province")
                            .value("전라남도"))
                    .andExpect(jsonPath("$.data.content[0].district")
                            .value("고흥군"))
                    .andExpect(jsonPath("$.data.content[0].regionCode")
                            .value("4677000000"))
                    .andExpect(jsonPath("$.data.content[0].isActive")
                            .value(true))
                    .andExpect(jsonPath("$.data.pageInfo.paginationType")
                            .value("OFFSET"))
                    .andExpect(jsonPath("$.data.pageInfo.page")
                            .value(0))
                    .andExpect(jsonPath("$.data.pageInfo.size")
                            .value(10))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements")
                            .value(1))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages")
                            .value(1));

            then(regionQueryService).should()
                    .findAdminRegions(query);
        }

        @Test
        @DisplayName("조회 조건 없이 지역 목록을 조회할 수 있다")
        void findAdminRegions_withoutConditions() throws Exception {
            // given
            RegionFindAdminQuery query = new RegionFindAdminQuery(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            given(regionQueryService.findAdminRegions(query))
                    .willReturn(Page.empty());

            // when & then
            mockMvc.perform(get(ADMIN_URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("지역 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));

            then(regionQueryService).should()
                    .findAdminRegions(query);
        }

        @Test
        @DisplayName("조회되는 지역이 없으면 빈 목록을 반환한다")
        void findAdminRegions_empty() throws Exception {
            // given
            RegionFindAdminQuery query = new RegionFindAdminQuery(
                    0,
                    10,
                    "제주특별자치도",
                    null,
                    null,
                    null
            );

            Page<RegionFindAdminResult> page = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10),
                    0
            );

            given(regionQueryService.findAdminRegions(query))
                    .willReturn(page);

            // when & then
            mockMvc.perform(
                            get(ADMIN_URI)
                                    .param("page", "0")
                                    .param("size", "10")
                                    .param("province", "제주특별자치도")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements")
                            .value(0))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages")
                            .value(0));
        }

        @Test
        @DisplayName("조회 조건의 최대 길이를 초과하면 400을 반환한다")
        void findAdminRegions_invalidCondition() throws Exception {
            // when & then
            mockMvc.perform(
                            get(ADMIN_URI)
                                    .param(
                                            "province",
                                            "123456789012345678901"
                                    )
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("지역 단건 조회")
    class FindRegion {

        @Test
        @DisplayName("지역 ID로 지역 상세 정보를 조회한다")
        void findRegion_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();

            RegionFindDetailResult result =
                    new RegionFindDetailResult(
                            regionId,
                            "전라남도",
                            "고흥군",
                            "4677000000",
                            true
                    );

            given(regionQueryService.findRegion(regionId))
                    .willReturn(result);

            // when & then
            mockMvc.perform(get(URI + "/{regionId}", regionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message")
                            .value("지역 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.regionId")
                            .value(regionId.toString()))
                    .andExpect(jsonPath("$.data.province")
                            .value("전라남도"))
                    .andExpect(jsonPath("$.data.district")
                            .value("고흥군"))
                    .andExpect(jsonPath("$.data.regionCode")
                            .value("4677000000"))
                    .andExpect(jsonPath("$.data.isActive")
                            .value(true));

            then(regionQueryService)
                    .should()
                    .findRegion(regionId);
        }

        @Test
        @DisplayName("존재하지 않는 지역이면 404를 반환한다")
        void findRegion_notFound() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionQueryService.findRegion(regionId))
                    .willThrow(
                            new BusinessException(
                                    RegionErrorCode.REGION_NOT_FOUND
                            )
                    );

            // when & then
            mockMvc.perform(get(URI + "/{regionId}", regionId))
                    .andExpect(status().isNotFound());

            then(regionQueryService)
                    .should()
                    .findRegion(regionId);
        }

        @Test
        @DisplayName("지역 ID가 UUID 형식이 아니면 400을 반환한다")
        void findRegion_invalidRegionId() throws Exception {
            // when & then
            mockMvc.perform(get(URI + "/{regionId}", "invalid-region-id"))
                    .andExpect(status().isBadRequest());

            then(regionQueryService)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("지역 등록")
    class CreateRegion {

        @Test
        @WithMockUser(roles = "MASTER")
        @DisplayName("관리자는 지역을 등록할 수 있다")
        void createRegion_success() throws Exception {
            UUID regionId = UUID.randomUUID();

            RegionCreateRequest request = new RegionCreateRequest(
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            RegionCreateResult result =
                    new RegionCreateResult(regionId);

            given(regionCommandService.createRegion(any()))
                    .willReturn(result);

            mockMvc.perform(
                            post("/api/v1/admin/regions")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.message")
                            .value("지역 등록 성공"))
                    .andExpect(jsonPath("$.data.regionId")
                            .value(regionId.toString()));

            then(regionCommandService)
                    .should()
                    .createRegion(any());
        }

        @Test
        @WithMockUser(roles = "MASTER")
        @DisplayName("필수 지역 정보가 누락되면 400을 반환한다")
        void createRegion_validationFail() throws Exception {
            RegionCreateRequest request = new RegionCreateRequest(
                    "",
                    "고흥군",
                    "4677000000"
            );

            mockMvc.perform(
                            post("/api/v1/admin/regions")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest());

            then(regionCommandService)
                    .shouldHaveNoInteractions();
        }
    }
}