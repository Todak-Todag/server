# 10. [내부 API] 서비스 수행 결과 조회

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/internal/v1/service-results/{serviceResultId}` |
| 호출 주체 | Care-Plan-Service (서비스 간 내부 호출) |
| 카테고리 | 검색 |
| 테이블명 | `p_care_plan_service_results` (+ `p_service_schedules` 조인) |

## 설명

Care-Plan-Service가 `CarePlanCompleted` 이벤트를 수신한 뒤, 이벤트 페이로드에 담긴 `serviceResultId`가 실제로 존재하는 데이터인지, 그리고 **그 수행 결과가 이벤트의 `carePlanId`에 속한 것이 맞는지** 교차 검증하기 위해 호출하는 서비스 간 내부 API다.

## 비즈니스 규칙

- `deleted_at IS NULL`인 수행 결과만 반환한다 (논리 삭제된 레코드는 제외).
- 인증은 `X-Internal-Api-Key` 헤더로 수행하며, `internal.key` 설정값과 대조한다.
- `X-Internal-Api-Key` 검증은 `InternalResponseInterceptor`가 `/internal/v1/**` 전체에 대해 수행하며, Controller는 이 헤더를 직접 처리하지 않는다 (06번과 동일).
- 존재하지 않거나 논리 삭제된 `serviceResultId`는 **`404 SERVICE_RESULTS_NOT_FOUND`** 를 반환한다. 이 API의 목적이 "존재 검증"이므로 다른 API들과 달리 403으로 감추지 않는다.
- **응답은 `carePlanId`와 `serviceResultId` 두 개만 반환한다**. 그 외 필드(`serviceScheduleId`/`startedAt`/`finishedAt`/`note`)는 노출하지 않는다.

### `carePlanId`를 함께 반환하는 이유

수신 측(care-plan-service)은 `CarePlanCompletedEventValidator.validateCarePlanId(...)`에서 **이벤트 페이로드의 `carePlanId`와 이 API가 반환한 `carePlanId`를 대조**한다. 즉 "이 `serviceResultId`가 존재하는가"에 더해 "그 결과가 정말 이 케어플랜의 것인가"까지 검증한다. 존재 여부만으로는 다른 케어플랜의 `serviceResultId`가 섞여 들어온 경우를 걸러낼 수 없기 때문이다.

`p_care_plan_service_results`에는 `care_plan_id` 컬럼이 없으므로, `service_schedule_id`(논리 FK)로 `p_service_schedules`를 조인해 `care_plan_id`를 가져온다. 조인은 조회 Repository(`CarePlanServiceResultQueryRepositoryImpl.findCarePlanIdByServiceResultId`)에서 **쿼리 한 번**으로 수행한다 — 같은 Impl의 `search()`가 이미 동일한 on절 조인을 쓰고 있어 일관된다.

**결과는 있는데 조인 대상 일정이 없거나 논리 삭제된 경우**(데이터 정합성이 깨진 상황)에도 `404 SERVICE_RESULTS_NOT_FOUND`를 반환한다. `carePlanId`를 확보하지 못하면 이 API의 목적인 교차 검증 자체가 불가능하므로, 결과 미존재와 동일하게 취급한다.

빠진 필드와 그 이유는 다음과 같다.

| 제외 필드 | 이유 |
| --- | --- |
| `serviceScheduleId` | 수신 측이 사용하지 않는다. 내부 식별자를 불필요하게 노출하지 않는다 |
| `startedAt` / `finishedAt` | 수신 측은 Care Plan 상태만 전환하며 수행 시각을 쓰지 않는다 |
| `note` | 수행 내용은 수신 측 관심사가 아니다 |

> 📌 **스펙 변경 이력 (2026-09-17)**: 이전 스펙은 "응답은 `serviceResultId` 하나"였고 `carePlanId`는 "이벤트 페이로드로 이미 전달되므로 제외" 대상이었다. 그러나 care-plan-service가 `ScheduleInternalResponse.Data(serviceResultId, carePlanId)`로 받아 교차 검증하도록 구현되어 있어, **`carePlanId`를 포함하는 쪽으로 확정**했다. 양쪽 코드 모두 이 스펙을 따른다.
>

## Request

| key | 설명 | value 타입 | 위치 | 제약사항 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- | --- |
| serviceResultId | 조회할 서비스 수행 결과 ID | UUID | Path Variable | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |

## Response

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 수행 결과가 속한 Care Plan ID (조인된 서비스 일정에서 확보) | UUID | - | X | `a1b2c3d4-1234-5678-9abc-def012345678` |
| serviceResultId | 서비스 수행 결과 ID | UUID | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |

**Example**

```jsx
// 성공
{
  "success": true,
  "code": 200,
  "message": "서비스 수행 결과 조회 성공",
  "data": {
    "carePlanId": "a1b2c3d4-1234-5678-9abc-def012345678",
    "serviceResultId": "d3e4f5a6-1234-5678-9abc-def012345678"
  }
}

// 실패
{
  "success": false,
  "code": "SERVICE_RESULTS_NOT_FOUND",
  "message": "존재하지 않는 서비스 수행 결과입니다.",
  "details": {
    "reason": "존재하지 않는 서비스 수행 결과입니다."
  },
  "timestamp": "2026-09-04T10:00:00Z"
}
```

## Status

| status | ErrorCode | response content |
| --- | --- | --- |
| `200` | - | 조회 성공 |
| `400` | `INVALID_PARAMETER` | `serviceResultId`가 UUID 형식이 아님 |
| `401` | `UNAUTHORIZED_INTERNAL_REQUEST` | `X-Internal-Api-Key`가 없거나 유효하지 않음 |
| `404` | `SERVICE_RESULTS_NOT_FOUND` | 존재하지 않는(또는 논리 삭제된) 수행 결과 / 조인 대상 일정이 없거나 논리 삭제되어 `carePlanId`를 확보할 수 없음 |
