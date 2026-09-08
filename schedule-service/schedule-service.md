# 🗓️ Schedule-Service 기능 명세

> 담당 서비스: `schedule-service` (Port `19004`)
기준: **실제 구현 코드** (2026-09-08 기준). 문서와 코드가 어긋나던 항목은 코드를 기준으로 정정했다.
API 상세 스펙은 `api/` 하위 개별 문서를 참고한다.
>

---

## 1. 서비스 개요

Schedule-Service는 **확정된 Care Plan에 포함된 서비스의 실제 일정(예약)을 관리**하는 도메인 서비스다. Provider-Service의 매칭 결과를 이벤트로 수신하여 매칭 이력과 일정을 생성하고, 퇴원 예정자의 일정 변경/취소·재매칭 시도, 서비스 제공자의 수행 완료 처리, 수행 결과 등록·조회를 담당한다.

담당 테이블: `p_service_matching_attempts`(서비스 매칭 시도), `p_service_schedules`(서비스 일정), `p_care_plan_service_results`(서비스 수행 결과), `p_schedule_outbox_events`(이벤트 아웃박스)

패키지 구조는 DDD + Layered Architecture를 따르며, 커맨드/쿼리를 서비스·리포지토리 레벨에서 분리한다.

```
schedule/
├── presentation/   controller(api, internal) · request · response
├── application/    facade · service(command, query) · command · query · result · event · port · support
├── domain/         entity · repository(command, query)
└── infrastructure/ persistence(command, query) · client · adapter · messaging
```

- **Facade**: 유스케이스 조합. Internal API(Feign) 호출처럼 **DB 트랜잭션 밖에서 처리해야 하는 작업**을 담당하고, 조회 결과의 존재 여부 판단(403 전환)도 여기서 한다.
- **CommandService**: 순수 트랜잭션 경계. 검증 → 엔티티 상태 전이 → 아웃박스 적재까지를 한 트랜잭션으로 묶는다.
- **Port/Adapter**: 상대 서비스 호출(`CarePlanPort`, `ProviderOfferingPort`)과 브로커 발행(`ProviderReMatchEventPort`, `CarePlanCompletedEventPort`)을 application이 인터페이스로 소유하고, infrastructure가 구현한다.
- 이벤트 관련 컴포넌트(페이로드 record, Serializer, 발행 판단 Appender)는 `application/event`에 둔다.

---

## 2. 도메인 모델

공통 감사 컬럼은 상속으로 처리한다.

| 상위 클래스 | 포함 컬럼 |
| --- | --- |
| `BaseEntity` | `created_at`(Instant, NOT NULL) / `created_by`(UUID, NOT NULL) |
| `BaseUpdatableEntity` | 위 + `updated_at`(Instant, NOT NULL) / `updated_by`(UUID, NOT NULL) |
| `BaseAuditableEntity` | 위 + `deleted_at`(Instant) / `deleted_by`(UUID) — 논리 삭제 |

> 감사 컬럼의 `created_by`/`updated_by`는 `SpringSecurityAuditorAware`가 채운다. 외부 API 요청은 `UserContext.userId`, 내부 API·이벤트 수신처럼 인증 주체가 없는 요청은 `SystemId.SYSTEM_USER_ID`로 기록된다.
>

### `p_service_matching_attempts` — 서비스 매칭 시도 (`BaseAuditableEntity`)

매칭 결과 이벤트를 수신할 때마다 **누적(append)** 되는 이력 테이블이다. 갱신하지 않는다.

| 컬럼명 | 타입 | PK | FK/참조 | Nullable | 설명 |
| --- | --- | --- | --- | --- | --- |
| matching_attempt_id | UUID | O |  | X | 매칭 시도 ID |
| care_plan_id | UUID |  | 논리 참조 → Care Plan | X | 케어플랜 ID |
| region_id | UUID |  | 논리 참조 → p_regions | X | 지역 ID |
| provide_service_id | UUID |  | 논리 참조 → p_provide_services | X | 제공 서비스 ID |
| service_preference_id | UUID |  | 논리 참조 → p_care_plan_service_preferences | X | 서비스 희망 일정 ID |
| service_offering_id | UUID |  | 논리 참조 → p_provide_service_offerings | O | 매칭된 제공자별 서비스 ID (실패 시 `null`) |
| date | LocalDate |  |  | X | 매칭을 시도한 날짜 |
| preferred_time_slot | ENUM |  |  | O | `MORNING` / `AFTERNOON` (성공 이벤트에는 없어 `null`) |
| status | ENUM |  |  | X | `MATCHED` / `FAILED` |
| failure_reason | TEXT |  |  | O | 매칭 실패 사유 |
| matched_at | Instant |  |  | O | 매칭 성공 일시 |
| failed_at | Instant |  |  | O | 매칭 실패 일시 |

