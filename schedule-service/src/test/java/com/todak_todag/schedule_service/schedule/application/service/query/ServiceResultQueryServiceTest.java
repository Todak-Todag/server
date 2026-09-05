package com.todak_todag.schedule_service.schedule.application.service.query;

import com.todak_todag.schedule_service.schedule.application.result.ServiceResultDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("서비스 수행 결과 조회 - QueryService")
class ServiceResultQueryServiceTest {

    @Mock
    private CarePlanServiceResultQueryRepository carePlanServiceResultQueryRepository;

    @InjectMocks
    private ServiceResultQueryService serviceResultQueryService;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("Repository 조회 결과를 Response 필드(serviceResultId/startedAt/finishedAt)로 변환한다")
    void search_mapsEntityToResult() {
        // given
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime finishedAt = LocalDateTime.of(2026, 9, 1, 10, 0);
        List<UUID> servicePreferenceIds = List.of(UUID.randomUUID());

        CarePlanServiceResult entity = CarePlanServiceResult.record(
                UUID.randomUUID(), startedAt, finishedAt, "정상 수행"
        );

        when(carePlanServiceResultQueryRepository.search(servicePreferenceIds, null, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        // when
        Page<ServiceResultSearchResult> result = serviceResultQueryService.search(servicePreferenceIds, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().startedAt()).isEqualTo(startedAt);
        assertThat(result.getContent().getFirst().finishedAt()).isEqualTo(finishedAt);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("전달받은 소유 ID 목록과 pageable을 그대로 Repository에 위임한다")
    void search_delegatesArgumentsToRepository() {
        // given
        List<UUID> serviceOfferingIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(carePlanServiceResultQueryRepository.search(null, serviceOfferingIds, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        serviceResultQueryService.search(null, serviceOfferingIds, pageable);

        // then
        verify(carePlanServiceResultQueryRepository).search(null, serviceOfferingIds, pageable);
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 페이지를 그대로 반환한다")
    void search_noResult_returnsEmptyPage() {
        // given
        List<UUID> servicePreferenceIds = List.of(UUID.randomUUID());

        when(carePlanServiceResultQueryRepository.search(servicePreferenceIds, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<ServiceResultSearchResult> result = serviceResultQueryService.search(servicePreferenceIds, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Nested
    @DisplayName("서비스 수행 결과 상세 조회")
    class findDetailByIdTest {

        @Test
        @DisplayName("Repository 조회 결과를 Response 필드 5개(serviceResultId/serviceScheduleId/startedAt/finishedAt/note)로 변환한다")
        void findDetailById_mapsEntityToResult() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            UUID serviceScheduleId = UUID.randomUUID();
            LocalDateTime startedAt = LocalDateTime.of(2026, 9, 1, 9, 0);
            LocalDateTime finishedAt = LocalDateTime.of(2026, 9, 1, 10, 0);

            CarePlanServiceResult entity = CarePlanServiceResult.record(
                    serviceScheduleId, startedAt, finishedAt, "정상 수행"
            );

            when(carePlanServiceResultQueryRepository.findById(serviceResultId))
                    .thenReturn(Optional.of(entity));

            // when
            Optional<ServiceResultDetailResult> result = serviceResultQueryService.findDetailById(serviceResultId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().serviceScheduleId()).isEqualTo(serviceScheduleId);
            assertThat(result.get().startedAt()).isEqualTo(startedAt);
            assertThat(result.get().finishedAt()).isEqualTo(finishedAt);
            assertThat(result.get().note()).isEqualTo("정상 수행");
        }

        @Test
        @DisplayName("note가 null인 결과도 그대로 변환한다")
        void findDetailById_nullNote_mappedAsNull() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            CarePlanServiceResult entity = CarePlanServiceResult.record(
                    UUID.randomUUID(),
                    LocalDateTime.of(2026, 9, 1, 9, 0),
                    LocalDateTime.of(2026, 9, 1, 10, 0),
                    null
            );

            when(carePlanServiceResultQueryRepository.findById(serviceResultId))
                    .thenReturn(Optional.of(entity));

            // when
            Optional<ServiceResultDetailResult> result = serviceResultQueryService.findDetailById(serviceResultId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().note()).isNull();
        }

        @Test
        @DisplayName("존재하지 않으면 빈 Optional을 그대로 반환한다")
        void findDetailById_notFound_returnsEmptyOptional() {
            // given
            UUID serviceResultId = UUID.randomUUID();
            when(carePlanServiceResultQueryRepository.findById(serviceResultId))
                    .thenReturn(Optional.empty());

            // when
            Optional<ServiceResultDetailResult> result = serviceResultQueryService.findDetailById(serviceResultId);

            // then
            assertThat(result).isEmpty();
            verify(carePlanServiceResultQueryRepository).findById(serviceResultId);
        }
    }
}
