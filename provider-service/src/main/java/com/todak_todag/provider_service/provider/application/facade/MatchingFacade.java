package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.provider.application.event.*;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPort;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.port.ScheduleSlot;
import com.todak_todag.provider_service.provider.application.support.MatchingService;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// 조회와 발행을 트랜잭션 밖에서 수행하고, 판정은 MatchingService에 위임한다
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingFacade {

    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;
    private final ProvideWorkQueryRepository provideWorkQueryRepository;
    private final MatchingService matchingService;
    private final SchedulePort schedulePort;
    private final MatchingEventPort matchingEventPort;

    public void match(CarePlanConfirmedEvent event) {
        for (CarePlanConfirmedEvent.Service service : event.services()) {
            // 서비스 하나가 실패해 메서드 전체가 죽으면 RabbitMQ가 이벤트를 재전송하고,
            // 앞 서비스에서 이미 발행한 매칭 결과가 다시 발행되어 일정이 중복 생성될 수도 있음
            try {
                matchService(event.carePlanId(), event.regionId(), service);
            } catch (Exception e) {
                log.error("[Provider] 서비스 매칭 처리 실패 carePlanId={} provideServiceId={}",
                        event.carePlanId(), service.provideServiceId(), e);
            }
        }
    }

    public void rematch(ProviderRematchedEvent event) {
        List<ServiceOffering> candidates = serviceOfferingQueryRepository
                .findAllByRegionIdAndProvideServiceId(event.regionId(), event.provideServiceId());

        Map<UUID, List<ProvideWork>> works = loadWorks(candidates);
        List<ScheduleSlot> occupied = new ArrayList<>(loadSchedules(candidates, event.date()));

        matchOne(
                event.carePlanId(), event.regionId(), event.provideServiceId(),
                event.servicePreferenceId(), event.date(), event.preferredTimeSlot(),
                candidates, works, occupied
        );
    }

    private void matchService(UUID carePlanId, UUID regionId, CarePlanConfirmedEvent.Service service) {

        // 희망 일정이 없으면 판정할 대상이 없어 조회도 하지 않는다
        if (service.preferences().isEmpty()) {
            return;
        }

        List<ServiceOffering> candidates = serviceOfferingQueryRepository
                .findAllByRegionIdAndProvideServiceId(regionId, service.provideServiceId());

        Map<UUID, List<ProvideWork>> works = loadWorks(candidates);

        LocalDate startDate = service.preferences().stream()
                .map(CarePlanConfirmedEvent.Preference::preferredDate)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        // 방금 배정한 건은 아직 Schedule에 저장되지 않으므로 메모리에서 누적한다
        List<ScheduleSlot> occupied = new ArrayList<>(loadSchedules(candidates, startDate));

        for (CarePlanConfirmedEvent.Preference preference : service.preferences()) {
            // 희망 일정 1건은 서로 독립적. 한 건이 실패해도 나머지는 계속 처리해야한다
            // 여기서 예외를 가두지 않으면 위와 같은 이유로 중복 발행이 발생할 가능성 있음
            try {
                matchOne(
                        carePlanId, regionId, service.provideServiceId(),
                        preference.servicePreferenceId(), preference.preferredDate(),
                        preference.preferredTimeSlot(),
                        candidates, works, occupied
                );
            } catch (Exception e) {
                log.error("[Provider] 매칭 처리 실패 servicePreferenceId={}",
                        preference.servicePreferenceId(), e);
            }
        }
    }

    private void matchOne(
            UUID carePlanId,
            UUID regionId,
            UUID provideServiceId,
            UUID servicePreferenceId,
            LocalDate date,
            TimeSlot preferredTimeSlot,
            List<ServiceOffering> candidates,
            Map<UUID, List<ProvideWork>> works,
            List<ScheduleSlot> occupied
    ) {
        Optional<MatchingService.Match> matched =
                matchingService.match(candidates, works, occupied, date, preferredTimeSlot);

        if (matched.isEmpty()) {
            log.info("[Provider] 매칭 실패 servicePreferenceId={} date={}", servicePreferenceId, date);

            matchingEventPort.publishMatchFailed(new ProviderMatchFailedEvent(
                    carePlanId, regionId, servicePreferenceId, provideServiceId,
                    date, preferredTimeSlot,
                    ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER, Instant.now()
            ));

            return;
        }

        MatchingService.Match match = matched.get();

        // 같은 이벤트 안의 다음 희망 일정이 같은 제공자에게 같은 시간으로 또 배정되지 않도록 메모리에 반영한다
        occupied.add(new ScheduleSlot(
                match.serviceOfferingId(), date, match.startedAt(), match.finishedAt()
        ));

        // 매칭 결과를 추적할 수 있도록 성공도 남긴다
        log.info("[Provider] 매칭 성공 servicePreferenceId={} serviceOfferingId={} date={} startedAt={}",
                servicePreferenceId, match.serviceOfferingId(), date, match.startedAt());

        matchingEventPort.publishMatched(new ProviderMatchedEvent(
                carePlanId, regionId, servicePreferenceId, provideServiceId,
                match.serviceOfferingId(), date, date.atTime(match.startedAt()), Instant.now()
        ));
    }

    private Map<UUID, List<ProvideWork>> loadWorks(List<ServiceOffering> candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }

        List<UUID> ids = candidates.stream().map(ServiceOffering::getId).toList();

        return provideWorkQueryRepository.findAllByServiceOfferingIdIn(ids).stream()
                .collect(Collectors.groupingBy(ProvideWork::getServiceOfferingId));
    }

    private List<ScheduleSlot> loadSchedules(List<ServiceOffering> candidates, LocalDate startDate) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = candidates.stream().map(ServiceOffering::getId).toList();

        return schedulePort.findSchedules(ids, startDate);
    }
}