### `p_service_schedules` — 서비스 일정 (`BaseAuditableEntity`)

| 컬럼명 | 타입 | PK | FK/참조 | Nullable | 제약조건/기본값 | 설명 |
| --- | --- | --- | --- | --- | --- | --- |
| service_schedule_id | UUID | O |  | X |  | 서비스 일정 ID |
| care_plan_id | UUID |  | 논리 FK → p_care_plans | X |  | 케어플랜 ID |
| service_preference_id | UUID |  | 논리 FK → p_care_plan_service_preferences | X |  | 서비스 희망 일정 ID |
| service_offering_id | UUID |  | 논리 FK → p_provide_service_offerings | X |  | 제공 서비스 ID |
| status | ENUM |  |  | X | SCHEDULED / RESCHEDULING / CHANGED / COMPLETED / CANCELED / NO_SHOW, 생성 시 SCHEDULED 고정 | 일정 상태 |
| date | LocalDate |  |  | X |  | 일정 날짜 |
| started_at | LocalDateTime |  |  | X |  | 시작 일시 |
| finished_at | LocalDateTime |  |  | X |  | 종료 일시 |
| cancel_reason | TEXT |  |  | O |  | 취소 사유 |
| canceled_at | LocalDateTime |  |  | O |  | 취소 일시 |

**생성 시 불변식** (`ServiceSchedule.confirm`)

- `carePlanId` / `servicePreferenceId` / `serviceOfferingId` 필수
- `date`는 **오늘 이후**여야 한다 (당일 일정 생성 불가)
- `startedAt` / `finishedAt` 필수이며, 둘 다 `date`와 같은 날짜여야 하고 `finishedAt > startedAt`
- 위반 시 `INVALID_PARAMETER`(400)

### `p_care_plan_service_results` — 서비스 수행 결과 (`BaseAuditableEntity`)

| 컬럼명 | 타입 | PK | FK/참조 | Nullable | 설명 |
| --- | --- | --- | --- | --- | --- |
| service_result_id | UUID | O |  | X | 서비스 수행 결과 ID |
| service_schedule_id | UUID |  | 논리 FK → p_service_schedules | X | 서비스 일정 ID (일정당 1건) |
| started_at | LocalDateTime |  |  | X | 실제 서비스 시작 일시 |
| finished_at | LocalDateTime |  |  | X | 실제 서비스 종료 일시 |
| note | TEXT |  |  | O | 비고 |

**생성 시 불변식** (`CarePlanServiceResult.record`): `serviceScheduleId`/`startedAt`/`finishedAt` 필수, `finishedAt > startedAt`.

### `p_schedule_outbox_events` — 이벤트 아웃박스 (`BaseUpdatableEntity`)

이벤트를 브로커로 바로 보내지 않고 **커맨드 트랜잭션과 같은 로컬 트랜잭션 안에서 먼저 적재**하기 위한 테이블. 논리 삭제 대상이 아니라 `deleted_at`이 없다.

| 컬럼명 | 타입 | PK | Nullable | 설명 |
| --- | --- | --- | --- | --- |
| outbox_event_id | UUID | O | X | 아웃박스 레코드 ID |
| event_type | String |  | X | `ProviderReMatched` / `CarePlanCompleted` |
| aggregate_id | UUID |  | X | 이벤트가 대변하는 대상 ID (아래 표 참고) |
| payload | TEXT |  | X | 이벤트 페이로드 JSON |
| status | ENUM |  | X | `PENDING` / `SENT` / `FAILED` |
| retry_count | int |  | X | 발행 실패 횟수 (기본 0) |
| last_error_message | TEXT |  | O | 마지막 발행 실패 사유 |
| published_at | Instant |  | O | 실제 발행 성공 일시 |

| event_type | aggregate_id | 사용처 |
| --- | --- | --- |
| `ProviderReMatched` (03번 경로) | `serviceScheduleId` | 변경 요청한 서비스 일정 |
| `ProviderReMatched` (16번 경로) | `matchingAttemptId` | 재시도 중복 접수(409) 판별 키 |
| `CarePlanCompleted` | `carePlanId` | 케어플랜당 1회 발행 보장(멱등) 키 |

---

## 3. 상태(status) 정의

### `p_service_schedules.status`

