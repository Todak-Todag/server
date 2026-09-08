package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.support.PostgresTestSupport;
import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaRegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegionQueryRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    private RegionQueryRepositoryImpl regionQueryRepository;

    @Autowired
    private JpaRegionRepository jpaRegionRepository;

    private Region goheung;
    private Region uiseong;
    private Region yeongwol;
    private Region gangnam;

    @BeforeEach
    void setUp() {
        jpaRegionRepository.deleteAll();

        goheung = Region.create(
                "전라남도",
                "고흥군",
                "4677000000"
        );

        uiseong = Region.create(
                "경상북도",
                "의성군",
                "4773000000"
        );

        yeongwol = Region.create(
                "강원특별자치도",
                "영월군",
                "5175000000"
        );

        gangnam = Region.create(
                "서울특별시",
                "강남구",
                "1168000000"
        );

        // MVP 서비스 대상 지역 3곳만 활성화
        goheung.updateActive(true);
        uiseong.updateActive(true);
        yeongwol.updateActive(true);

        jpaRegionRepository.saveAll(
                List.of(
                        goheung,
                        uiseong,
                        yeongwol,
                        gangnam
                )
        );
    }

    @Nested
    @DisplayName("공용 지역 목록 조회")
    class FindAvailableRegions {

        @Test
        @DisplayName("서비스 활성화된 지역만 조회한다")
        void findAllAvailableRegions() {

            // when
            List<Region> result =
                    regionQueryRepository.findAllAvailableRegions();

            // then
            assertThat(result)
                    .hasSize(3);

            assertThat(result)
                    .extracting(Region::getDistrict)
                    .containsExactlyInAnyOrder(
                            "고흥군",
                            "의성군",
                            "영월군"
                    );

            assertThat(result)
                    .extracting(Region::getDistrict)
                    .doesNotContain("강남구");
        }
    }

    @Nested
    @DisplayName("관리자 지역 목록 조회")
    class FindAdminRegions {

        @Test
        @DisplayName("시도 조건으로 지역을 조회한다")
        void findByProvince() {

            // given
            RegionFindAdminQuery query =
                    new RegionFindAdminQuery(
                            0,
                            20,
                            "전라남도",
                            null,
                            null,
                            null
                    );

            // when
            Page<Region> result =
                    regionQueryRepository.findAllByAdminConditions(
                            query,
                            PageRequest.of(0, 20)
                    );

            // then
            assertThat(result.getContent())
                    .hasSize(1);

            assertThat(result.getContent().getFirst().getDistrict())
                    .isEqualTo("고흥군");
        }

        @Test
        @DisplayName("시군구 조건으로 지역을 조회한다")
        void findByDistrict() {

            // given
            RegionFindAdminQuery query =
                    new RegionFindAdminQuery(
                            0,
                            20,
                            null,
                            "의성군",
                            null,
                            null
                    );

            // when
            Page<Region> result =
                    regionQueryRepository.findAllByAdminConditions(
                            query,
                            PageRequest.of(0, 20)
                    );

            // then
            assertThat(result.getContent())
                    .hasSize(1);

            assertThat(result.getContent().getFirst().getProvince())
                    .isEqualTo("경상북도");
        }

        @Test
        @DisplayName("활성 여부 조건으로 지역을 조회한다")
        void findByActive() {

            // given
            RegionFindAdminQuery query =
                    new RegionFindAdminQuery(
                            0,
                            20,
                            null,
                            null,
                            null,
                            false
                    );

            // when
            Page<Region> result =
                    regionQueryRepository.findAllByAdminConditions(
                            query,
                            PageRequest.of(0, 20)
                    );

            // then
            assertThat(result.getContent())
                    .hasSize(1);

            assertThat(result.getContent().getFirst().getDistrict())
                    .isEqualTo("강남구");
        }

        @Test
        @DisplayName("여러 조건을 조합하여 지역을 조회한다")
        void findByMultipleConditions() {

            // given
            RegionFindAdminQuery query =
                    new RegionFindAdminQuery(
                            0,
                            20,
                            "강원특별자치도",
                            "영월군",
                            "5175000000",
                            true
                    );

            // when
            Page<Region> result =
                    regionQueryRepository.findAllByAdminConditions(
                            query,
                            PageRequest.of(0, 20)
                    );

            // then
            assertThat(result.getContent())
                    .hasSize(1);

            Region region = result.getContent().getFirst();

            assertThat(region.getProvince())
                    .isEqualTo("강원특별자치도");

            assertThat(region.getDistrict())
                    .isEqualTo("영월군");

            assertThat(region.getRegionCode())
                    .isEqualTo("5175000000");

            assertThat(region.isActive())
                    .isTrue();
        }
    }
}