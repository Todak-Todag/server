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
import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.repository.query.OutboxEventQueryRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
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
import static org.mockito.BDDMockito.willDoNothing;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

    @Mock
    private OutboxEventQueryRepository outboxEventQueryRepository;

    // 적재 결과 payload를 실제로 직렬화·역직렬화해야 해서 실제 매퍼를 쓴다
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private MatchingFacade matchingFacade;

    private final UUID carePlanId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID providerIdA = UUID.randomUUID();
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
                matchingEventPort,
                outboxEventQueryRepository,
                objectMapper
        );
    }

    // 후보마다 제공자가 달라, 제공자 기준 판정으로 바뀌어도 기존 테스트의 결과는 같다
    private ServiceOffering offering(UUID id) {
        return offering(id, UUID.randomUUID(), provideServiceId);
    }

    private ServiceOffering offering(UUID id, UUID providerId, UUID provideServiceId) {
        ServiceOffering offering = ServiceOffering.of(providerId, provideServiceId, regionId);
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

    // 이전 수신에서 이미 적재된 매칭 결과
    private ProviderOutboxEvent matchedResult(UUID preferenceId, UUID offeringId, LocalDate date, String startedAt) {
        ProviderMatchedEvent matched = new ProviderMatchedEvent(
                carePlanId, regionId, preferenceId, provideServiceId,
                offeringId, date, date.atTime(LocalTime.parse(startedAt)), Instant.now()
        );

        return ProviderOutboxEvent.of(OutboxEventType.PROVIDER_MATCHED, preferenceId, objectMapper.writeValueAsString(matched));
    }

    private ProviderOutboxEvent failedResult(UUID preferenceId, LocalDate date) {
        ProviderMatchFailedEvent failed = new ProviderMatchFailedEvent(
                carePlanId, regionId, preferenceId, provideServiceId,
                date, TimeSlot.MORNING, ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER, Instant.now()
        );

        return ProviderOutboxEvent.of(OutboxEventType.PROVIDER_MATCH_FAILED, preferenceId, objectMapper.writeValueAsString(failed));
    }

    private ProviderRematchedEvent rematchEvent(UUID preferenceId, LocalDate date) {
        return new ProviderRematchedEvent(carePlanId, regionId, provideServiceId, preferenceId, date, TimeSlot.MORNING);
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
    @DisplayName("Schedule-Service 호출이 실패하면 아무것도 발행하지 않고 예외를 그대로 던진다")
    void match_externalFailure_rethrows() {
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(new BusinessException(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));

        // 조회 단계에서 실패하므로 아직 적재된 것이 없다
        // 적재가 0건이어야 재전송되어도 중복 발행이 생기지 않는다
        assertThatThrownBy(() -> matchingFacade.match(
                event(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING))
        )).isInstanceOf(BusinessException.class);

        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("매칭 중 외부 서비스 연결이 끊기면 리스너 재시도를 받도록 예외를 그대로 던진다")
    void match_connectionFailure_rethrows() {
        // 상대 서비스가 죽으면 Feign이 status -1로 예외를 던진다
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(new RetryableException(
                        -1, "Connection refused", Request.HttpMethod.GET, (Long) null, feignRequest()
                ));

        assertThatThrownBy(() -> matchingFacade.match(
                event(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING))
        )).isInstanceOf(RetryableException.class);

        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("매칭 중 4xx 응답은 다시 보내도 같은 결과라 재시도하지 않는다")
    void match_clientError_doesNotThrow() {
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(FeignException.errorStatus("ScheduleClient#findSchedules", Response.builder()
                        .status(400)
                        .reason("Bad Request")
                        .request(feignRequest())
                        .headers(Map.of())
                        .build()));

        assertThatCode(() -> matchingFacade.match(
                event(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING))
        )).doesNotThrowAnyException();

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
        ), false);

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
        ), false);

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
        ), false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);

        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("재매칭 중 외부 서비스 연결이 끊기면 리스너 재시도를 받도록 예외를 그대로 던진다")
    void rematch_connectionFailure_rethrows() {
        // 상대 서비스가 죽으면 Feign이 status -1로 예외를 던진다
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(new RetryableException(
                        -1, "Connection refused", Request.HttpMethod.GET, (Long) null, feignRequest()
                ));

        assertThatThrownBy(() -> matchingFacade.rematch(new ProviderRematchedEvent(
                carePlanId, regionId, provideServiceId, UUID.randomUUID(), THURSDAY, TimeSlot.MORNING
        ), false)).isInstanceOf(RetryableException.class);

        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("재매칭 중 4xx 응답은 다시 보내도 같은 결과라 재시도하지 않는다")
    void rematch_clientError_doesNotThrow() {
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willThrow(FeignException.errorStatus("ScheduleClient#findSchedules", Response.builder()
                        .status(400)
                        .reason("Bad Request")
                        .request(feignRequest())
                        .headers(Map.of())
                        .build()));

        assertThatCode(() -> matchingFacade.rematch(new ProviderRematchedEvent(
                carePlanId, regionId, provideServiceId, UUID.randomUUID(), THURSDAY, TimeSlot.MORNING
        ), false)).doesNotThrowAnyException();
    }

    // 후보 제공자 1명 + 목요일 09:00~13:00 근무표
    private void givenMatchableCandidate() {
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
    }

    private Request feignRequest() {
        return Request.create(
                Request.HttpMethod.GET, "/internal/v1/service-schedules",
                Map.of(), null, StandardCharsets.UTF_8
        );
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
        ), false)).doesNotThrowAnyException();
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

    @Test
    @DisplayName("같은 제공자의 다른 서비스 종류 일정과 겹치는 시간에는 배정하지 않는다")
    void match_otherServiceOfSameProvider_occupies() {
        UUID otherOfferingId = UUID.randomUUID();

        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .willReturn(List.of(offering(offeringIdA, providerIdA, provideServiceId)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(offeringIdA, "09:00", "13:00")));
        // A 제공자는 다른 서비스 종류(otherOfferingId)로 목요일 09:00~10:00에 일정이 있다
        given(serviceOfferingQueryRepository.findOfferingProviderIdsIncludingDeleted(any()))
                .willReturn(Map.of(offeringIdA, providerIdA, otherOfferingId, providerIdA));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of(new ScheduleSlot(
                        otherOfferingId, THURSDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))));

        matchingFacade.match(event(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)));

        // 후보가 아닌 다른 서비스 종류의 제공 서비스 ID까지 함께 조회한다
        verify(schedulePort).findSchedules(
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of(offeringIdA, otherOfferingId))),
                any()
        );

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort).publishMatched(captor.capture());
        assertThat(captor.getValue().startedAt()).isEqualTo(THURSDAY.atTime(10, 0));
    }

    @Test
    @DisplayName("같은 이벤트에서 서비스 종류가 달라도 같은 제공자의 방금 배정한 시간은 피한다")
    void match_sharesOccupiedAcrossServices() {
        UUID nursingServiceId = UUID.randomUUID();
        UUID careServiceId = UUID.randomUUID();
        UUID nursingOfferingId = UUID.randomUUID();
        UUID careOfferingId = UUID.randomUUID();

        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, nursingServiceId))
                .willReturn(List.of(offering(nursingOfferingId, providerIdA, nursingServiceId)));
        given(serviceOfferingQueryRepository.findAllByRegionIdAndProvideServiceId(regionId, careServiceId))
                .willReturn(List.of(offering(careOfferingId, providerIdA, careServiceId)));
        given(provideWorkQueryRepository.findAllByServiceOfferingIdIn(anyList()))
                .willReturn(List.of(work(nursingOfferingId, "09:00", "13:00")))
                .willReturn(List.of(work(careOfferingId, "09:00", "13:00")));
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.match(new CarePlanConfirmedEvent(carePlanId, regionId, List.of(
                new CarePlanConfirmedEvent.Service(UUID.randomUUID(), nursingServiceId,
                        List.of(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING))),
                new CarePlanConfirmedEvent.Service(UUID.randomUUID(), careServiceId,
                        List.of(preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)))
        )));

        // 서비스 종류가 여러 개여도 Schedule-Service는 한 번만 호출한다
        verify(schedulePort, times(1)).findSchedules(anyList(), any());

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort, times(2)).publishMatched(captor.capture());

        // 방문간호를 09:00에 배정했으므로 같은 제공자의 방문요양은 10:00으로 밀린다
        assertThat(captor.getAllValues().get(0).startedAt()).isEqualTo(THURSDAY.atTime(9, 0));
        assertThat(captor.getAllValues().get(1).startedAt()).isEqualTo(THURSDAY.atTime(10, 0));
    }

    @Test
    @DisplayName("이미 결과가 적재된 희망 일정은 건너뛰고, 그 배정 시간을 피해 나머지를 배정한다")
    void match_skipsProcessedPreference_andKeepsItsSlot() {
        // 재수신 전에 첫 희망 일정만 09:00으로 적재됐고, 아직 Schedule에는 저장되지 않은 상황
        UUID processedPreferenceId = UUID.randomUUID();
        UUID newPreferenceId = UUID.randomUUID();

        given(outboxEventQueryRepository.findAllByAggregateIdIn(any()))
                .willReturn(List.of(matchedResult(processedPreferenceId, offeringIdA, THURSDAY, "09:00")));
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.match(event(
                preference(processedPreferenceId, THURSDAY, TimeSlot.MORNING),
                preference(newPreferenceId, THURSDAY, TimeSlot.MORNING)
        ));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort, times(1)).publishMatched(captor.capture());

        assertThat(captor.getValue().servicePreferenceId()).isEqualTo(newPreferenceId);
        // 복원한 09:00 점유를 피해 10:00으로 배정된다
        assertThat(captor.getValue().startedAt()).isEqualTo(THURSDAY.atTime(10, 0));
    }

    @Test
    @DisplayName("재전달된 재매칭은 같은 날짜 결과가 최근 적재됐으면 건너뛴다")
    void rematch_redelivered_recentSameDate_skips() {
        UUID preferenceId = UUID.randomUUID();

        given(outboxEventQueryRepository.findAllByAggregateIdAndCreatedAtAfter(eq(preferenceId), any()))
                .willReturn(List.of(failedResult(preferenceId, THURSDAY)));

        matchingFacade.rematch(rematchEvent(preferenceId, THURSDAY), true);

        verify(schedulePort, never()).findSchedules(anyList(), any());
        verify(matchingEventPort, never()).publishMatched(any());
        verify(matchingEventPort, never()).publishMatchFailed(any());
    }

    @Test
    @DisplayName("재전달된 재매칭이어도 같은 날짜 결과가 없으면 처리한다")
    void rematch_redelivered_differentDate_processes() {
        UUID preferenceId = UUID.randomUUID();

        given(outboxEventQueryRepository.findAllByAggregateIdAndCreatedAtAfter(eq(preferenceId), any()))
                .willReturn(List.of(failedResult(preferenceId, THURSDAY.plusDays(1))));
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.rematch(rematchEvent(preferenceId, THURSDAY), true);

        verify(matchingEventPort).publishMatched(any());
    }

    @Test
    @DisplayName("재전달이 아닌 재매칭은 적재 이력을 보지 않고 처리한다")
    void rematch_notRedelivered_ignoresHistory() {
        // 같은 날짜로 다시 요청한 정상 재시도는 새 메시지라 그대로 처리돼야 한다
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.rematch(rematchEvent(UUID.randomUUID(), THURSDAY), false);

        verify(outboxEventQueryRepository, never()).findAllByAggregateIdAndCreatedAtAfter(any(), any());
        verify(matchingEventPort).publishMatched(any());
    }

    @Test
    @DisplayName("가운데 희망 일정 적재만 실패하면 나머지는 적재하고 마지막에 예외를 던진다")
    void match_middleAppendFailure_processesRestThenThrows() {
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        willDoNothing()
                .willThrow(new DataAccessResourceFailureException("db down"))
                .willDoNothing()
                .given(matchingEventPort).publishMatched(any());

        assertThatThrownBy(() -> matchingFacade.match(event(
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING),
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING),
                preference(UUID.randomUUID(), THURSDAY, TimeSlot.MORNING)
        ))).isInstanceOf(DataAccessException.class);

        // 실패한 가운데 건 뒤의 희망 일정도 처리됐다
        verify(matchingEventPort, times(3)).publishMatched(any());
    }

    @Test
    @DisplayName("재시도 때는 이미 적재한 희망 일정을 건너뛰고 실패했던 건만 적재한다")
    void match_retry_appendsOnlyFailedPreference() {
        UUID firstId = UUID.randomUUID();
        UUID failedId = UUID.randomUUID();
        UUID thirdId = UUID.randomUUID();

        // 첫 시도에서 첫째(09:00)·셋째(10:00)만 적재된 상태
        given(outboxEventQueryRepository.findAllByAggregateIdIn(any()))
                .willReturn(List.of(
                        matchedResult(firstId, offeringIdA, THURSDAY, "09:00"),
                        matchedResult(thirdId, offeringIdA, THURSDAY, "10:00")
                ));
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());

        matchingFacade.match(event(
                preference(firstId, THURSDAY, TimeSlot.MORNING),
                preference(failedId, THURSDAY, TimeSlot.MORNING),
                preference(thirdId, THURSDAY, TimeSlot.MORNING)
        ));

        ArgumentCaptor<ProviderMatchedEvent> captor = ArgumentCaptor.forClass(ProviderMatchedEvent.class);
        verify(matchingEventPort, times(1)).publishMatched(captor.capture());

        assertThat(captor.getValue().servicePreferenceId()).isEqualTo(failedId);
        // 복원한 09:00·10:00 점유를 피해 11:00에 배정된다
        assertThat(captor.getValue().startedAt()).isEqualTo(THURSDAY.atTime(11, 0));
    }

    @Test
    @DisplayName("재매칭 적재가 DB 오류로 실패하면 리스너 재시도를 받도록 예외를 던진다")
    void rematch_appendFailure_rethrows() {
        givenMatchableCandidate();
        given(schedulePort.findSchedules(anyList(), any()))
                .willReturn(List.of());
        willThrow(new DataAccessResourceFailureException("db down"))
                .given(matchingEventPort).publishMatched(any());

        assertThatThrownBy(() -> matchingFacade.rematch(rematchEvent(UUID.randomUUID(), THURSDAY), false))
                .isInstanceOf(DataAccessException.class);
    }
}
