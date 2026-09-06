# 11. [이벤트 발행] CarePlanCompleted - 서비스 수행 완료 알림

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 발행) |
| 사용자 | None |
| 카테고리 | 발행 |
| 테이블명 | `p_service_schedules` |

## 설명

서비스 수행 결과 등록으로 케어플랜의 서비스가 모두 완료되면 `CarePlanCompleted` 이벤트를 RabbitMQ에 발행한다.

## 발행 조건

`p_service_schedules`에서 `care_plan_id`로 목록을 조회한 뒤, 가장 마지막에 확정된 일정의 수행 결과가 기록되는 시점에 발행한다.

- 재매칭이 완료되어 새 일정이 잡힌 경우 → 그 일정이 수행 완료된 시점에 발행
- 재매칭에 실패해 취소된 경우 → 더 이상 남은 일정이 없으므로, 마지막으로 수행되었던 결과 ID를 조회하여 반환

## 발행 시점

서비스 수행 결과 등록 트랜잭션의 **커밋 시점**이며, **아웃박스 패턴**을 사용한다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `schedule.exchange` |
| Routing Key | `schedule.completed.key` |
| Queue | `care-plan.schedule-completed.queue` |
| 재시도 | 3회, DLQ 미운용 |

> **재처리(Reprocessing) 기능**: 트래픽이 증가하면 현재의 "운영자가 FAILED 건을 직접 확인 후 DB로 수동 처리"하는 방식은 유지가 어렵다. FAILED 이벤트를 재발행하거나 재처리할 수 있는 기능을 별도로 추가 개발할 예정이다 (현재 범위 밖, 백로그로 관리).
>

## 이벤트 페이로드 (Publish)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| serviceResultId | 서비스 수행 결과 ID | UUID | - | X | `550e8400-e29b-41d4-a716-446655440000` |

**Example**

```jsx
{
  "serviceResultId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 발행 후 | Care-Plan-Service가 수신 → `serviceResultId`로 결과 데이터 조회(10번 Internal API 사용) → 유효한(존재하는) 데이터인 경우에 한해 `p_care_plans.status`를 `COMPLETED`로 변경. 그 외 추가 로직 없음 |

## 변경 이력

| 날짜 | 변경 내용 |
| --- | --- |
| 2026-09-06 | 문서 최초 작성 (Notion 원본 확인 및 정리) — 발행 조건, 메시징 정보(Exchange/Routing Key/Queue/재시도), 페이로드, 처리 결과 전체 확정 상태로 확인됨. 10번 API와의 연관 관계 명시 |
