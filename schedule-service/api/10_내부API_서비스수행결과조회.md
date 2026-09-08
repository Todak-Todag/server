# 10. [내부 API] 서비스 수행 결과 조회

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/internal/v1/service-results/{serviceResultId}` |
| 호출 주체 | Care-Plan-Service (서비스 간 내부 호출) |
| 카테고리 | 검색 |
| 테이블명 | `p_care_plan_service_results` |

## 설명

Care-Plan-Service가 `CarePlanCompleted` 이벤트를 수신한 뒤, 이벤트 페이로드에 담긴 `serviceResultId`가 실제로 존재하는 데이터인지 검증하기 위해 호출하는 서비스 간 내부 API다.

## 비즈니스 규칙

- `deleted_at IS NULL`인 수행 결과만 반환한다 (논리 삭제된 레코드는 제외).
- 인증은 `X-Internal-Api-Key` 헤더로 수행하며, `internal.key` 설정값과 대조한다.
- `X-Internal-Api-Key` 검증은 `InternalResponseInterceptor`가 `/internal/v1/**` 전체에 대해 수행하며, Controller는 이 헤더를 직접 처리하지 않는다 (06번과 동일).
- 존재하지 않거나 논리 삭제된 `serviceResultId`는 **`404 SERVICE_RESULTS_NOT_FOUND`** 를 반환한다. 이 API의 목적이 "존재 검증"이므로 다른 API들과 달리 403으로 감추지 않는다.
- **응답은 `serviceResultId` 하나만 반환한다**. 이 API의 목적은 존재 검증뿐이므로 그 외 필드(`serviceScheduleId`/`startedAt`/`finishedAt`/`note`)는 노출하지 않는다.

### 응답은 `serviceResultId` 하나

이 API는 `CarePlanCompleted` 페이로드의 `serviceResultId`가 **실제 존재하는 데이터인지 확인**하는 용도뿐이다. 존재 여부는 `200`(존재) / `404`(미존재)라는 상태 코드로 이미 전달되므로, 본문에는 조회 대상을 되짚어 주는 `serviceResultId`만 담는다.

빠진 필드와 그 이유는 다음과 같다.

| 제외 필드 | 이유 |
| --- | --- |
| `carePlanId` | `CarePlanCompleted` 이벤트 페이로드(`carePlanId`, `serviceResultId`, `status`)로 **이미 전달된다.** 수신 측은 이벤트의 `carePlanId`로 대상 Care Plan을 찾는다. 게다가 `p_care_plan_service_results`에는 `care_plan_id` 컬럼이 없어 반환하려면 `p_service_schedules` 조인이 필요하다 |
| `serviceScheduleId` | 수신 측이 사용하지 않는다. 내부 식별자를 불필요하게 노출하지 않는다 |
| `startedAt` / `finishedAt` | 수신 측은 Care Plan 상태만 전환하며 수행 시각을 쓰지 않는다 |
| `note` | 수행 내용은 수신 측 관심사가 아니다 |

care-plan-service도 응답 DTO(`ScheduleInternalResponse.Data`)를 `serviceResultId` 하나로 맞추기로 확정됐다.

> ⚠️ **구현 정합성**: 현재 schedule-service 코드(`InternalServiceResultResponse`)는 아직 `serviceResultId`/`serviceScheduleId`/`startedAt`/`finishedAt` 4개 필드를 반환한다. **이 문서가 확정 스펙이며, 코드를 여기에 맞춰 축소해야 한다.**
>

## Request

| key | 설명 | value 타입 | 위치 | 제약사항 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- | --- |
| serviceResultId | 조회할 서비스 수행 결과 ID | UUID | Path Variable | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |

## Response

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| serviceResultId | 서비스 수행 결과 ID | UUID | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |

**Example**

```jsx
// 성공
{
  "success": true,
  "code": 200,
  "message": "서비스 수행 결과 조회 성공",
  "data": {
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
| `404` | `SERVICE_RESULTS_NOT_FOUND` | 존재하지 않는(또는 논리 삭제된) 수행 결과 |
