package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.event.CarePlanConfirmedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderRematchedEvent;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPort;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.port.ScheduleSlot;
import com.todak_todag.provider_service.provider.application.support.MatchingService;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.amqp.AmqpException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("매칭 처리")
class MatchingFacadeTest {

    private static final LocalDate THURSDAY = LocalDate.of(2026, 9, 10);

    @Mock
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @Mock
    private ProvideWorkQueryRepository provideWorkQueryRepository;

    @Mock
    private SchedulePort schedulePort;

    @Mock
    private MatchingEventPort matchingEventPort;

    private MatchingFacade matchingFacade;

    private final UUID carePlanId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID offeringIdA = UUID.randomUUID();
    private final UUID offeringIdB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 판정 로직은 MatchingServiceTest가 검증하므로 실제 구현을 그대로 쓴다
        matchingFacade = new MatchingFacade(
                serviceOfferingQueryRepository,
                provideWorkQueryRepository,
                new MatchingService(),
                schedulePort,
                matchingEventPort
        );
    }

    private ServiceOffering offering(UUID id) {
        ServiceOffering offering = ServiceOffering.of(UUID.randomUUID(), provideServiceId, regionId);
        ReflectionTestUtils.setField(offering, "id", id);

        return offering;
    }

    private ProvideWork work(UUID serviceOfferingId, String startedAt, String finishedAt) {
        return ProvideWork.of(serviceOfferingId, 4, LocalTime.parse(startedAt), LocalTime.parse(finishedAt));
    }

    private CarePlanConfirmedEvent event(CarePlanConfirmedEvent.Preference... preferences) {
        return new CarePlanConfirmedEvent(
                carePlanId,
                regionId,
                List.of(new CarePlanConfirmedEvent.Service(
                        UUID.randomUUID(), provideServiceId, List.of(preferences)))
        );
    }

    private CarePlanConfirmedEvent.Preference preference(UUID id, LocalDate date, TimeSlot timeSlot) {
        return new CarePlanConfirmedEvent.Preference(id, date, timeSlot);
    }

    @Test
    @DisplayName("매칭에 성공하면 ProviderMatched를 발행한다")
    void match_publishesMatched() {
        UUID preferenceId = UUID.randomUUID();

        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.match(event(preference(preferenceId, THURSDAY, TimeSlot.MORNING)));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort).publishMatched(captor.capture());
        verify(matchingEventPort, never()).publishMatchFailed(any());

        ProviderMatchedEvent published = captor.getValue();
        assertThat(published.carePlanId()).isEqualTo(carePlanId);
        assertThat(published.regionId()).isEqualTo(regionId);
        assertThat(published.servicePreferenceId()).isEqualTo(preferenceId);
        assertThat(published.provideServiceId()).isEqualTo(provideServiceId);
        assertThat(published.serviceOfferingId()).isEqualTo(offeringIdA);
        assertThat(published.date()).isEqualTo(THURSDAY);
        assertThat(published.startedAt()).isEqualTo(THURSDAY.atTime(9, 0));
    }

    @Test
    @DisplayName("후보가 없으면 ProviderMatchFailed를 발행한다")
    void match_publishesMatchFailed() {
        UUID preferenceId = UUID.randomUUID();

        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of());

        matchingFacade.match(event(preference(preferenceId, THURSDAY, TimeSlot.MORNING)));

        ArgumentCaptor<ProviderMatchFailedEvent> captor = ArgumentCaptor.forClass(ProviderMatchFailedEvent.class);
        verify(matchingEventPort).publishMatchFailed(captor.capture());
        verify(matchingEventPort, never()).publishMatched(any());

        ProviderMatchFailedEvent published = captor.getValue();
        assertThat(published.servicePreferenceId()).isEqualTo(preferenceId);
        assertThat(published.preferredTimeSlot()).isEqualTo(TimeSlot.MORNING);
        assertThat(published.failureReason()).isEqualTo(ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER);
    }

    @Test
    @DisplayName("후보가 없으면 Schedule-Service를 호출하지 않는다")
    void match_skipsScheduleCallWhenNoCandidate() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of());

        matchingFacade.match(event(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)));

        verify(schedulePort, never()).findSchedules(anyList(), any());
    }

    @Test
    @DisplayName("희망 일정이 여러 건이어도 Schedule-Service는 한 번만 호출한다")
    void match_callsScheduleOnce() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.match(event(
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING),
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)
        ));

        verify(schedulePort, times(1)).findSchedules(anyList(), any());
    }

    @Test
    @DisplayName("같은 이벤트 안에서 방금 배정한 시간에 다시 배정하지 않는다")
    void match_accumulatesInMemory() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA), offering(offeringIdB)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(
                        work(offeringIdA, "09:00", "13:00"),
                        work(offeringIdB, "10:00", "18:00")
                ));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.match(event(
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING),
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)
        ));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort, times(2)).publishMatched(captor.capture());

        List<ProviderMatchedEvent> published = captor.getAllValues();

        // 첫 건이 A를 09:00에 잡았으므로 두 번째는 부하가 적은 B로 간다
        assertThat(published.get(0).serviceOfferingId()).isEqualTo(offeringIdA);
        assertThat(published.get(0).startedAt()).isEqualTo(THURSDAY.atTime(9, 0));
        assertThat(published.get(1).serviceOfferingId()).isEqualTo(offeringIdB);
        assertThat(published.get(1).startedAt()).isEqualTo(THURSDAY.atTime(10, 0));
    }

    @Test
    @DisplayName("Schedule-Service 호출이 실패해도 예외를 밖으로 던지지 않는다")
    void match_isolatesException() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(new BusinessException(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));

        // 예외가 밖으로 나가면 RabbitMQ 재시도로 이미 발행한 결과가 중복 발행된다
        matchingFacade.match(event(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)));

        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("재매칭도 같은 판정으로 ProviderMatched를 발행한다")
    void rematch_publishesMatched() {
        UUID preferenceId = UUID.randomUUID();

        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.rematch(new ProviderRematchedEvent(
                carePlanId, regionId, provideServiceId, preferenceId, THURSDAY, TimeSlot.MORNING
        ));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort).publishMatched(captor.capture());

        assertThat(captor.getValue().servicePreferenceId()).isEqualTo(preferenceId);
        assertThat(captor.getValue().startedAt()).isEqualTo(THURSDAY.atTime(9, 0));
    }

    @Test
    @DisplayName("재매칭 시간대가 null이면 하루 전체를 대상으로 매칭한다")
    void rematch_nullTimeSlot() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "14:00", "18:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.rematch(new ProviderRematchedEvent(
                carePlanId, regionId, provideServiceId, UUID.randomUUID(), THURSDAY, null
        ));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort).publishMatched(captor.capture());

        assertThat(captor.getValue().startedAt()).isEqualTo(THURSDAY.atTime(14, 0));
    }

    @Test
    @DisplayName("재매칭 중 외부 서비스 장애는 리스너 재시도를 받도록 예외를 그대로 던진다")
    void rematch_externalFailure_rethrows() {
        // 잠시 뒤 성공할 수 있는 실패라 리스너 재시도에 맡긴다
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(new BusinessException(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> matchingFacade.rematch(new ProviderRematchedEvent(
                carePlanId, regionId, provideServiceId, UUID.randomUUID(), THURSDAY, TimeSlot.MORNING
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);

        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("재매칭 중 발행이 실패해도 예외를 밖으로 던지지 않는다")
    void rematch_publishFailure_doesNotThrow() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());
        willThrow(new AmqpException("broker unavailable"))
                .given(matchingEventPort).publishMatched(any());

        assertThatCode(() -> matchingFacade.rematch(new ProviderRematchedEvent(
                carePlanId, regionId, provideServiceId, UUID.randomUUID(), THURSDAY, TimeSlot.MORNING
        ))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("발행이 실패한 배정은 점유로 잡지 않아 다음 희망 일정이 같은 시각에 배정된다")
    void match_publishFailure_doesNotOccupySlot() {
        // 발행 실패분을 미리 점유로 잡으면 뒤따르는 희망 일정이 존재하지 않는 일정을 피해 배정된다
        UUID firstPreferenceId = UUID.randomUUID();
        UUID secondPreferenceId = UUID.randomUUID();

        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        // 첫 번째 발행만 실패시킨다
        willThrow(new AmqpException("broker unavailable"))
                .willDoNothing()
                .given(matchingEventPort).publishMatched(any());

        matchingFacade.match(event(
                preference(firstPreferenceId, THURSDAY, TimeSlot.MORNING),
                preference(secondPreferenceId, THURSDAY, TimeSlot.MORNING)
        ));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort, times(2)).publishMatched(captor.capture());

        // 첫 건이 유실됐으므로 09:00은 비어 있고, 두 번째 희망 일정이 그 자리를 받는다
        ProviderMatchedEvent second = captor.getAllValues().get(1);
        assertThat(second.servicePreferenceId()).isEqualTo(secondPreferenceId);
        assertThat(second.startedAt()).isEqualTo(THURSDAY.atTime(9, 0));
    }

    @Test
    @DisplayName("매칭 실패 이벤트 발행이 실패해도 다음 희망 일정 처리는 계속된다")
    void match_publishFailedEventFailure_continues() {
        UUID firstPreferenceId = UUID.randomUUID();
        UUID secondPreferenceId = UUID.randomUUID();

        // 후보가 없어 두 건 모두 매칭 실패한다
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of());

        willThrow(new AmqpException("broker unavailable"))
                .willDoNothing()
                .given(matchingEventPort).publishMatchFailed(any());

        matchingFacade.match(event(
                preference(firstPreferenceId, THURSDAY, TimeSlot.MORNING),
                preference(secondPreferenceId, THURSDAY, TimeSlot.MORNING)
        ));

        verify(matchingEventPort, times(2)).publishMatchFailed(any());
    }
}