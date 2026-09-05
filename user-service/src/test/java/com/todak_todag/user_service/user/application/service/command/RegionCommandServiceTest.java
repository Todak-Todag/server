package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.user.application.command.RegionCreateCommand;
import com.todak_todag.user_service.user.application.result.RegionCreateResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.command.RegionCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RegionCommandServiceTest {

    @Mock
    private RegionCommandRepository regionCommandRepository;

    @Mock
    private RegionQueryRepository regionQueryRepository;

    @InjectMocks
    private RegionCommandService regionCommandService;

    @Nested
    @DisplayName("지역 등록")
    class CreateRegion {

        @Test
        @DisplayName("지역을 비활성 상태로 등록한다")
        void createRegion_success() {
            UUID regionId = UUID.randomUUID();

            RegionCreateCommand command = new RegionCreateCommand(
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            Region savedRegion = Mockito.mock(Region.class);

            given(regionQueryRepository.existsByRegionCode(command.regionCode()))
                    .willReturn(false);

            given(savedRegion.getId())
                    .willReturn(regionId);

            given(regionCommandRepository.save(any(Region.class)))
                    .willReturn(savedRegion);

            RegionCreateResult result =
                    regionCommandService.createRegion(command);

            assertThat(result.regionId()).isEqualTo(regionId);

            then(regionQueryRepository)
                    .should()
                    .existsByRegionCode(command.regionCode());

            then(regionCommandRepository)
                    .should()
                    .save(any(Region.class));
        }

        @Test
        @DisplayName("동일한 행정구역 코드가 존재하면 등록할 수 없다")
        void createRegion_duplicateRegionCode() {
            RegionCreateCommand command = new RegionCreateCommand(
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            given(regionQueryRepository.existsByRegionCode(command.regionCode()))
                    .willReturn(true);

            assertThatThrownBy(() ->
                    regionCommandService.createRegion(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(RegionErrorCode.REGION_DUPLICATE);
                    });

            then(regionQueryRepository)
                    .should()
                    .existsByRegionCode(command.regionCode());

            then(regionCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }
}