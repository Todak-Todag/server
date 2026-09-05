package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideWorkCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("제공 가능 일정 등록")
class ProvideWorkCommandServiceTest {

    private static final int MONDAY = 1;
    private static final int TUESDAY = 2;

    private final UUID serviceOfferingId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();
    private final UUID provideWorkId = UUID.randomUUID();

    @Mock
    private ProvideWorkCommandRepository provideWorkCommandRepository;

    @Mock
    private ProvideWorkQueryRepository provideWorkQueryRepository;

    @Mock
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @InjectMocks
    private ProvideWorkCommandService provideWorkCommandService;

    private ProvideWorkCreateCommand command(int day, LocalTime startedAt, LocalTime finishedAt) {
        return new ProvideWorkCreateCommand(serviceOfferingId, providerId, day, startedAt, finishedAt);
    }

    private ServiceOffering ownedOffering() {
        ServiceOffering offering = Mockito.mock(ServiceOffering.class);
        given(offering.isOwnedBy(providerId)).willReturn(true);
        return offering;
    }

    private ProvideWork savedProvideWork() {
        ProvideWork provideWork = Mockito.mock(ProvideWork.class);
        given(provideWork.getId()).willReturn(provideWorkId);
        return provideWork;
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("겹치는 일정이 없으면 등록된다")
        void create_success() {
            ServiceOffering offering = ownedOffering();
            ProvideWork saved = savedProvideWork();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId)).willReturn(List.of());
            given(provideWorkCommandRepository.save(any(ProvideWork.class))).willReturn(saved);

            ProvideWorkCreateResult result = provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));

            assertThat(result.provideWorkId()).isEqualTo(provideWorkId);
            verify(provideWorkCommandRepository).save(any(ProvideWork.class));
        }

        @Test
        @DisplayName("요일이 다르면 시간이 같아도 등록된다")
        void create_differentDay() {
            ServiceOffering offering = ownedOffering();
            ProvideWork saved = savedProvideWork();
            ProvideWork existing = ProvideWork.of(serviceOfferingId, MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId))
                    .willReturn(List.of(existing));
            given(provideWorkCommandRepository.save(any(ProvideWork.class))).willReturn(saved);

            ProvideWorkCreateResult result = provideWorkCommandService.create(
                    command(TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));

            assertThat(result.provideWorkId()).isEqualTo(provideWorkId);
        }

        @Test
        @DisplayName("앞 일정의 종료 시각과 시작 시각이 같으면 겹치지 않는다")
        void create_adjacentTime() {
            ServiceOffering offering = ownedOffering();
            ProvideWork saved = savedProvideWork();
            ProvideWork morning = ProvideWork.of(serviceOfferingId, MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId))
                    .willReturn(List.of(morning));
            given(provideWorkCommandRepository.save(any(ProvideWork.class))).willReturn(saved);

            ProvideWorkCreateResult result = provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0)));

            assertThat(result.provideWorkId()).isEqualTo(provideWorkId);
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("존재하지 않는 제공 서비스면 SERVICE_OFFERING_NOT_FOUND")
        void notFound() {
            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(provideWorkQueryRepository, never()).findAllByServiceOfferingId(any());
            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 AUTH_FORBIDDEN")
        void notOwner() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(provideWorkQueryRepository, never()).findAllByServiceOfferingId(any());
            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 요일에 시간이 겹치면 PROVIDE_WORK_TIME_OVERLAP")
        void timeOverlap() {
            ServiceOffering offering = ownedOffering();
            ProvideWork existing = ProvideWork.of(serviceOfferingId, MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId))
                    .willReturn(List.of(existing));

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);

            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("기존 일정을 완전히 포함해도 PROVIDE_WORK_TIME_OVERLAP")
        void timeOverlap_contains() {
            ServiceOffering offering = ownedOffering();
            ProvideWork existing = ProvideWork.of(serviceOfferingId, MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId))
                    .willReturn(List.of(existing));

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);

            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 이르면 PROVIDE_WORK_INVALID_TIME_RANGE")
        void invalidTimeRange() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId)).willReturn(List.of());

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(13, 0), LocalTime.of(9, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_TIME_RANGE);

            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("시작 시각과 종료 시각이 같으면 PROVIDE_WORK_INVALID_TIME_RANGE")
        void invalidTimeRange_same() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingQueryRepository.findById(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findAllByServiceOfferingId(serviceOfferingId)).willReturn(List.of());

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_TIME_RANGE);

            verify(provideWorkCommandRepository, never()).save(any());
        }
    }
}