| 상태 | 의미 | 진입 경로 |
| --- | --- | --- |
| `SCHEDULED` | 예정 (생성 시 기본값) | `ProviderMatched` 수신 / 재매칭 실패로 복구 |
| `RESCHEDULING` | 변경 중 (재매칭 진행 중인 중간 상태) | 03번 서비스 일정 변경 |
| `CHANGED` | 변경 완료 (재매칭 성공으로 새 일정에 자리를 넘긴 과거 이력) | 재매칭 성공 시 `ProviderMatched` 수신 |
| `COMPLETED` | 수행 완료 | 05번 서비스 수행 완료 |
| `NO_SHOW` | 예약 부도(미수행) | 05번 서비스 수행 완료 |
| `CANCELED` | 취소됨 (`cancel_reason`, `canceled_at` 기록) | 04번 서비스 일정 취소 |

**상태 전이 규칙** (엔티티가 스스로 검증)

| 메서드 | 허용 진입 상태 | 결과 | 위반 시 |
| --- | --- | --- | --- |
| `rescheduling()` | `SCHEDULED` | `RESCHEDULING` | 400 `SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING` |
| `markChanged()` | `RESCHEDULING` | `CHANGED` | 409 `SERVICE_SCHEDULE_INVALID_STATUS_FOR_CHANGED` |
| `restoreToScheduled()` | `RESCHEDULING` | `SCHEDULED` | 409 `SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESTORE` |
| `cancel(reason)` | `SCHEDULED`, `RESCHEDULING` | `CANCELED` | 409 `SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL` |
| `complete()` / `markNoShow()` | `SCHEDULED` | `COMPLETED` / `NO_SHOW` | 409 `SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED` |
| `assertResultRegistrable()` | `COMPLETED`, `NO_SHOW` | (검증만) | 409 `SERVICE_RESULTS_INVALID_SCHEDULE_STATUS` |

### `p_service_matching_attempts.status`

`MATCHED` / `FAILED` 두 가지뿐이며, 이력 레코드이므로 생성 이후 전이하지 않는다.

### `p_schedule_outbox_events.status`

| 상태 | 의미 |
| --- | --- |
| `PENDING` | 발행 대기 (신규 적재 또는 발행 실패 후 재시도 대기) — 릴레이가 폴링하는 유일한 상태 |
| `SENT` | 브로커 발행 성공 (최종) |
| `FAILED` | 재시도 상한(`MAX_RETRY_COUNT = 3`) 초과 (최종, DLQ 역할) |

---

## 4. 핵심 비즈니스 규칙

### 4.1 서비스 일정 변경(연기) — 03번

- 요청 주체는 **퇴원 예정자**(`@PreAuthorize("hasRole('PATIENT')")`).
- 일정 **시작 24시간 전까지만** 변경 가능하다 (`ServiceScheduleValidator.validateDeadline`).
- 본인에게 배정된 일정만 변경 가능하며(`patientId` 대조), `status`가 `SCHEDULED`인 경우에만 가능하다.
- 기존 일정 날짜 `D` 기준 **하루 앞당기기(D-1)** 또는 **하루 미루기(D+1)** 만 가능하다.
  - 하루 앞당기기: 변경일이 오늘(당일)인 경우 불가
  - 하루 미루기: Care Plan의 일정 범위(`finishDate`)를 초과할 수 없음
- 소유권·범위 검증에 필요한 `carePlanId`/`finishDate`/`patientId`는 `servicePreferenceId`를 기준으로 **care-plan-service Internal API(Feign)** 를 호출해 조회한다 (5.5절).
- 검증 순서는 **소유권(403) → 마감 시각 → 날짜 규칙 → 상태 전이**다. 비소유자가 400/409 응답으로 대상의 존재나 상태를 알아내지 못하게 하기 위함이다.
- 변경 요청이 접수되면 `status`가 `RESCHEDULING`으로 바뀌고, **같은 트랜잭션에서** `ProviderReMatched` 이벤트가 아웃박스에 적재된다.
  - 페이로드의 `regionId`/`provideServiceId`는 해당 `servicePreferenceId`의 **가장 최근 `MATCHED` 매칭 시도 레코드**에서 읽어온다. 없으면 404 `SERVICE_MATCHING_ATTEMPT_NOT_FOUND`.
  - 이 경로에서는 시간대를 선택하지 않으므로 `preferredTimeSlot`은 `null`이다.
- 재매칭 **성공** 시 기존 일정은 `CHANGED`가 되고 **새 일정 레코드**가 `SCHEDULED`로 생성된다.
- 재매칭 **실패** 시 기존 일정은 `SCHEDULED`로 복구된다.

