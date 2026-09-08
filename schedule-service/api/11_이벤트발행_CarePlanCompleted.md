# 11. [이벤트 발행] CarePlanCompleted - 서비스 수행 완료 알림

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 발행) |
| 사용자 | None |
| 카테고리 | 발행 |
| 테이블명 | `p_service_schedules`, `p_care_plan_service_results`, `p_schedule_outbox_events` |

## 설명

케어플랜에 속한 서비스 일정이 모두 결말나면 `CarePlanCompleted` 이벤트를 RabbitMQ에 발행한다. "결말"에는 정상 수행(`COMPLETED`)뿐 아니라 미수행(`NO_SHOW`)과 취소(`CANCELED`)도 포함된다.

발행 판단과 아웃박스 적재는 `application/event/CarePlanCompletionEventAppender`가 담당한다.

## 발행 조건

**아래 두 조건을 모두 만족할 때** 아웃박스에 적재한다.

1. 해당 케어플랜에 **아직 끝나지 않은 일정이 하나도 없다.**
   - "끝나지 않음" = `status`가 `SCHEDULED`/`RESCHEDULING`이거나, `COMPLETED`/`NO_SHOW`인데 **수행 결과가 아직 등록되지 않은** 경우
   - 케어플랜이 끝났다는 것의 정의 그 자체이며, 어떤 일정이 트리거였는지와 무관하다.
2. 해당 케어플랜으로 `CarePlanCompleted`가 **아직 적재된 적이 없다.**
   - 아웃박스의 `event_type = 'CarePlanCompleted' AND aggregate_id = carePlanId` 존재 여부로 판별하는 멱등 장치다.
   - 이미 결말난 일정들의 수행 결과가 뒤늦게 등록되어도 중복 발행되지 않는다.
- 조건 1이 **조급한 발행**을, 조건 2가 **중복 발행**을 막는다.

### 마지막 일정 선정 기준

페이로드의 기준이 되는 "마지막 일정"은 트리거가 된 일정이 아니라 **다시 조회한 결과**다. 트리거가 반드시 마지막 일정이라는 보장이 없기 때문이다.

| 항목 | 값 |
| --- | --- |
| 정렬 | `finished_at DESC`, `created_at DESC` (동일 시각 tie-break) |
| 제외 | `status = CHANGED`, `deleted_at IS NOT NULL` |
| 개수 | 1건 (조회 실패 시 트리거 일정으로 폴백) |
- **`date`가 아니라 `finished_at`을 쓰는 이유**: 한 케어플랜은 서비스 종류(`service_preference`)가 여러 개라 같은 `date`에 일정이 여러 건 존재할 수 있어 `date`만으로는 마지막이 1건으로 정해지지 않는다.
- **`CHANGED`를 제외하는 이유**: `CHANGED`는 재매칭 성공으로 새 레코드에 자리를 넘긴 과거 이력이다. 일정은 하루 앞당기는 것도 가능하므로 대체된 레코드가 대체한 레코드보다 `finished_at`이 더 늦을 수 있고, 이 경우 이미 끝난 이력이 영원히 "마지막"을 차지해 이벤트가 발행되지 않는다.

### 재매칭 케이스

- 재매칭 성공 시 기존 일정은 `CHANGED`로 남고 **새 일정 레코드가 `SCHEDULED`로 생성**된다 (13번 구현 완료). `CHANGED`는 마지막 일정 후보에서 제외되므로 새 일정이 수행 완료된 시점에 발행된다.
- 재매칭 실패 시에는 기존 일정이 `SCHEDULED`로 **복구**되므로(14번) 케어플랜은 아직 끝나지 않은 것으로 판정된다. 사용자가 그 일정을 04번으로 취소하면 그 시점에 `CANCELED`로 발행된다.

## 발행 시점

**아웃박스 패턴**을 사용한다. 아래 커맨드 트랜잭션 안에서 완료 조건을 판정해 아웃박스에 적재하고, 실제 브로커 발행은 커밋 이후 릴레이가 담당한다.

