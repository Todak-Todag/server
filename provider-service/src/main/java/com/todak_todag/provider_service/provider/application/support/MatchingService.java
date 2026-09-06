package com.todak_todag.provider_service.provider.application.support;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.provider.application.port.ScheduleSlot;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// DB·외부 호출 없이 주어진 데이터만으로 매칭을 판정한다
@Component
public class MatchingService {

    // 서비스 소요 시간은 종류와 무관하게 1시간 고정
    private static final int SERVICE_HOURS = 1;

    // finishedAt을 함께 담아 소요 시간 계산을 이 클래스 안으로 모은다
    // Facade가 plusHours(1)을 따로 하면 SERVICE_HOURS와 두 곳으로 흩어짐
    public record Match(UUID serviceOfferingId, LocalTime startedAt, LocalTime finishedAt) {
    }

    private record Candidate(UUID serviceOfferingId, LocalTime startedAt, long scheduleCount) {
    }

    public Optional<Match> match(
            List<ServiceOffering> candidates,
            Map<UUID, List<ProvideWork>> worksByOffering,
            List<ScheduleSlot> occupied,
            LocalDate date,
            TimeSlot preferredTimeSlot
    ) {
        int day = date.getDayOfWeek().getValue();
        LocalTime slotStart = TimeSlot.startOf(preferredTimeSlot);
        LocalTime slotEnd = TimeSlot.endOf(preferredTimeSlot);

        return candidates.stream()
                .map(offering -> toCandidate(offering, worksByOffering, occupied, date, day, slotStart, slotEnd))
                .flatMap(Optional::stream)
                // 특정 제공자에게 일정이 몰리지 않도록 부하가 적은 쪽을 고른다
                .min(Comparator.comparingLong(Candidate::scheduleCount)
                        .thenComparing(Candidate::startedAt))
                .map(candidate -> new Match(
                        candidate.serviceOfferingId(),
                        candidate.startedAt(),
                        candidate.startedAt().plusHours(SERVICE_HOURS)
                ));
    }

    private Optional<Candidate> toCandidate(
            ServiceOffering offering,
            Map<UUID, List<ProvideWork>> worksByOffering,
            List<ScheduleSlot> occupied,
            LocalDate date,
            int day,
            LocalTime slotStart,
            LocalTime slotEnd
    ) {
        List<ScheduleSlot> occupiedOnDate = occupied.stream()
                .filter(slot -> slot.serviceOfferingId().equals(offering.getId()))
                .filter(slot -> slot.date().equals(date))
                .sorted(Comparator.comparing(ScheduleSlot::startedAt))
                .toList();

        return worksByOffering.getOrDefault(offering.getId(), List.of()).stream()
                .filter(work -> work.getDay() == day)
                .map(work -> earliestStart(work, occupiedOnDate, slotStart, slotEnd))
                .flatMap(Optional::stream)
                .min(Comparator.naturalOrder())
                .map(startedAt -> new Candidate(
                        offering.getId(),
                        startedAt,
                        countSchedules(occupied, offering.getId())
                ));
    }

    // 제공 가능 시간과 희망 시간대의 교집합에서, 기존 일정을 뺀 뒤 1시간이 들어가는 첫 시각을 찾는다
    private Optional<LocalTime> earliestStart(
            ProvideWork work,
            List<ScheduleSlot> occupiedOnDate,
            LocalTime slotStart,
            LocalTime slotEnd
    ) {
        LocalTime rangeStart = max(work.getStartedAt(), slotStart);
        LocalTime rangeEnd = min(work.getFinishedAt(), slotEnd);

        LocalTime cursor = rangeStart;

        for (ScheduleSlot slot : occupiedOnDate) {
            if (!slot.finishedAt().isAfter(cursor)) {
                continue;
            }

            if (fits(cursor, slot.startedAt(), rangeEnd)) {
                return Optional.of(cursor);
            }

            cursor = slot.finishedAt();
        }

        return fits(cursor, rangeEnd, rangeEnd) ? Optional.of(cursor) : Optional.empty();
    }

    // cursor부터 1시간이 limit과 rangeEnd를 모두 넘지 않는지
    private boolean fits(LocalTime cursor, LocalTime limit, LocalTime rangeEnd) {
        // 이미 구간 끝을 지난 시각은 배정 대상이 아니다
        // 23:00에 1시간을 더하면 00:00이 되어 아래 비교가 전부 통과해버리므로 여기서 먼저 막는다
        if (!cursor.isBefore(limit) || !cursor.isBefore(rangeEnd)) {
            return false;
        }

        LocalTime end = cursor.plusHours(SERVICE_HOURS);

        return !end.isAfter(limit) && !end.isAfter(rangeEnd);
    }

    private long countSchedules(List<ScheduleSlot> occupied, UUID serviceOfferingId) {
        return occupied.stream()
                .filter(slot -> slot.serviceOfferingId().equals(serviceOfferingId))
                .count();
    }

    private LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}