### 4.2 서비스 일정 취소 — 04번

- 요청 주체는 퇴원 예정자다.
- **일정 시작 24시간 전까지만** 취소 가능하다. 위반 시 **409** `SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED`.
- `SCHEDULED`뿐 아니라 **`RESCHEDULING`(변경 중) 상태에서도 취소할 수 있다.**
- 취소 시 `status`는 `CANCELED`로 변경되고 `cancelReason`(필수), `canceledAt`이 기록된다.
- 이미 완료/부도/변경완료/취소된 일정은 `409 CONFLICT`.
- 마지막 일정이 취소되면 그 시점에 `CarePlanCompleted` 발행 조건을 판정한다 (5.2절).

### 4.3 서비스 수행 완료 처리 — 05번

- 요청 주체는 서비스 제공자다(`hasRole('SERVICE_PROVIDER')`).
- URL: `PATCH /api/v1/service-schedules/{serviceScheduleId}/result`
- `status`가 `SCHEDULED`일 때만 변경 가능하며, 결과 값은 `COMPLETED` 또는 `NO_SHOW`다.
- 요청 시점이 일정의 `finishedAt` **이후**여야 한다. 위반 시 **400** `SERVICE_SCHEDULE_STATUS_UPDATE_TOO_EARLY`.
- "본인이 배정된 서비스 제공자인지" 검증은 **provider-service Internal API**(`GET /internal/v1/service-offerings/{serviceOfferingId}`)로 `providerId`를 조회해 요청자 `userId`와 비교한다 (5.6절).
- 이미 취소/변경 중/변경완료/완료/부도인 일정은 `409 CONFLICT`.

### 4.4 서비스 수행 결과 등록 — 07번

- 요청 주체는 서비스 제공자이며, 4.3과 같은 방식으로 배정 여부를 검증한다.
- 대상 일정의 `status`가 **`COMPLETED` 또는 `NO_SHOW`** 여야 한다(409). 즉 **05번이 반드시 선행**된다.
- 하나의 일정에는 결과 1건만 등록할 수 있다. 재등록 시 409 `SERVICE_RESULTS_ALREADY_EXISTS`.
- 실제 시작/종료 일시와 비고(`note`)를 기록하며 `finishedAt > startedAt`이어야 한다.
- 등록 후 `CarePlanCompleted` 발행 조건을 판정한다 (5.2절).

### 4.5 재매칭 시도 — 16번

- 요청 주체는 퇴원 예정자이며, 대상 매칭 시도의 `status`가 `FAILED`일 때만 재시도할 수 있다(409).
- **동기적으로는 아무 레코드도 쓰지 않고** `ProviderReMatched`만 아웃박스에 적재하므로 `202 ACCEPTED`로 응답한다. 새 매칭 시도 이력은 결과 이벤트를 수신할 때 생성된다.
- 같은 `matchingAttemptId`로 이미 아웃박스에 `ProviderReMatched`가 적재됐다면 409 `MATCHING_ATTEMPT_RETRY_ALREADY_REQUESTED`. (동기 상태 변경이 없어 `status`로는 "재시도 중"을 표현할 수 없기 때문에 아웃박스를 판별 키로 쓴다.)
- 희망 날짜는 Care Plan의 `startDate`~`finishDate` 범위 안이어야 한다(400). Internal API 응답에 `startDate`가 없어 **`startDate = finishDate - 29일`(30일 고정 기간)** 로 역산한다.

### 4.6 매칭 실패 내역 조회 — 15번

- 요청 주체는 퇴원 예정자다.
- Care Plan이 `CONFIRMED`가 아니면(재매칭이 의미 없으므로) 빈 페이지를 반환한다.
- `status` 미지정 시 기본값은 `FAILED`이며, **`FAILED` 조회일 때만** "해당 `servicePreferenceId`로 생성된 일정이 아직 하나도 없는" 조건을 함께 적용한다. 매칭 시도는 누적되므로 나중에 성공해 해소된 과거 실패를 이 조건으로 걸러낸다.

### 4.7 조회 권한 공통 규칙