| 트리거 | 마지막 일정의 결말 상태 |
| --- | --- |
| 07번 서비스 수행 결과 등록 | `COMPLETED` / `NO_SHOW` |
| 04번 서비스 일정 취소 | `CANCELED` |
- 이 구조 덕에 "DB는 커밋됐는데 이벤트만 유실" 또는 "이벤트는 나갔는데 DB는 롤백"이 발생하지 않는다.
- 14번(`ProviderMatchFailed` 수신)은 일정을 `CANCELED`로 만들지 않고 `SCHEDULED`로 복구하므로 **발행 지점이 아니다** (이전 문서의 "미구현" 표기는 해소).

### 릴레이 동작

| 항목 | 값 |
| --- | --- |
| 트리거 | `@Scheduled(fixedDelay = ${schedule.outbox.relay.fixed-delay-ms:5000})` |
| 배치 크기 | `PENDING` 최대 100건, `created_at ASC` |
| 재시도 | 실패 시 `retry_count++` 후 `PENDING` 유지 → 3회 도달 시 `FAILED` |

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `schedule.exchange` (Direct) |
| Routing Key | `schedule.completed.key` |
| Queue | `care-plan.schedule-completed.queue` |
| 재시도 | 3회, DLQ 미운용 |
- **재시도/실패 처리 주체는 브로커가 아니라 아웃박스다.** 3회를 넘기면 `FAILED`로 남아 DLQ 대신 운영자가 확인한다.
- Exchange를 Direct로 두는 이유는 Routing Key가 와일드카드 없는 단일 값이기 때문이며, care-plan-service의 `care-plan.exchange`도 Direct라 방식을 맞췄다.
- Queue와 Binding은 **발행 측(schedule-service)에서 함께 선언한다.** Direct Exchange는 바인딩된 큐가 없으면 메시지를 조용히 버리므로, 소비자가 아직 뜨지 않았거나 배포 순서가 뒤바뀐 상황에서도 이벤트가 유실되지 않게 하기 위함이다.

> **재처리(Reprocessing) 기능**: FAILED 이벤트를 재발행/재처리하는 기능은 현재 범위 밖이며 백로그로 관리한다.
>

## 이벤트 페이로드 (Publish)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| serviceResultId | 마지막 일정에 등록된 서비스 수행 결과 ID | UUID | - | O | `550e8400-e29b-41d4-a716-446655440000` |
| status | 마지막 일정의 상태 | ENUM | `COMPLETED` / `NO_SHOW` / `CANCELED` | X | `COMPLETED` |
- `serviceResultId`는 **마지막 일정 자신에게 등록된 결과**를 가리킨다. 07번이 "하나의 일정에는 하나의 결과만" 허용하므로 최대 1건이다.
- `status`가 `CANCELED`면 그 일정은 수행된 적이 없어 결과가 존재하지 않으므로 `serviceResultId`는 `null`이다.
- `CANCELED`가 아닌데 결과가 없다면 처리 순서가 어긋난 비정상 상태다. `status`만으로도 수신 측이 판단할 수 있으므로 발행을 막지 않고 **경고 로그**를 남긴다.
- **`status`가 필요한 이유**: `serviceResultId`만으로는 수신 측이 "정상 수행되어 끝난 케어플랜"인지 "취소로 끝난 케어플랜"인지 구분할 수 없다.

**Example**

```jsx
// 마지막 일정이 정상 수행 완료된 경우
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "serviceResultId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED"
}

// 마지막 일정이 미수행(예약 부도)으로 끝난 경우
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "serviceResultId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "NO_SHOW"
}

// 마지막 일정이 취소되어 끝난 경우
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "serviceResultId": null,
  "status": "CANCELED"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 발행 후 | Care-Plan-Service가 수신 → `serviceResultId`로 결과 데이터 조회(10번 내부 API) → 유효한 데이터인 경우에 한해 `p_care_plans.status`를 `COMPLETED`로 변경 |
