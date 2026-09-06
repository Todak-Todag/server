# 11. [이벤트 발행] CarePlanCompleted - 서비스 수행 완료 알림

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 발행) |
| 사용자 | None |
| 카테고리 | 발행 |
| 테이블명 | `p_service_schedules`, `p_care_plan_service_results` |

## 설명

케어플랜에 속한 서비스 일정이 모두 결말나면 `CarePlanCompleted` 이벤트를 RabbitMQ에 발행한다. "결말"에는 정상 수행(`COMPLETED`)뿐 아니라 미수행(`NO_SHOW`)과 취소(`CANCELED`)도 포함된다.

## 발행 조건

`p_service_schedules`에서 `care_plan_id`로 목록을 조회한 뒤, **아래 두 조건을 모두 만족할 때** 발행한다.

1. 이번 트랜잭션에서 결말이 난 일정이 해당 케어플랜의 **마지막 일정**이다.
2. 해당 케어플랜에 **진행 중(`SCHEDULED` / `RESCHEDULING`) 일정이 하나도 남아있지 않다.**

### 마지막 일정 선정 기준

| 항목 | 값 |
| --- | --- |
| 정렬 | `finished_at DESC`, `created_at DESC` (동일 시각 tie-break) |
| 제외 | `status = CHANGED`, `deleted_at IS NOT NULL` |
| 개수 | 1건 |
- **`date`가 아니라 `finished_at`을 쓰는 이유**: 한 케어플랜은 서비스 종류(`service_preference`)가 여러 개라 같은 `date`에 일정이 여러 건 존재할 수 있어 `date`만으로는 마지막이 1건으로 정해지지 않는다.
- **`CHANGED`를 제외하는 이유**: `CHANGED`는 재매칭 성공으로 새 레코드에 자리를 넘긴 과거 이력이다. 4.1절상 일정은 하루 앞당기는 것도 가능하므로 대체된 레코드가 대체한 레코드보다 `finished_at`이 더 늦을 수 있고, 이 경우 이미 끝난 이력이 영원히 "마지막"을 차지해 이벤트가 발행되지 않는다.

### 두 조건이 모두 필요한 이유

| 시나리오 (A: 앞선 일정, B: 마지막 일정) | 조건 1만 | 조건 2만 | 조건 1 + 2 |
| --- | --- | --- | --- |
| B가 수행 완료되어 결과 등록 | 발행 | 발행 | 발행 |
| B 취소 후, A의 수행 결과가 뒤늦게 등록 | 발행 안 함 | **중복 발행** | 발행 안 함 |
| B가 `RESCHEDULING` 상태 | 발행 안 함 | 발행 안 함 | 발행 안 함 |
| B가 먼저 취소됐고 A는 아직 `SCHEDULED` | **조급하게 발행** | 발행 안 함 | 발행 안 함 |
- 조건 1이 **중복 발행**을 막고, 조건 2가 **조급한 발행**을 막는다.

### 재매칭 케이스

- 재매칭이 완료되어 새 일정이 잡힌 경우 → 새로 생성된 일정이 마지막 일정이 되며, 그 일정이 수행 완료된 시점에 발행된다.
    - ⚠️ **확인 필요**: 이때 기존 레코드가 `CHANGED`로 남는지는 문서상 서술이 엇갈린다. `schedule-service.md` 3장 흐름도는 "재매칭 성공 → `CHANGED`"라 하고, 5.3절은 "`ProviderMatched` 수신 → 새 레코드 생성(`SCHEDULED`)"이라고만 한다. 13번(`ProviderMatched` 수신)이 미구현이라 확정할 수 없어, 발행 로직은 **어느 쪽이든 동작하도록** 만들어 두었다 — `CHANGED`가 남으면 마지막 일정 후보에서 제외되고, 남지 않으면 애초에 후보가 아니다.
- 재매칭에 실패해 취소된 경우 → 더 이상 남은 일정이 없으므로 그 취소 시점에 `status: CANCELED`로 발행된다.

## 발행 시점

**아웃박스 패턴**을 사용하며, 아래 두 트랜잭션의 **커밋 시점**에 발행된다.

