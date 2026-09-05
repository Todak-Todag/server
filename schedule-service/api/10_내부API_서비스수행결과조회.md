# [내부 API] 서비스 수행 결과 조회

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/internal/v1/service-results/{serviceResultId}` |
| 사용자 | care-plan-service (내부 호출) |
| 카테고리 | 검색 |
| 테이블명 | `p_care_plan_service_results` |

## 설명

Care-Plan-Service가 `CarePlanCompleted` 이벤트를 수신한 뒤, 이벤트 페이로드에 담긴 `serviceResultId`가 실제로 존재하는 데이터인지 검증하기 위해 호출하는 서비스 간 내부 API다.

## 비즈니스 규칙

- `deletedAt IS NULL`인 수행 결과만 반환한다 (논리 삭제된 레코드는 제외).
- 인증은 `X-Internal-Api-Key` 헤더로 수행하며, 각 서비스가 환경변수로 보유한 Key와 대조하여 검증한다.
- `X-Internal-Api-Key` 검증은 **Interceptor**에서 수행하며, Controller는 이 헤더를 직접 처리하지 않는다.

## Request

| key | 설명 | value 타입 | 위치 | 제약사항 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- | --- |
| serviceResultId | 조회할 서비스 수행 결과 ID | UUID | Path Variable | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |

## Response

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| serviceResultId | 서비스 수행 결과 ID | UUID | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |
| serviceScheduleId | 서비스 일정 ID | UUID | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |
| startedAt | 시작 일시 | LocalDateTime | - | X | `2026-09-01T09:00:00` |
| finishedAt | 종료 일시 | LocalDateTime | - | X | `2026-09-01T10:00:00` |

**Example**

```jsx
// 성공
{
  "success": true,
  "code": 200,
  "message": "서비스 수행 결과 조회 성공",
  "data": {
    "serviceResultId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "serviceScheduleId": "d3e4f5a6-1234-5678-9abc-def012345678",
    "startedAt": "2026-09-01T09:00:00",
    "finishedAt": "2026-09-01T10:00:00"
  }
}
```

## Status

| status | response content |
| --- | --- |
| `200` | 조회 성공 |
| `401` | `X-Internal-Api-Key`가 없거나 유효하지 않음 |
| `404` | 존재하지 않는 수행 결과 |

## 변경 이력

| 날짜 | 변경 내용 |
| --- | --- |
| 2026-09-04 | 신규 API 문서 최초 작성 |
| 2026-09-04 | `테이블명` 속성 오기 정정 (`p_service_schedules` → `p_care_plan_service_results`, Response 필드 기준) |
| 2026-09-04 | Status 표에 "존재하지 않는 serviceResultId" 케이스가 없음을 확인 필요 항목으로 표시 — 이 API의 본래 목적(존재 검증)에 직결되는 중요한 누락이라 강조 표시 |