- 퇴원 예정자는 **본인이 받은** 일정/결과만, 서비스 제공자는 **본인이 제공한** 일정/결과만 조회할 수 있다.
- **단건 조회**는 점(point) 검증 — 대상 레코드의 `servicePreferenceId`/`serviceOfferingId`로 Internal API를 호출해 `patientId`/`providerId`를 요청자와 대조한다.
- **목록 조회**는 배치 검증 — 요청자가 소유한 ID 목록을 Internal API로 1회 조회해 `IN` 조건으로 필터링한다 (5.7절). 소유 ID가 0건이면 DB 조회 없이 빈 페이지를 반환한다.
- 대상이 없거나 남의 것이면 모두 **403 `AUTH_FORBIDDEN`** 으로 통일한다 (404로 리소스 존재 여부를 노출하지 않기 위함).

### 4.8 페이지네이션 공통 규칙

`PageableFactory`가 보정한다.

- `page`: `null`이거나 음수면 `0`
- `size`: `10`/`30`/`50`만 허용, 그 외에는 `10`
- `sort`: 정렬 필드는 **`createdAt` 고정**이고 방향만 파싱한다. 기본값 `createdAt,DESC`
- 응답 `pageInfo.paginationType`은 `OFFSET`

---

## 5. 서비스 간 연동 (이벤트 & 내부 API)

메시징 브로커: **RabbitMQ** (Direct Exchange)

### 5.1 전체 흐름

```
Care-Plan-Service (CarePlanConfirmed 발행)
        ↓
Provider-Service (매칭 가능 Provider 판단 시 06번 내부 API로 Schedule-Service 호출, 5.4절)
        ↓
[성공] Provider-Service (ProviderMatched 발행) ──▶ Schedule-Service 수신
   ──▶ p_service_matching_attempts에 MATCHED 이력 추가
   ──▶ (재매칭이면) 기존 RESCHEDULING 일정을 CHANGED로 변경
   ──▶ p_service_schedules에 새 일정 생성 (SCHEDULED)

[실패] Provider-Service (ProviderMatchFailed 발행) ──▶ Schedule-Service 수신
   ──▶ p_service_matching_attempts에 FAILED 이력 추가
   ──▶ (재매칭이면) 기존 RESCHEDULING 일정을 SCHEDULED로 복구
   ──▶ (초기 매칭이면) p_service_schedules는 건드리지 않음 (아직 일정 레코드가 없음)

Schedule-Service 재매칭 트리거 (둘 다 아웃박스 적재)
   ① 16번 재매칭 시도 API  — 초기 매칭 실패 건 (사용자가 15번으로 확인 후 호출)
   ② 03번 서비스 일정 변경 API — 이미 확정된 일정의 날짜 변경
   → 릴레이가 ProviderReMatched 발행 ──▶ Provider-Service 재매칭
   → 결과는 초기 매칭과 동일하게 ProviderMatched / ProviderMatchFailed로 수신

Schedule-Service (케어플랜의 모든 일정이 결말남) ──▶ CarePlanCompleted 아웃박스 적재 → 발행
   ──▶ Care-Plan-Service 수신 → 10번 내부 API로 serviceResultId 존재 검증 → p_care_plans 상태 변경
```

### 5.2 발행(Publish) 이벤트

| 이벤트명 | 발행 트리거 | Exchange / Routing Key / Queue | 상세 문서 |
| --- | --- | --- | --- |
| `CarePlanCompleted` | 07번 수행 결과 등록, 04번 일정 취소 — 각 트랜잭션 끝에서 완료 조건을 판정해 적재 | `schedule.exchange` / `schedule.completed.key` / `care-plan.schedule-completed.queue` | `11_...md` |
| `ProviderReMatched` | 03번 일정 변경, 16번 재매칭 시도 | `schedule.exchange` / `schedule.rematched.key` / `provider.schedule-rematched.queue` | `12_...md` |

**`CarePlanCompleted` 발행 조건** (`CarePlanCompletionEventAppender`) — 두 조건을 모두 만족해야 적재한다.

1. 해당 케어플랜에 **아직 끝나지 않은 일정이 하나도 없다.**
  - "끝나지 않음" = `status`가 `SCHEDULED`/`RESCHEDULING`이거나, `COMPLETED`/`NO_SHOW`인데 수행 결과가 아직 없는 경우
2. 해당 케어플랜으로 `CarePlanCompleted`가 **아직 적재된 적이 없다** (`aggregate_id = carePlanId` 기준 멱등).

페이로드 기준이 되는 "마지막 일정"은 트리거가 된 일정이 아니라 `finished_at DESC, created_at DESC` 정렬로 다시 조회하며, `CHANGED`와 논리 삭제 건은 제외한다.

**페이로드**: `carePlanId`, `serviceResultId`(마지막 일정의 결과, `CANCELED`면 `null`), `status`(마지막 일정의 상태)

### 5.3 수신(Consume) 이벤트

