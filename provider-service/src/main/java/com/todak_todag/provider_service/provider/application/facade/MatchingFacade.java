package com.todak_todag.provider_service.provider.application.facade;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.event.*;
import com.todak_todag.provider_service.provider.application.port.MatchingEventPort;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.support.MatchingService;
import com.todak_todag.provider_service.provider.domain.entity.OutboxEventType;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ProviderOutboxEvent;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.OutboxEventQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideWorkQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// 조회와 발행을 트랜잭션 밖에서 수행하고, 판정은 MatchingService에 위임한다
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingFacade {

    // 브로커 재전달은 컨슈머가 끊긴 직후 일어난다. 이보다 오래된 같은 날짜 결과는 이전의 정상 요청으로 본다
    // ponytail: 요청 ID가 없어 시간 기준 추정. Schedule이 messageId를 보내면 그 값으로 교체
    private static final Duration REDELIVERY_WINDOW = Duration.ofMinutes(10);

    private final ServiceOfferingQueryRepository serviceOfferingQueryRepository;
    private final ProvideWorkQueryRepository provideWorkQueryRepository;
    private final MatchingService matchingService;
    private final SchedulePort schedulePort;
    private final MatchingEventPort matchingEventPort;
    private final OutboxEventQueryRepository outboxEventQueryRepository;
    private final ObjectMapper objectMapper;

    public void match(CarePlanConfirmedEvent event) {
        // 외부 조회를 모두 끝낸 뒤에 적재를 시작한다
        // 조회 중 실패하면 아직 아무것도 적재되지 않은 상태라, 이벤트를 다시 던져
        // 리스너 재시도에 맡겨도 앞 서비스의 결과가 중복 발행되지 않는다
        Prepared prepared;

        try {
            prepared = prepare(event);
        } catch (RuntimeException e) {
            // 외부 서비스 장애·DB 조회 실패는 잠시 뒤 성공할 수 있어 리스너 재시도에 맡긴다
            // (재시도를 소진하면 default-requeue-rejected: false 설정에 따라 폐기된다)
            if (isRetryable(e)) {
                log.warn("[Provider] 일시적 장애로 매칭 재시도 carePlanId={}", event.carePlanId());

                throw e;
            }

            log.error("[Provider] 매칭 준비 실패 carePlanId={}", event.carePlanId(), e);

            return;
        }

        // 한 희망 일정의 적재가 실패해도 나머지는 끝까지 처리한다
        // 재시도할 실패가 있었으면 마지막에 던져 리스너 재시도를 받는다
        // 재시도 때 이미 적재한 희망 일정은 처리 기록으로 건너뛰므로 중복되지 않는다
        RuntimeException retryableFailure = null;

        for (MatchingTarget target : prepared.targets()) {
            RuntimeException failure = apply(
                    event.carePlanId(), event.regionId(), target, prepared.occupied(), prepared.processedPreferenceIds()
            );

            if (retryableFailure == null) {
                retryableFailure = failure;
            }
        }

        if (retryableFailure != null) {
            log.warn("[Provider] 적재에 실패한 희망 일정이 있어 매칭 재시도 carePlanId={}", event.carePlanId());

            throw retryableFailure;
        }
    }

    public void rematch(ProviderRematchedEvent event, boolean redelivered) {
        // 적재에 성공한 뒤에는 예외를 던지지 않는다 (재처리 시 중복 방지)
        // 외부 장애와 DB 적재 실패는 잠시 뒤 성공할 수 있어 리스너 재시도에 맡긴다
        // 한 건만 처리하므로 적재가 실패했다면 성공분이 없어 다시 처리해도 중복되지 않는다
        // (재시도를 소진하면 default-requeue-rejected: false 설정에 따라 폐기된다)
        try {
            // 재전달된 메시지만 확인한다. 사용자의 정상 재요청은 새 메시지라 redelivered가 false다
            if (redelivered && alreadyRematched(event)) {
                log.info("[Provider] 이미 처리한 재매칭 요청의 재전달이라 건너뜀 servicePreferenceId={} date={}",
                        event.servicePreferenceId(), event.date());

                return;
            }

            List<ServiceOffering> candidates = serviceOfferingQueryRepository
                    .findAllByRegionIdAndProvideServiceId(event.regionId(), event.provideServiceId());

            Map<UUID, List<ProvideWork>> works = loadWorks(candidates);
            List<MatchingService.OccupiedSlot> occupied = loadOccupied(candidates, event.date());

            matchOne(
                    event.carePlanId(), event.regionId(), event.provideServiceId(),
                    event.servicePreferenceId(), event.date(), event.preferredTimeSlot(),
                    candidates, works, occupied
            );
        } catch (RuntimeException e) {
            if (isRetryable(e)) {
                log.warn("[Provider] 일시적 장애로 재매칭 재시도 servicePreferenceId={} date={}",
                        event.servicePreferenceId(), event.date());

                throw e;
            }

            log.error("[Provider] 재매칭 처리 실패 carePlanId={} servicePreferenceId={} date={}",
                    event.carePlanId(), event.servicePreferenceId(), event.date(), e);
        }
    }

    // 매칭 판정에 필요한 조회 결과를 서비스 종류 단위로 모아둔다
    private record MatchingTarget(
            CarePlanConfirmedEvent.Service service,
            List<ServiceOffering> candidates,
            Map<UUID, List<ProvideWork>> works
    ) {
    }

    // 점유는 한 이벤트의 모든 서비스 종류가 공유한다
    // 같은 제공자가 여러 종류를 제공하면, 방금 배정한 방문간호 시간이 방문요양 판정에도 반영돼야 한다
    private record Prepared(
            List<MatchingTarget> targets,
            List<MatchingService.OccupiedSlot> occupied,
            // 이전 수신에서 결과를 이미 적재한 희망 일정
            Set<UUID> processedPreferenceIds
    ) {
    }

    // 외부 조회 단계
    // 여기서 실패하면 아직 적재된 것이 없으므로 호출자가 안전하게 예외를 다시 던질 수 있다
    private Prepared prepare(CarePlanConfirmedEvent event) {
        // CarePlanConfirmed는 Care Plan당 한 번이라, 결과가 이미 적재된 희망 일정은 재수신이다
        List<ProviderOutboxEvent> processed = loadProcessed(event);
        Set<UUID> processedPreferenceIds = processed.stream()
                .map(ProviderOutboxEvent::getAggregateId)
                .collect(Collectors.toSet());

        List<MatchingTarget> targets = new ArrayList<>();

        for (CarePlanConfirmedEvent.Service service : event.services()) {
            // 희망 일정이 없으면 판정할 대상이 없어 조회도 하지 않는다
            if (service.preferences().isEmpty()) {
                continue;
            }

            List<ServiceOffering> candidates = serviceOfferingQueryRepository
                    .findAllByRegionIdAndProvideServiceId(event.regionId(), service.provideServiceId());

            targets.add(new MatchingTarget(service, candidates, loadWorks(candidates)));
        }

        List<ServiceOffering> allCandidates = targets.stream()
                .flatMap(target -> target.candidates().stream())
                .toList();

        // 후보가 하나도 없으면 점유를 볼 제공자가 없어 Schedule-Service를 호출하지 않는다
        if (allCandidates.isEmpty()) {
            return new Prepared(targets, new ArrayList<>(), processedPreferenceIds);
        }

        // 모든 서비스 종류의 점유를 한 번에 조회한다
        // 희망 날짜는 모두 같은 Care Plan 기간(30일) 안에 있어, 가장 이른 날짜부터 30일이면 전부 포함된다
        LocalDate startDate = targets.stream()
                .flatMap(target -> target.service().preferences().stream())
                .map(CarePlanConfirmedEvent.Preference::preferredDate)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        List<MatchingService.OccupiedSlot> occupied = loadOccupied(allCandidates, startDate);
        restoreOccupied(occupied, processed, allCandidates);

        return new Prepared(targets, occupied, processedPreferenceIds);
    }

    // 적재 단계
    // 외부 호출 없이 판정하고 아웃박스에 쌓는다
    // 재시도할 실패가 있었으면 첫 번째 것을 돌려준다
    private RuntimeException apply(
            UUID carePlanId,
            UUID regionId,
            MatchingTarget target,
            List<MatchingService.OccupiedSlot> occupied,
            Set<UUID> processedPreferenceIds
    ) {
        RuntimeException retryableFailure = null;

        for (CarePlanConfirmedEvent.Preference preference : target.service().preferences()) {
            if (processedPreferenceIds.contains(preference.servicePreferenceId())) {
                log.info("[Provider] 이미 처리한 희망 일정이라 건너뜀 servicePreferenceId={}",
                        preference.servicePreferenceId());

                continue;
            }

            // 희망 일정 1건은 서로 독립적. 한 건이 실패해도 나머지는 계속 처리해야한다
            try {
                matchOne(
                        carePlanId, regionId, target.service().provideServiceId(),
                        preference.servicePreferenceId(), preference.preferredDate(),
                        preference.preferredTimeSlot(),
                        target.candidates(), target.works(), occupied
                );
            } catch (RuntimeException e) {
                log.error("[Provider] 매칭 처리 실패 servicePreferenceId={}",
                        preference.servicePreferenceId(), e);

                if (retryableFailure == null && isRetryable(e)) {
                    retryableFailure = e;
                }
            }
        }

        return retryableFailure;
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
            List<MatchingService.OccupiedSlot> occupied
    ) {
        Optional<MatchingService.Match> matched =
                matchingService.match(candidates, works, occupied, date, preferredTimeSlot);

        if (matched.isEmpty()) {
            ProviderMatchFailedEvent failedEvent = new ProviderMatchFailedEvent(
                    carePlanId, regionId, servicePreferenceId, provideServiceId,
                    date, preferredTimeSlot,
                    ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER, Instant.now()
            );

            log.info("[Provider] 매칭 실패 servicePreferenceId={} date={}", servicePreferenceId, date);

            try {
                matchingEventPort.publishMatchFailed(failedEvent);
            } catch (Exception e) {
                // 아웃박스 적재가 실패하면 Schedule은 매칭 실패 사실을 알지 못한 채 RESCHEDULING에 머문다
                // 릴레이가 집어갈 레코드조차 없으므로 페이로드 전체를 남긴다
                log.error("[Provider] 매칭 실패 이벤트 적재 실패 event={}", failedEvent, e);

                throw e;
            }

            return;
        }

        MatchingService.Match match = matched.get();

        ProviderMatchedEvent matchedEvent = new ProviderMatchedEvent(
                carePlanId, regionId, servicePreferenceId, provideServiceId,
                match.serviceOfferingId(), date, date.atTime(match.startedAt()), Instant.now()
        );

        try {
            matchingEventPort.publishMatched(matchedEvent);
        } catch (Exception e) {
            // 아웃박스 적재가 실패하면 이 배정은 어디에도 남지 않는다
            // 릴레이가 집어갈 레코드조차 없으므로 페이로드 전체를 남긴다
            log.error("[Provider] 매칭 결과 적재 실패 event={}", matchedEvent, e);

            throw e;
        }

        // 적재에 성공한 뒤에야 메모리에 반영한다
        // 실패한 배정을 미리 넣으면 뒤따르는 희망 일정이 존재하지 않는 일정을 피해 배정된다
        occupied.add(new MatchingService.OccupiedSlot(
                match.providerId(), date, match.startedAt(), match.finishedAt()
        ));

        // 매칭 결과를 추적할 수 있도록 성공도 남긴다
        log.info("[Provider] 매칭 성공 servicePreferenceId={} serviceOfferingId={} date={} startedAt={}",
                servicePreferenceId, match.serviceOfferingId(), date, match.startedAt());
    }

    private Map<UUID, List<ProvideWork>> loadWorks(List<ServiceOffering> candidates) {
        if (candidates.isEmpty()) {
            return Map.of();
        }

        List<UUID> ids = candidates.stream().map(ServiceOffering::getId).toList();

        return provideWorkQueryRepository.findAllByServiceOfferingIdIn(ids).stream()
                .collect(Collectors.groupingBy(ProvideWork::getServiceOfferingId));
    }

    private List<ProviderOutboxEvent> loadProcessed(CarePlanConfirmedEvent event) {
        Set<UUID> preferenceIds = event.services().stream()
                .flatMap(service -> service.preferences().stream())
                .map(CarePlanConfirmedEvent.Preference::servicePreferenceId)
                .collect(Collectors.toSet());

        if (preferenceIds.isEmpty()) {
            return List.of();
        }

        return outboxEventQueryRepository.findAllByAggregateIdIn(preferenceIds);
    }

    // 재수신 전에 적재한 배정은 아직 Schedule에 저장되지 않았을 수 있어 점유에 다시 넣는다
    // 이미 Schedule에 반영돼 조회된 구간은 같은 값이라 중복으로 넣지 않는다
    private void restoreOccupied(
            List<MatchingService.OccupiedSlot> occupied,
            List<ProviderOutboxEvent> processed,
            List<ServiceOffering> candidates
    ) {
        Map<UUID, UUID> providerIdByOfferingId = candidates.stream()
                .collect(Collectors.toMap(ServiceOffering::getId, ServiceOffering::getProviderId, (a, b) -> a));

        processed.stream()
                .filter(outboxEvent -> outboxEvent.getEventType() == OutboxEventType.PROVIDER_MATCHED)
                .map(outboxEvent -> objectMapper.readValue(outboxEvent.getPayload(), ProviderMatchedEvent.class))
                .filter(matched -> providerIdByOfferingId.containsKey(matched.serviceOfferingId()))
                .map(matched -> MatchingService.OccupiedSlot.startingAt(
                        providerIdByOfferingId.get(matched.serviceOfferingId()),
                        matched.date(),
                        matched.startedAt().toLocalTime()
                ))
                .filter(slot -> !occupied.contains(slot))
                .forEach(occupied::add);
    }

    // 같은 희망 일정·같은 날짜의 결과가 최근에 적재됐는지
    private boolean alreadyRematched(ProviderRematchedEvent event) {
        Instant since = Instant.now().minus(REDELIVERY_WINDOW);

        return outboxEventQueryRepository
                .findAllByAggregateIdAndCreatedAtAfter(event.servicePreferenceId(), since).stream()
                .map(this::resultDate)
                .anyMatch(event.date()::equals);
    }

    private LocalDate resultDate(ProviderOutboxEvent outboxEvent) {
        return switch (outboxEvent.getEventType()) {
            case PROVIDER_MATCHED ->
                    objectMapper.readValue(outboxEvent.getPayload(), ProviderMatchedEvent.class).date();
            case PROVIDER_MATCH_FAILED ->
                    objectMapper.readValue(outboxEvent.getPayload(), ProviderMatchFailedEvent.class).date();
        };
    }

    // 겹침과 부하는 제공자 단위로 본다
    // 후보 제공자가 가진 다른 서비스 종류·삭제된 제공 서비스의 일정도 그 제공자의 시간을 차지한다
    private List<MatchingService.OccupiedSlot> loadOccupied(List<ServiceOffering> candidates, LocalDate startDate) {
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // 후보 자신은 제공자를 이미 알고 있어, 조회 결과와 무관하게 매핑을 보장한다
        Map<UUID, UUID> providerIdByOfferingId = new HashMap<>();
        candidates.forEach(offering -> providerIdByOfferingId.put(offering.getId(), offering.getProviderId()));

        providerIdByOfferingId.putAll(serviceOfferingQueryRepository.findOfferingProviderIdsIncludingDeleted(
                candidates.stream().map(ServiceOffering::getProviderId).collect(Collectors.toSet())
        ));

        // 방금 배정한 건은 아직 Schedule에 저장되지 않으므로 호출자가 이 목록에 누적한다
        return schedulePort.findSchedules(List.copyOf(providerIdByOfferingId.keySet()), startDate).stream()
                .map(slot -> new MatchingService.OccupiedSlot(
                        providerIdByOfferingId.get(slot.serviceOfferingId()),
                        slot.date(), slot.startedAt(), slot.finishedAt()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // 외부 서비스 장애나 DB 적재 실패처럼 잠시 뒤 성공할 수 있는 실패인지
    // 재시도할 가치가 있는 것만 다시 던져 리스너 재시도를 받는다
    private boolean isRetryable(RuntimeException e) {
        // 연결 실패·타임아웃은 status가 -1, 상대 서버 오류는 5xx
        // 4xx는 다시 보내도 같은 결과라 재시도하지 않는다
        if (e instanceof FeignException feignException) {
            return feignException.status() < 0 || feignException.status() >= 500;
        }

        // 아웃박스 적재·처리 기록 조회 실패 (커밋 실패는 TransactionException으로 온다)
        if (e instanceof DataAccessException || e instanceof TransactionException) {
            return true;
        }

        return e instanceof BusinessException businessException
                && businessException.getErrorCode() == ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE;
    }
}