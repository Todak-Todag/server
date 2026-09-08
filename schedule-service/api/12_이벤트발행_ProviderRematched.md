# 12. [이벤트 발행] ProviderRematched - 재매칭 알림

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 발행) |
| 사용자 | None |
| 카테고리 | 발행 |
| 테이블명 | `p_service_schedules`, `p_service_matching_attempts`, `p_schedule_outbox_events` |

## 설명

Care Plan 확정 후 첫 매칭에서 실패했거나, 확정되어 있던 서비스 일정을 변경할 때 `ProviderRematched` 이벤트를 RabbitMQ에 발행한다.

> 코드상 이벤트 타입 문자열은 `ProviderReMatched`이며(`ProviderReMatchEventPort.EVENT_TYPE`), 아웃박스 `event_type` 컬럼에도 이 값이 저장된다.
>

## 발행 시나리오

| # | 시나리오 | 트리거 API | 아웃박스 `aggregate_id` | `preferredTimeSlot` |
| --- | --- | --- | --- | --- |
| 1 | Care Plan 확정 후 첫 매칭 실패 → 사용자가 실패 내역(15번)을 보고 희망 날짜/시간대를 재선택 | **16번 재매칭 시도** `POST /api/v1/matching-attempts/{matchingAttemptId}/retry` | `matchingAttemptId` | 사용자가 선택한 값 (선택 안 하면 `null`) |
| 2 | 확정된 서비스 일정을 하루 앞당기거나 미룸 | **03번 서비스 일정 변경** `PATCH /api/v1/service-schedules/{serviceScheduleId}/status` | `serviceScheduleId` | 항상 `null` (시간대를 고르지 않음) |
- 시나리오 1의 "재매칭 시도 API"는 **16번**으로 문서화·구현 완료됐다.
- **`aggregate_id`가 시나리오마다 다른 이유**: 16번은 동기적으로 아무 상태도 바꾸지 않아 "이미 재시도 접수됨"(409)을 `status`로 표현할 수 없어, 아웃박스에 같은 `matchingAttemptId`가 적재됐는지로 판별한다. 03번은 일정이 `RESCHEDULING`으로 전이되므로 상태로 중복을 막을 수 있다.

### 페이로드 출처

| 필드 | 시나리오 1 (16번) | 시나리오 2 (03번) |
| --- | --- | --- |
| `carePlanId` | 재시도 대상 매칭 시도 레코드 | 변경 대상 서비스 일정 |
| `regionId`, `provideServiceId` | 재시도 대상(실패) 매칭 시도 레코드 | 해당 `servicePreferenceId`의 **가장 최근 `MATCHED` 매칭 시도** 레코드 |
| `servicePreferenceId` | 재시도 대상 매칭 시도 레코드 | 변경 대상 서비스 일정 |
| `date`, `preferredTimeSlot` | 사용자 입력 | 사용자 입력(`date`)만, 시간대는 `null` |
- 12번 문서에 남아 있던 "03번 경로의 `regionId`/`provideServiceId` 출처 불명확" 항목은 위와 같이 확정됐다. 최근 `MATCHED` 시도 기록이 없으면 03번은 `404 SERVICE_MATCHING_ATTEMPT_NOT_FOUND`로 실패한다.

## 발행 시점

두 경로 모두 **커맨드 트랜잭션 안에서 아웃박스에 적재만** 하고, 실제 브로커 발행은 커밋 이후 릴레이(`@Scheduled`, 기본 5초 주기, 배치 100건)가 수행한다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `schedule.exchange` (Direct) |
| Routing Key | `schedule.rematched.key` |
| Queue | `provider.schedule-rematched.queue` |
| 재시도 | 3회(아웃박스 `retry_count`), 초과 시 `FAILED`. DLQ 미운용 |

> Queue와 Binding은 발행 측(schedule-service)에서 함께 선언한다 — Direct Exchange는 바인딩된 큐가 없으면 메시지를 조용히 버리기 때문이다.
>

> **재처리(Reprocessing) 기능**: FAILED 이벤트를 재발행/재처리하는 기능은 현재 범위 밖이며 백로그로 관리한다.
>

## 이벤트 페이로드 (Publish)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| regionId | 지역 ID | UUID | - | X | `6b2f9d41-c857-4e13-a690-5d8b2c7f1e34` |
| provideServiceId | 제공할 서비스 ID | UUID | - | X | `94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43` |
| servicePreferenceId | 사용자 희망 일정 ID | UUID | - | X | `3f2504e0-4f89-41d3-9a0c-0305e82c3301` |
| date | 재매칭을 원하는 날짜 | LocalDate | - | X | `2026-09-02` |
| preferredTimeSlot | 재매칭을 원하는 시간대 | Enum(String) | `MORNING`, `AFTERNOON` | O | `MORNING` (03번 경로로 발행 시 `null`) |

**Example**

```jsx
// 03번(서비스 일정 변경) 경로
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "regionId": "6b2f9d41-c857-4e13-a690-5d8b2c7f1e34",
  "provideServiceId": "94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43",
  "servicePreferenceId": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "date": "2026-09-02",
  "preferredTimeSlot": null
}

// 16번(재매칭 시도) 경로
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "regionId": "6b2f9d41-c857-4e13-a690-5d8b2c7f1e34",
  "provideServiceId": "94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43",
  "servicePreferenceId": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "date": "2026-09-10",
  "preferredTimeSlot": "MORNING"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 발행 후 | Provider-Service가 페이로드의 데이터와 해당 날짜에 서비스 제공이 가능한 서비스 제공자를 조회하여 매칭 진행 → 성공 시 `ProviderMatched`(13번) / 실패 시 `ProviderMatchFailed`(14번) 발행 |