| 이벤트명 | 처리 내용 | Exchange / Routing Key / Queue | 상세 문서 |
| --- | --- | --- | --- |
| `ProviderMatched` | `MATCHED` 이력 추가 → (재매칭이면) 기존 `RESCHEDULING` 일정 `CHANGED` → 새 일정 생성(`SCHEDULED`) | `provider.exchange` / `provider.matched.key` / `schedule.provider-matched.queue` | `13_...md` |
| `ProviderMatchFailed` | `FAILED` 이력 추가 → (재매칭이면) 기존 `RESCHEDULING` 일정 `SCHEDULED` 복구 | `provider.exchange` / `provider.match-failed.key` / `schedule.provider-match-failed.queue` | `14_...md` |
- **신규/재매칭 구분**: 같은 `servicePreferenceId`로 `RESCHEDULING` 상태인 일정이 있는지로 판단한다. 0건이면 신규 매칭, 1건이면 재매칭, 2건 이상이면 데이터 이상으로 409 `SERVICE_SCHEDULE_MULTIPLE_RESCHEDULING`.
- **중복 수신 방어**: 페이로드에 이벤트 ID가 없어, 같은 매칭 결과를 특정하는 값 조합을 대체 키로 쓴다.
  - 성공: `servicePreferenceId` + `serviceOfferingId` + `date` + `matchedAt`
  - 실패: `servicePreferenceId` + `date` + `failedAt`
- 페이로드에 `finishedAt`이 없는 문제는 **MVP 단계에서 서비스 소요 시간을 1시간으로 고정**(`ServiceMatchingCommandService.DEFAULT_SERVICE_DURATION`)해 `finishedAt = startedAt + 1h`로 계산하는 것으로 처리했다. 서비스별 소요 시간이 달라지면 페이로드 확장이 필요하다.
- 리스너는 `BusinessException`을 잡아 로그만 남기고 메시지를 버린다 (재시도해도 같은 결과이기 때문). 그 외 예외는 리스너 컨테이너의 재시도 설정을 따른다.

### 5.4 내부 API — Provider-Service → Schedule-Service (06번, 수신 방향)

- **엔드포인트**: `GET /internal/v1/service-schedules?serviceOfferingIds={...}&startDate={...}`
- **인증**: `X-Internal-Api-Key`. `InternalResponseInterceptor`가 `/internal/v1/**` 전체에 대해 검증하며, 불일치 시 401 `UNAUTHORIZED_INTERNAL_REQUEST`. Controller는 헤더를 직접 다루지 않는다.
- **설계 근거**: 서비스 간 관계가 논리 FK뿐이라 DB JOIN이 불가능하다. Provider-Service의 `p_provide_works`는 제공 가능 요일/시간대만 알고 실제 예약 현황은 `p_service_schedules`에만 있다.
- **응답 원칙**: 가능/불가능을 판단하지 않고 기존 일정 목록을 그대로 반환한다. 시간대 겹침 판단은 Provider-Service의 책임이다.
- **조회 대상 상태**: `SCHEDULED`, `RESCHEDULING`만. 나머지는 향후 일정과 충돌하지 않아 제외한다.
- **Batch 조회**: `startDate`부터 **30일**(`BATCH_QUERY_DAYS = 30`, `startDate ~ startDate+29`)을 한 번에 반환한다.

### 5.5 내부 API — Schedule-Service → Care-Plan-Service (호출 방향)

`CarePlanPort` / `CarePlanAdapter` / `CarePlanClient`(Feign, `infrastructure/client/care_plan`). 인증 헤더는 `FeignConfig`의 `RequestInterceptor`가 모든 요청에 자동으로 붙인다.

| 용도 | Method/URL | 응답 필드 | 사용처 |
| --- | --- | --- | --- |
| Care Plan 범위/소유자 조회 | `GET /internal/v1/service-preferences/{servicePreferenceId}/care-plan` | `carePlanId`, `finishDate`, `patientId` | 02·03·04·09·16번의 소유권/날짜 범위 검증 |
| 소유 희망 일정 ID 목록 | `GET /internal/v1/service-preferences?patientId={patientId}` | `content`: UUID 배열 | 01·08·15번 목록 필터 |
| Care Plan 상태 조회 | `GET /internal/v1/care-plans/{patientId}` | `carePlanId`, `patientId`, `status` | 15번의 `CONFIRMED` 선검사 |
- 16번이 필요로 하던 `startDate`는 **응답에 없다.** Care Plan 기간이 30일 고정이라는 점을 이용해 `CarePlanRange.startDate() = finishDate - 29일`로 역산한다(`CARE_PLAN_PERIOD_DAYS = 30`).
- `status`는 `String`으로 받아 `CarePlanStatus`(`UNDER_REVIEW`/`CONFIRMED`/`IN_PROGRESS`/`COMPLETED`)로 변환하며, 상대 서비스가 ENUM을 확장해도 터지지 않도록 알 수 없는 값은 `null`(= `CONFIRMED` 아님)로 취급한다.
- 호출은 모두 **Facade에서 DB 트랜잭션 밖으로** 수행한다.

