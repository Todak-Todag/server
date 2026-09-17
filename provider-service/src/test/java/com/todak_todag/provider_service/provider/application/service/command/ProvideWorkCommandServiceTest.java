package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkDeleteCommand;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkUpdateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideWorkCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 겹침 판정은 DB 쿼리(existsOverlapped)로 옮겼으므로, 이 테스트는 "판정 결과에 따라 서비스가 어떻게 동작하는가"만 검증한다
// 요일·시각 경계 규칙 자체(요일이 다르면 안 겹침, 종료-시작이 맞닿으면 안 겹침 등)는 리포지토리 테스트에서 검증한다
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
    private ServiceOfferingCommandRepository serviceOfferingCommandRepository;

    @InjectMocks
    private ProvideWorkCommandService provideWorkCommandService;

    private ProvideWorkCreateCommand command(int day, LocalTime startedAt, LocalTime finishedAt) {
        return new ProvideWorkCreateCommand(serviceOfferingId, providerId, day, startedAt, finishedAt);
    }

    private ProvideWorkUpdateCommand updateCommand(int day, LocalTime startedAt, LocalTime finishedAt) {
        return new ProvideWorkUpdateCommand(
                serviceOfferingId, provideWorkId, providerId, day, startedAt, finishedAt);
    }

    private ProvideWorkDeleteCommand deleteCommand() {
        return new ProvideWorkDeleteCommand(serviceOfferingId, provideWorkId, providerId);
    }

    private ProvideWork existingProvideWork(int day, LocalTime startedAt, LocalTime finishedAt) {
        ProvideWork provideWork = ProvideWork.of(serviceOfferingId, day, startedAt, finishedAt);
        ReflectionTestUtils.setField(provideWork, "id", provideWorkId);
        return provideWork;
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

    // 겹침 조회 결과를 고정한다 (실제 판정은 리포지토리 쿼리가 담당)
    private void givenOverlapped(boolean overlapped) {
        given(provideWorkQueryRepository.existsOverlapped(any(), any(), any(), any(), any()))
                .willReturn(overlapped);
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("겹치는 일정이 없으면 등록된다")
        void create_success() {
            ServiceOffering offering = ownedOffering();
            ProvideWork saved = savedProvideWork();

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            givenOverlapped(false);
            given(provideWorkCommandRepository.save(any(ProvideWork.class))).willReturn(saved);

            ProvideWorkCreateResult result = provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));

            assertThat(result.provideWorkId()).isEqualTo(provideWorkId);
            verify(provideWorkCommandRepository).save(any(ProvideWork.class));
        }

        @Test
        @DisplayName("겹침 조회에 제공 서비스·요일·시간이 그대로 전달된다")
        void create_passesQueryArguments() {
            ServiceOffering offering = ownedOffering();
            ProvideWork saved = savedProvideWork();

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            givenOverlapped(false);
            given(provideWorkCommandRepository.save(any(ProvideWork.class))).willReturn(saved);

            provideWorkCommandService.create(command(TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));

            // 등록이므로 제외 대상(excludedProvideWorkId)은 null이어야 한다
            verify(provideWorkQueryRepository).existsOverlapped(
                    serviceOfferingId, null, TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("존재하지 않는 제공 서비스면 SERVICE_OFFERING_NOT_FOUND")
        void notFound() {
            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(provideWorkQueryRepository, never()).existsOverlapped(any(), any(), any(), any(), any());
            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 AUTH_FORBIDDEN")
        void notOwner() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(provideWorkQueryRepository, never()).existsOverlapped(any(), any(), any(), any(), any());
            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("겹치는 일정이 있으면 PROVIDE_WORK_TIME_OVERLAP")
        void timeOverlap() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            givenOverlapped(true);

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);

            verify(provideWorkCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 이르면 PROVIDE_WORK_INVALID_TIME_RANGE")
        void invalidTimeRange() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            givenOverlapped(false);

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

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            givenOverlapped(false);

            assertThatThrownBy(() -> provideWorkCommandService.create(
                    command(MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_TIME_RANGE);

            verify(provideWorkCommandRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("요일과 시간을 바꾸면 반영된다")
        void update_success() {
            ServiceOffering offering = ownedOffering();
            ProvideWork provideWork = existingProvideWork(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(provideWork));
            givenOverlapped(false);

            ProvideWorkUpdateResult result = provideWorkCommandService.update(
                    updateCommand(TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

            assertThat(result.provideWorkId()).isEqualTo(provideWorkId);
            assertThat(provideWork.getDay()).isEqualTo(TUESDAY);
            assertThat(provideWork.getStartedAt()).isEqualTo(LocalTime.of(14, 0));
            assertThat(provideWork.getFinishedAt()).isEqualTo(LocalTime.of(18, 0));
        }

        @Test
        @DisplayName("겹침 조회에 자기 자신이 제외 대상으로 전달된다")
        void update_excludesItself() {
            ServiceOffering offering = ownedOffering();
            ProvideWork provideWork = existingProvideWork(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(provideWork));
            givenOverlapped(false);

            ProvideWorkUpdateResult result = provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));

            assertThat(result.provideWorkId()).isEqualTo(provideWorkId);
            assertThat(provideWork.getDay()).isEqualTo(MONDAY);

            // 수정 대상 자신은 겹침 대상에서 빠져야 하므로 provideWorkId가 제외 인자로 넘어간다
            verify(provideWorkQueryRepository).existsOverlapped(
                    serviceOfferingId, provideWorkId, MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        }

        @Test
        @DisplayName("다른 일정과 시간이 겹치면 PROVIDE_WORK_TIME_OVERLAP")
        void update_timeOverlap() {
            ServiceOffering offering = ownedOffering();
            ProvideWork target = existingProvideWork(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(target));
            givenOverlapped(true);

            assertThatThrownBy(() -> provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);

            assertThat(target.getStartedAt()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("존재하지 않는 제공 서비스면 SERVICE_OFFERING_NOT_FOUND")
        void update_offeringNotFound() {
            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(provideWorkQueryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 AUTH_FORBIDDEN")
        void update_notOwner() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(provideWorkQueryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("존재하지 않는 제공 가능 일정이면 PROVIDE_WORK_NOT_FOUND")
        void update_provideWorkNotFound() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 제공 서비스에 속한 일정이면 PROVIDE_WORK_NOT_FOUND")
        void update_belongsToOtherOffering() {
            ServiceOffering offering = ownedOffering();
            ProvideWork otherOfferingWork =
                    ProvideWork.of(UUID.randomUUID(), MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(otherOfferingWork));

            assertThatThrownBy(() -> provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND);

            verify(provideWorkQueryRepository, never()).existsOverlapped(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 이르면 PROVIDE_WORK_INVALID_TIME_RANGE")
        void update_invalidTimeRange() {
            ServiceOffering offering = ownedOffering();
            ProvideWork provideWork = existingProvideWork(MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(provideWork));
            givenOverlapped(false);

            assertThatThrownBy(() -> provideWorkCommandService.update(
                    updateCommand(MONDAY, LocalTime.of(18, 0), LocalTime.of(14, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_INVALID_TIME_RANGE);

            assertThat(provideWork.getStartedAt()).isEqualTo(LocalTime.of(9, 0));
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("논리 삭제된다")
        void delete_success() {
            ServiceOffering offering = ownedOffering();
            ProvideWork provideWork = Mockito.mock(ProvideWork.class);

            given(provideWork.getId()).willReturn(provideWorkId);
            given(provideWork.getServiceOfferingId()).willReturn(serviceOfferingId);

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(provideWork));

            provideWorkCommandService.delete(deleteCommand());

            verify(provideWork).markDeleted(providerId);
        }

        @Test
        @DisplayName("존재하지 않는 제공 서비스면 SERVICE_OFFERING_NOT_FOUND")
        void delete_offeringNotFound() {
            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> provideWorkCommandService.delete(deleteCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);

            verify(provideWorkQueryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("본인 소유가 아니면 AUTH_FORBIDDEN")
        void delete_notOwner() {
            ServiceOffering offering = Mockito.mock(ServiceOffering.class);
            given(offering.isOwnedBy(providerId)).willReturn(false);

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));

            assertThatThrownBy(() -> provideWorkCommandService.delete(deleteCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.AUTH_FORBIDDEN);

            verify(provideWorkQueryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("존재하지 않는 제공 가능 일정이면 PROVIDE_WORK_NOT_FOUND")
        void delete_provideWorkNotFound() {
            ServiceOffering offering = ownedOffering();

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> provideWorkCommandService.delete(deleteCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 제공 서비스에 속한 일정이면 PROVIDE_WORK_NOT_FOUND")
        void delete_belongsToOtherOffering() {
            ServiceOffering offering = ownedOffering();
            ProvideWork provideWork = Mockito.mock(ProvideWork.class);

            given(provideWork.getServiceOfferingId()).willReturn(UUID.randomUUID());

            given(serviceOfferingCommandRepository.findByIdForUpdate(serviceOfferingId)).willReturn(Optional.of(offering));
            given(provideWorkQueryRepository.findById(provideWorkId)).willReturn(Optional.of(provideWork));

            assertThatThrownBy(() -> provideWorkCommandService.delete(deleteCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND);

            verify(provideWork, never()).markDeleted(any());
        }
    }
}
