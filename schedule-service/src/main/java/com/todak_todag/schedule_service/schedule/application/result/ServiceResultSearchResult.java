package com.todak_todag.schedule_service.schedule.application.result;

import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;

import java.time.LocalDateTime;
import java.util.UUID;

// 서비스 수행 결과 목록 조회 결과 (08번 문서 Response 표 기준)
// 08번 문서의 Response 표는 serviceResultId / startedAt / finishedAt 3개 필드만 정의한다.
// 참고: 목록에서 어떤 일정에 대한 결과인지 알 수 있는 serviceScheduleId가 없다는 점은 08번 문서와
//      schedule-service.md 8장에 "참고 (영향 미확정)"으로만 남아 있어, 임의로 추가하지 않고 문서 표를 그대로 따른다.
public record ServiceResultSearchResult(
        UUID serviceResultId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public static ServiceResultSearchResult from(CarePlanServiceResult carePlanServiceResult) {
        return new ServiceResultSearchResult(
                carePlanServiceResult.getServiceResultId(),
                carePlanServiceResult.getStartedAt(),
                carePlanServiceResult.getFinishedAt()
        );
    }
}