### 5.6 내부 API — Schedule-Service → Provider-Service (호출 방향)

`ProviderOfferingPort` / `ProviderOfferingAdapter` / `ProviderServiceOfferingClient`.

| 용도 | Method/URL | 응답 필드 | 사용처 |
| --- | --- | --- | --- |
| 배정 제공자 조회 | `GET /internal/v1/service-offerings/{serviceOfferingId}` | `providerId` (응답 `data`의 최상위 필드) | 02·05·07·09번의 제공자 본인 검증 |
| 소유 제공 서비스 ID 목록 | `GET /internal/v1/service-offerings?providerId={providerId}` | `content`: UUID 배열 | 01·08번 목록 필터 |

### 5.7 목록 조회용 ID 목록 조회 전략

- 5.5/5.6의 단건 조회를 목록에 그대로 쓰면 레코드 수만큼 호출이 발생하고, 필터링이 애플리케이션 레벨로 밀려 DB 페이지네이션이 불가능해진다.
- 따라서 목록 조회는 요청자 소유 ID 전체를 **요청당 1회** 조회한 뒤 `WHERE service_preference_id IN (:ids)` / `WHERE service_offering_id IN (:ids)`로 필터링하고, `status`/`date` 필터와 페이지네이션을 같은 쿼리에 적용한다.
- 두 API 모두 공통 `ApiResponse` 포맷을 따르며 `data.content`는 **UUID 문자열 배열**이다(객체 배열 아님).

```json
{
  "success": true,
  "code": 200,
  "message": "제공 서비스 목록 조회 성공",
  "data": {
    "content": ["9b2f1c3a-1111-4a2b-8c3d-abcdef123456", "5d8a7e2b-2222-4c3d-9e4f-fedcba654321"]
  }
}
```

- `p_care_plan_service_results`에는 소유권 컬럼이 없으므로, 08번은 `p_service_schedules`와 `service_schedule_id`로 조인한 뒤 필터를 적용한다. 09번(단건)은 결과의 `serviceScheduleId`로 일정을 먼저 조회해 `servicePreferenceId`/`serviceOfferingId`를 얻은 뒤 Internal API를 호출한다. 두 테이블 모두 schedule-service 소유라 DB 조인에 문제가 없다.

### 5.8 내부 API — Care-Plan-Service → Schedule-Service (10번, 수신 방향)

- **엔드포인트**: `GET /internal/v1/service-results/{serviceResultId}`
- **용도**: `CarePlanCompleted` 페이로드의 `serviceResultId` 존재 검증
- **응답 필드**: `serviceResultId` **하나만** (존재 검증이 목적이므로 최소 필드만 노출. `carePlanId`는 `CarePlanCompleted` 페이로드로 이미 전달되고, 나머지 필드는 수신 측이 쓰지 않는다)
- **필터**: `deleted_at IS NULL`. 없으면 404 `SERVICE_RESULTS_NOT_FOUND`
- **인증**: 5.4절과 동일 (Interceptor)

### 5.9 아웃박스 릴레이

| 항목 | 값 |
| --- | --- |
| 트리거 | `ScheduleOutboxRelayScheduler` — `@Scheduled(fixedDelay = ${schedule.outbox.relay.fixed-delay-ms:5000})` |
| 활성화 스위치 | `schedule.outbox.relay.enabled` (미설정 시 활성) |
| 배치 크기 | 한 폴링당 `PENDING` 최대 100건, `created_at ASC` |
| 재시도 | 실패 시 `retry_count++` 후 `PENDING` 유지 → 3회 도달 시 `FAILED` |
- 커맨드 트랜잭션은 적재만 하고, 실제 브로커 발행은 커밋 이후 릴레이가 담당한다. "DB는 커밋됐는데 이벤트만 유실" 또는 "이벤트는 나갔는데 DB는 롤백"이 발생하지 않는다.
- 이벤트 1건 처리 실패가 배치 전체를 중단시키지 않는다. 알 수 없는 `event_type`은 정상 처리할 방법이 없으므로 실패로 기록한다.
- Queue와 Binding은 **발행 측(schedule-service)에서도 선언한다.** Direct Exchange는 바인딩된 큐가 없으면 메시지를 조용히 버리기 때문이다.
- `FAILED` 건의 재발행/재처리 기능은 현재 범위 밖(백로그)이며, 운영자가 직접 확인한다.

