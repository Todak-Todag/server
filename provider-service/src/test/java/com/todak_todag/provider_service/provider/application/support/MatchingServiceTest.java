package com.todak_todag.provider_service.provider.application.support;

import com.todak_todag.provider_service.global.common.TimeSlot;
import com.todak_todag.provider_service.provider.application.port.ScheduleSlot;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("매칭 판정")
class MatchingServiceTest {

    // 2026-09-10은 목요일 (day = 4)
    private static final LocalDate THURSDAY = LocalDate.of(2026, 9, 10);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 11);

    private final MatchingService matchingService = new MatchingService();

    private final UUID offeringIdA = UUID.randomUUID();
    private final UUID offeringIdB = UUID.randomUUID();

    private ServiceOffering offering(UUID id) {
        ServiceOffering offering = ServiceOffering.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ReflectionTestUtils.setField(offering, "id", id);

        return offering;
    }

    private ProvideWork work(UUID serviceOfferingId, int day, String startedAt, String finishedAt) {
        return ProvideWork.of(serviceOfferingId, day, LocalTime.parse(startedAt), LocalTime.parse(finishedAt));
    }

    private ScheduleSlot slot(UUID serviceOfferingId, LocalDate date, String startedAt, String finishedAt) {
        return new ScheduleSlot(serviceOfferingId, date, LocalTime.parse(startedAt), LocalTime.parse(finishedAt));
    }

    @Nested
    @DisplayName("요일·시간대 판정")
    class DayAndTimeSlot {

        @Test
        @DisplayName("요일과 시간대가 맞으면 가장 빠른 시각으로 매칭한다")
        void match_earliestStart() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match).isPresent();
            assertThat(match.get().serviceOfferingId()).isEqualTo(offeringIdA);
            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("소요 시간 1시간을 더한 종료 시각을 함께 반환한다")
        void match_finishedAt() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().finishedAt()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("제공 가능 요일이 아니면 매칭되지 않는다")
        void match_dayMismatch() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), FRIDAY, TimeSlot.MORNING);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("희망 시간대와 경계만 닿으면 1시간이 들어가지 않아 매칭되지 않는다")
        void match_timeSlotBoundary() {
            // 09:00~13:00 제공자에게 AFTERNOON(13:00~18:00)을 요청하면 겹치는 구간이 없다
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, TimeSlot.AFTERNOON);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("희망 시간대가 null이면 하루 전체를 대상으로 매칭한다")
        void match_nullTimeSlot() {
            // 14:00~18:00은 MORNING과 겹치지 않지만 하루 전체 기준으로는 매칭된다
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "14:00", "18:00")));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, null);

            assertThat(match).isPresent();
            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(14, 0));
        }

        @Test
        @DisplayName("제공 가능 시간이 없으면 매칭되지 않는다")
        void match_noProvideWork() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, Map.of(), List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("제공 가능 시간이 업무 시간을 벗어나면 매칭되지 않는다")
        void match_outsideBusinessHours() {
            // 23시 이후 구간은 1시간을 더하면 자정을 넘어 되감기므로 시간 비교가 뒤집힌다
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works =
                    Map.of(offeringIdA, List.of(work(offeringIdA, 4, "23:00", "23:59")));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("기존 일정이 업무 시간을 넘겨 끝나도 매칭되지 않는다")
        void match_occupiedRunsPastBusinessHours() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works =
                    Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdA, THURSDAY, "09:00", "23:30"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("후보가 없으면 매칭되지 않는다")
        void match_noCandidate() {
            Optional<MatchingService.Match> match =
                    matchingService.match(List.of(), Map.of(), List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match).isEmpty();
        }
    }

    @Nested
    @DisplayName("기존 일정 차감")
    class OccupiedSlots {

        @Test
        @DisplayName("앞이 차 있으면 그 이후 첫 시각으로 매칭한다")
        void match_afterOccupied() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdA, THURSDAY, "09:00", "10:00"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("뒤가 차 있어도 앞에 1시간이 남으면 앞으로 매칭한다")
        void match_beforeOccupied() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdA, THURSDAY, "11:00", "12:00"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("앞의 틈이 1시간 미만이면 건너뛰고 다음 구간으로 매칭한다")
        void match_skipsNarrowGap() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdA, THURSDAY, "09:30", "10:30"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("남은 구간에 1시간이 들어가지 않으면 매칭되지 않는다")
        void match_noRoomLeft() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdA, THURSDAY, "09:00", "13:00"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match).isEmpty();
        }

        @Test
        @DisplayName("다른 날짜의 일정은 차감 대상이 아니다")
        void match_ignoresOtherDate() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdA, FRIDAY, "09:00", "13:00"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("다른 제공자의 일정은 차감 대상이 아니다")
        void match_ignoresOtherOffering() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")));
            List<ScheduleSlot> occupied = List.of(slot(offeringIdB, THURSDAY, "09:00", "13:00"));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(9, 0));
        }
    }

    @Nested
    @DisplayName("후보 선택")
    class CandidateSelection {

        @Test
        @DisplayName("일정 수가 적은 제공자를 선택한다")
        void match_prefersLessLoaded() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA), offering(offeringIdB));
            Map<UUID, List<ProvideWork>> works = Map.of(
                    offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")),
                    offeringIdB, List.of(work(offeringIdB, 4, "10:00", "18:00"))
            );

            // A는 다른 날짜에 일정이 2건, B는 없다
            List<ScheduleSlot> occupied = List.of(
                    slot(offeringIdA, FRIDAY, "09:00", "10:00"),
                    slot(offeringIdA, FRIDAY, "10:00", "11:00")
            );

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, occupied, THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().serviceOfferingId()).isEqualTo(offeringIdB);
            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("일정 수가 같으면 시작 시각이 빠른 제공자를 선택한다")
        void match_prefersEarlierWhenTied() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdB), offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(
                    offeringIdA, List.of(work(offeringIdA, 4, "09:00", "13:00")),
                    offeringIdB, List.of(work(offeringIdB, 4, "10:00", "18:00"))
            );

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().serviceOfferingId()).isEqualTo(offeringIdA);
            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("제공 가능 시간이 여러 개면 그중 가장 빠른 시각을 쓴다")
        void match_multipleWorks() {
            List<ServiceOffering> candidates = List.of(offering(offeringIdA));
            Map<UUID, List<ProvideWork>> works = Map.of(offeringIdA, List.of(
                    work(offeringIdA, 4, "11:00", "13:00"),
                    work(offeringIdA, 4, "09:00", "10:30")
            ));

            Optional<MatchingService.Match> match =
                    matchingService.match(candidates, works, List.of(), THURSDAY, TimeSlot.MORNING);

            assertThat(match.get().startedAt()).isEqualTo(LocalTime.of(9, 0));
        }
    }
}