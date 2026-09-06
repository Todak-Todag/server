package com.todak_todag.provider_service.global.common;

import java.time.LocalTime;

public enum TimeSlot {

    MORNING(LocalTime.of(9, 0), LocalTime.of(13, 0)),
    AFTERNOON(LocalTime.of(13, 0), LocalTime.of(18, 0));

    // 시간대가 지정되지 않은 재매칭은 하루 전체를 대상으로 한다
    public static final LocalTime DAY_START = LocalTime.of(9, 0);
    public static final LocalTime DAY_END = LocalTime.of(18, 0);

    private final LocalTime startedAt;
    private final LocalTime finishedAt;

    TimeSlot(LocalTime startedAt, LocalTime finishedAt) {
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public static LocalTime startOf(TimeSlot timeSlot) {
        return timeSlot == null ? DAY_START : timeSlot.startedAt;
    }

    public static LocalTime endOf(TimeSlot timeSlot) {
        return timeSlot == null ? DAY_END : timeSlot.finishedAt;
    }
}