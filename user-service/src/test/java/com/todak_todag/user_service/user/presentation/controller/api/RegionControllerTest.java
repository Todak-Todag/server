package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaRegionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegionControllerTest {

    private static final String URI = "/api/v1/regions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaRegionRepository jpaRegionRepository;

    @AfterEach
    void tearDown() {
        jpaRegionRepository.deleteAll();
    }

    @Nested
    @DisplayName("서비스 가능 지역 목록 조회")
    class FindAvailableRegions {

        @Test
        @DisplayName("활성화된 지역을 조회한다")
        void findAvailableRegions_success() throws Exception {
            // given
            UUID regionId = UUID.randomUUID();

            persist(
                    regionId,
                    "전라남도",
                    "고흥군",
                    "4677000000",
                    true
            );

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
        @DisplayName("비활성화된 지역은 조회하지 않는다")
        void findAvailableRegions_excludesInactiveRegions() throws Exception {
            // given
            persist(
                    UUID.randomUUID(),
                    "서울특별시",
                    "강남구",
                    "1168000000",
                    true
            );

            persist(
                    UUID.randomUUID(),
                    "부산광역시",
                    "해운대구",
                    "2635000000",
                    false
            );

            // when & then
            mockMvc.perform(get(URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].province")
                            .value("서울특별시"));
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 배열을 반환한다")
        void findAvailableRegions_empty() throws Exception {
            // when & then
            mockMvc.perform(get(URI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    private Region persist(
            UUID regionId,
            String province,
            String district,
            String regionCode,
            boolean active
    ) {
        Region region = createRegion(
                regionId,
                province,
                district,
                regionCode,
                active
        );

        return jpaRegionRepository.save(region);
    }

    private Region createRegion(
            UUID regionId,
            String province,
            String district,
            String regionCode,
            boolean active
    ) {
        try {
            Constructor<Region> constructor = Region.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Region region = constructor.newInstance();

            setField(region, "id", regionId);
            setField(region, "province", province);
            setField(region, "district", district);
            setField(region, "regionCode", regionCode);
            setField(region, "active", active);

            return region;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setField(Region region, String fieldName, Object value) {
        try {
            Field field = Region.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(region, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}