---

## 6. API 목록

| # | 기능 | Method | URL | 권한 |
| --- | --- | --- | --- | --- |
| 01 | 서비스 일정 목록 조회 | GET | `/api/v1/service-schedules` | PATIENT, SERVICE_PROVIDER |
| 02 | 서비스 일정 상세 조회 | GET | `/api/v1/service-schedules/{serviceScheduleId}` | PATIENT, SERVICE_PROVIDER |
| 03 | 서비스 일정 변경 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/status` | PATIENT |
| 04 | 서비스 일정 취소 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/cancel` | PATIENT |
| 05 | 서비스 수행 완료 상태 변경 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/result` | SERVICE_PROVIDER |
| 06 | \[내부 API\] 서비스 제공자 일정 조회 | GET | `/internal/v1/service-schedules` | Internal Key |
| 07 | 서비스 수행 결과 등록 | POST | `/api/v1/service-results/{serviceScheduleId}` | SERVICE_PROVIDER |
| 08 | 서비스 수행 결과 목록 조회 | GET | `/api/v1/service-results` | PATIENT, SERVICE_PROVIDER |
| 09 | 서비스 수행 결과 상세 조회 | GET | `/api/v1/service-results/{serviceResultId}` | PATIENT, SERVICE_PROVIDER |
| 10 | \[내부 API\] 서비스 수행 결과 조회 | GET | `/internal/v1/service-results/{serviceResultId}` | Internal Key |
| 11 | \[이벤트 발행\] CarePlanCompleted | - | RabbitMQ Publish | - |
| 12 | \[이벤트 발행\] ProviderRematched | - | RabbitMQ Publish | - |
| 13 | \[이벤트 수신\] ProviderMatched | - | RabbitMQ Consume | - |
| 14 | \[이벤트 수신\] ProviderMatchFailed | - | RabbitMQ Consume | - |
| 15 | 매칭 실패 내역 조회 | GET | `/api/v1/matching-attempts` | PATIENT |
| 16 | 재매칭 시도 | POST | `/api/v1/matching-attempts/{matchingAttemptId}/retry` | PATIENT |

> Schedule-Service가 **호출하는** care-plan-service / provider-service Internal API(5.5·5.6절)는 상대 서비스 소유 명세이므로 이 목록에 번호를 두지 않는다.
>

---

## 7. 인증/인가

- **인증**: API Gateway에서 JWT를 검증하고 `X-User-Id`, `X-User-Role` 헤더로 전달한다. `HeaderAuthenticationFilter`가 이를 `UserContext`로 변환해 `SecurityContext`에 담고, Controller는 `@AuthenticationPrincipal UserContext user`로만 주입받는다(모든 외부 API 공통). 헤더가 없거나 값이 잘못되면 인증 주체 없이 진행된다.
- **인가**: 메서드 레벨 `@PreAuthorize`(`ROLE_` 접두사 자동 부여)로 역할을 제한하고, 소유권 검증은 각 유스케이스에서 Internal API 조회 결과와 대조해 수행한다.
- **내부 API**(`/internal/v1/**`): API Gateway를 거치지 않으며 `X-Internal-Api-Key`를 `InternalResponseInterceptor`가 검증한다. Schedule-Service가 다른 서비스를 호출할 때는 `FeignConfig`가 같은 헤더를 자동으로 붙인다. 키는 `internal.key` 설정값이며 미설정 시 애플리케이션이 기동되지 않는다.
- **에러 응답 포맷**: `success`(false) / `code`(ErrorCode 이름) / `message` / `details.reason` / `timestamp`(Instant). `GlobalExceptionHandler`가 `BusinessException`, 검증 예외(400), `AccessDeniedException`(403), 그 외(500)를 처리한다.

> ⚠️ **임시 설정 (코드 내 TODO)**: `SecurityConfig`가 개발 테스트를 위해 `/api/v1/service-schedules/**`, `/api/v1/service-results/**`, `/internal/v1/**` 등을 `permitAll`로 열어두고 있다. **2026-09-09 이후 변경 예정**이며, 현재는 `@PreAuthorize`만 실질적인 인가 장치로 동작한다.
>