| 트리거 | 결말 상태 | 비고 |
| --- | --- | --- |
| 07번 서비스 수행 결과 등록 | `COMPLETED` / `NO_SHOW` | 문서 최초 작성 시부터 명시되어 있던 트리거 |
| 04번 서비스 일정 취소 | `CANCELED` | 취소로 끝나는 케어플랜은 07번 경로로는 영원히 완료 처리되지 않아 추가됨 |
- 커맨드 트랜잭션 안에서는 아웃박스 레코드 적재만 수행하고, 실제 브로커 발행은 커밋 이후 릴레이가 담당한다. 따라서 "DB는 커밋됐는데 이벤트만 유실" 또는 "이벤트는 나갔는데 DB는 롤백"이 발생하지 않는다.
- ⚠️ **미구현**: 14번(`ProviderMatchFailed` 수신 → `CANCELED` 전환) 경로도 같은 발행 지점이 되어야 하나 해당 소비자가 아직 구현되지 않았다. 구현 시 동일한 발행 조건 판단 로직을 재사용한다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `schedule.exchange` (Direct) |
| Routing Key | `schedule.completed.key` |
| Queue | `care-plan.schedule-completed.queue` |
| 재시도 | 3회, DLQ 미운용 |
- **재시도/실패 처리 주체는 브로커가 아니라 아웃박스다.** 발행에 실패하면 아웃박스 레코드의 `retry_count`가 증가하고 다음 폴링 주기에 재시도되며, 3회를 넘기면 `FAILED`로 남아 DLQ 대신 운영자가 확인한다.
- Exchange를 Direct로 두는 이유는 Routing Key가 와일드카드 없는 단일 값이기 때문이며, care-plan-service의 `care-plan.exchange`도 Direct라 방식을 맞췄다.
- Queue와 Binding은 **발행 측(schedule-service)에서 함께 선언한다.** Direct Exchange는 바인딩된 큐가 없으면 메시지를 조용히 버리므로, 소비자가 아직 뜨지 않았거나 배포 순서가 뒤바뀐 상황에서도 이벤트가 유실되지 않게 하기 위함이다.

> **재처리(Reprocessing) 기능**: 트래픽이 증가하면 현재의 "운영자가 FAILED 건을 직접 확인 후 DB로 수동 처리"하는 방식은 유지가 어렵다. FAILED 이벤트를 재발행하거나 재처리할 수 있는 기능을 별도로 추가 개발할 예정이다 (현재 범위 밖, 백로그로 관리).
>

## 이벤트 페이로드 (Publish)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| serviceResultId | 마지막 일정에 등록된 서비스 수행 결과 ID | UUID | - | O | `550e8400-e29b-41d4-a716-446655440000` |
| status | 마지막 일정의 상태 | ENUM | `COMPLETED` / `NO_SHOW` / `CANCELED` | X | `COMPLETED` |
- `serviceResultId`는 **마지막 일정 자신에게 등록된 결과**를 가리킨다. 07번이 "하나의 일정에는 하나의 결과만" 허용하므로 최대 1건이다.
- `status`가 `CANCELED`인 경우 그 일정은 수행된 적이 없어 결과가 존재하지 않으므로 `serviceResultId`는 `null`이다.
- **`status`가 필요한 이유**: `serviceResultId`만으로는 수신 측이 "정상 수행되어 끝난 케어플랜"인지 "취소로 끝난 케어플랜"인지 구분할 수 없다.

**Example**

```jsx
// 마지막 일정이 정상 수행 완료된 경우
{
  "serviceResultId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED"
}

// 마지막 일정이 미수행(예약 부도)으로 끝난 경우
{
  "serviceResultId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "NO_SHOW"
}

// 마지막 일정이 취소되어 끝난 경우 (재매칭 실패 등)
{
  "serviceResultId": null,
  "status": "CANCELED"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 발행 후 | Care-Plan-Service가 수신 → `serviceResultId`로 결과 데이터 조회(10번 Internal API 사용) → 유효한(존재하는) 데이터인 경우에 한해 `p_care_plans.status`를 `COMPLETED`로 변경. 그 외 추가 로직 없음 |

> ⚠️ **확인 필요 (신규, 중요)**: 위 처리 절차는 `serviceResultId`가 항상 존재한다는 전제로 작성되어 있는데, `status: CANCELED`인 경우 `serviceResultId`가 `null`이라 10번 Internal API 검증을 수행할 수 없다. 취소로 끝난 케어플랜을 Care-Plan-Service가 어떤 상태로 전환할지(`COMPLETED`인지, 별도 상태인지, 검증 없이 처리할지)가 정해지지 않았다 — 수신 측 로직이므로 Care-Plan-Service 팀과 확인 필요.
>

> ⚠️ **확인 필요 (신규, 중요)**: **care-plan-service의 현재 구현이 이 문서와 일치하지 않아, 지금 상태로는 이 이벤트를 수신하지 못한다.** 아래 4개 항목 모두 상대 서비스 쪽 수정이 필요하다.
>
>
>
> | 항목 | 이 문서 (schedule-service 구현 기준) | care-plan-service 현재 코드 |
> | --- | --- | --- |
> | Queue | `care-plan.schedule-completed.queue` | `care-plan.completed.queue` |
> | 페이로드 | `serviceResultId`, `status` | `carePlanId`, `patientId`, `completedAt` |
> | 수신 후 처리 | 10번 Internal API로 존재 검증 후 상태 변경 | `completeCarePlan(carePlanId)` 직접 호출 (검증 없음) |
> | Exchange | `schedule.exchange` 바인딩 | 선언 없음 (`care-plan.exchange`만 존재) |
