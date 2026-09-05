# 🗓️ Care-Plan-Service 기능 명세

> 담당 서비스: `care-plan-service` (Port `19005`)
> 담당자: 한솔
> 출처: Notion `Table 명세서`, 실제 코드(`src/main/java/com/spring/careplanservice`), 팀 대화 확인 내용
> API 상세 스펙은 `api/` 하위 개별 문서를 참고한다 (현재 `01_서비스 희망 일정 수정.md`부터 순차 작성 중).

---

## 1. 서비스 개요

Care-Plan-Service는 **퇴원 예정자의 Care Plan(돌봄 계획)과 그에 속한 서비스별 희망 일정을 관리**하는 도메인 서비스다. 퇴원(discharge) 정보를 기반으로 Care Plan을
생성하고, 병원 담당자가 권고한 서비스를 퇴원 예정자가 선택·희망 일정을 입력하도록 하며, Care Plan이 `CONFIRMED` 상태로 전이되면 `CarePlanConfirmed` 이벤트를 발행해
Provider-Service의 서비스 제공자 매칭을 트리거한다. 이후 매칭된 서비스가 모두 수행 완료되면 Schedule-Service로부터 `CarePlanCompleted` 이벤트를 수신하여 Care Plan을
종료 처리한다.

담당 테이블: `p_care_plans`(케어플랜), `p_care_plan_services`(케어플랜에 포함된 서비스), `p_care_plan_service_preferences`(서비스별 희망 일정)

---

## 2. 도메인 모델

> ⚠️ 아래 3개 테이블은 Notion 원본 `Table 명세서`가 표 셀 밀림(줄바꿈이 새 행으로 잘못 파싱됨) 상태로 전달되어, 실제 엔티티 코드(`domain/entity/*.java`)를 기준으로 컬럼
> 설명을 재구성했다. 특히 `p_care_plan_service_preferences.plan_service_id`의 참조 대상과 `p_care_plan_services`의 `care_plan_id`/
`provide_service_id` 설명 문구가 원본에서 서로 뒤바뀌어 있었다 (변경 이력 참고).

### `p_care_plans` — 케어플랜

| 컬럼명          | 타입         | PK | FK/참조                               | Nullable | 제약조건/기본값                                                                           | 설명                          |
|--------------|------------|----|-------------------------------------|----------|------------------------------------------------------------------------------------|-----------------------------|
| care_plan_id | UUID       | O  | `@GeneratedValue(UUID)`             | X        |                                                                                    | 케어플랜 ID                     |
| patient_id   | UUID       |    | 논리 FK → `p_users.user_id`           | X        |                                                                                    | 환자 ID                       |
| discharge_id | UUID       |    | 논리 FK → `p_discharges.discharge_id` | X        |                                                                                    | 퇴원 정보 ID                    |
| status       | ENUM       |    |                                     | X        | `UNDER_REVIEW` / `CONFIRMED` / `IN_PROGRESS` / `COMPLETED`, DEFAULT `UNDER_REVIEW` | 케어플랜 진행 상태 (검토, 확정, 진행, 종료) |
| note         | TEXT       |    |                                     | O        |                                                                                    | 병원 담당자의 코멘트                 |
| start_date   | DATE       |    |                                     | X        |                                                                                    | 시작 일시                       |
| finish_date  | DATE       |    |                                     | X        |                                                                                    | 종료 일시                       |
| created_at   | TIMESTAMPZ |    |                                     | X        |                                                                                    | 생성 일시                       |
| created_by   | UUID       |    |                                     | X        |                                                                                    | 생성자 사용자 ID                  |
| updated_at   | TIMESTAMPZ |    |                                     | X        |                                                                                    | 수정 일시                       |
| updated_by   | UUID       |    |                                     | X        |                                                                                    | 수정자 사용자 ID                  |
| deleted_at   | TIMESTAMPZ |    |                                     | O        |                                                                                    | 논리 삭제 일시                    |
| deleted_by   | UUID       |    |                                     | O        |                                                                                    | 논리 삭제 처리자 ID                |

Entity: `CarePlan extends BaseAuditEntity` (created + updated + deleted 감사 컬럼 모두 보유)

### `p_care_plan_services` — 케어플랜에 포함된 서비스

| 컬럼명                | 타입         | PK | FK/참조                                           | Nullable | 제약조건/기본값 | 설명             |
|--------------------|------------|----|-------------------------------------------------|----------|----------|----------------|
| plan_service_id    | UUID       | O  | `@GeneratedValue(UUID)`                         | X        |          | 케어플랜 내의 서비스 ID |
| care_plan_id       | UUID       |    | 논리 FK → `p_care_plans.care_plan_id`             | X        |          | 케어플랜 ID        |
| provide_service_id | UUID       |    | 논리 FK → `p_provide_services.provide_service_id` | X        |          | 제공 서비스 ID      |
| created_at         | TIMESTAMPZ |    |                                                 | X        |          | 생성 일시          |
| created_by         | UUID       |    |                                                 | X        |          | 생성자 사용자 ID     |
| deleted_at         | TIMESTAMPZ |    |                                                 | O        |          | 논리 삭제 일시       |
| deleted_by         | UUID       |    |                                                 | O        |          | 논리 삭제 처리자 ID   |

Entity: `CarePlanService extends BaseCreateDeleteEntity` (updated 감사 컬럼 없음 — 생성/논리삭제만 관리, 수정 불가 리소스)

### `p_care_plan_service_preferences` — 퇴원 예정자의 서비스별 희망 일정

| 컬럼명                   | 타입         | PK | FK/참조                                          | Nullable | 제약조건/기본값                | 설명           |
|-----------------------|------------|----|------------------------------------------------|----------|-------------------------|--------------|
| service_preference_id | UUID       | O  | `@GeneratedValue(UUID)`                        | X        |                         | 서비스 희망 일정 ID |
| plan_service_id       | UUID       |    | 논리 FK → `p_care_plan_services.plan_service_id` | X        |                         | 케어플랜 서비스 ID  |
| preferred_time_slot   | ENUM       |    |                                                | X        | `MORNING` / `AFTERNOON` | 희망 시간대       |
| preferred_date        | DATE       |    |                                                | X        |                         | 희망 날짜        |
| created_at            | TIMESTAMPZ |    |                                                | X        |                         | 생성 일시        |
| created_by            | UUID       |    |                                                | X        |                         | 생성자 사용자 ID   |
| updated_at            | TIMESTAMPZ |    |                                                | X        |                         | 수정 일시        |
| updated_by            | UUID       |    |                                                | X        |                         | 수정자 사용자 ID   |
| deleted_at            | TIMESTAMPZ |    |                                                | O        |                         | 논리 삭제 일시     |
| deleted_by            | UUID       |    |                                                | O        |                         | 논리 삭제 처리자 ID |

Entity: `CarePlanServicePreference extends BaseAuditEntity` (updated 컬럼 보유 — `01_서비스 희망 일정 수정.md` API로 수정 가능한 리소스이기 때문)

---

## 3. 상태(status) 정의

`CarePlanStatus`는 `p_care_plans.status`에만 존재하며, `p_care_plan_services`/`p_care_plan_service_preferences`는 자체 상태값이 없다 — 두
테이블의 유효성은 상위 `CarePlan.status`를 기준으로 판단한다 (예: 희망 일정 등록/수정은 `UNDER_REVIEW`일 때만 가능).

| 상태             | 의미                                                       |
|----------------|----------------------------------------------------------|
| `UNDER_REVIEW` | 검토 중 (기본값) — 병원 담당자 권고 확인, 서비스 선택, 희망 일정 입력/수정 단계        |
| `CONFIRMED`    | 확정 — `CarePlanConfirmed` 이벤트 발행, Provider-Service 매칭 트리거 |
| `IN_PROGRESS`  | 진행 중                                                     |
| `COMPLETED`    | 종료 — Schedule-Service의 `CarePlanCompleted` 이벤트 수신으로 전이   |

### 상태 전이 규칙 (`CarePlan.canTransitionTo`, ✅ 확정 · 코드 기준)

| 현재 상태          | 허용되는 다음 상태         |
|----------------|--------------------|
| `UNDER_REVIEW` | `CONFIRMED`만 허용    |
| `CONFIRMED`    | `IN_PROGRESS`만 허용  |
| `IN_PROGRESS`  | 없음 (API를 통한 전이 불가) |
| `COMPLETED`    | 없음                 |

- 허용되지 않는 전이를 요청하면 `CARE_PLAN_INVALID_STATUS_TRANSITION`(400) 예외가 발생한다.
- `IN_PROGRESS → COMPLETED` 전이는 API(`PATCH /api/v1/care-plans/{carePlanId}/status`)로는 불가능하며, Schedule-Service가 발행하는
  `CarePlanCompleted` 이벤트를 수신했을 때만 `CarePlan.complete()`가 내부적으로 전이시킨다.

> ⚠️ 확인 필요 (코드 `TODO` 주석, `CarePlan.java:81`): `complete()`는 "이벤트 계약 이후 수정"이라는 TODO가 남아 있으며, 현재는 `status != IN_PROGRESS`
> 이면 예외 없이 조용히 무시(return)한다. 이벤트 계약이 확정되면 이 처리 방식(무시 vs 예외)을 재검토해야 한다.

---

## 4. 핵심 비즈니스 규칙

### 4.1 Care Plan 생성

- 요청 주체는 병원 담당자(`HOSPITAL_STAFF`) 또는 퇴원 예정자(`PATIENT`)다.
- 요청자가 `PATIENT`인 경우, 요청자 본인(`userId`)과 대상 환자(`patientId`)가 일치해야 한다 (`HOSPITAL_STAFF`는 이 제약이 없음).
- `dischargeId` 기준으로 이미 생성된 Care Plan이 있으면 `CARE_PLAN_ALREADY_EXISTS`(409) 예외가 발생한다 (퇴원 건당 Care Plan 1개).
- Discharge-Service Internal API(`GET /internal/v1/discharges/{dischargeId}`)로 조회한 퇴원 건의 `patientId`와 요청한 `patientId`가
  일치해야 한다 (`CARE_PLAN_PATIENT_MISMATCH`, 400).
- 조회한 퇴원 건의 `actualDate`(실제 퇴원일)가 없으면(`null`) 아직 퇴원이 완료되지 않은 것으로 보고 `DISCHARGE_NOT_COMPLETED`(409) 예외를 발생시킨다.
- `startDate`는 `actualDate + 1일`, `finishDate`는 `startDate + 29일`로 **고정 30일** 계산되어 저장된다 (`CARE_PLAN_PERIOD_DAYS = 30`).
- 생성 시점에 `provideServiceIds`가 함께 전달되면 각 ID에 대해 `p_care_plan_services` 레코드를 함께 생성한다 (중복 ID는 `distinct()`로 제거).
- 생성 직후 상태는 항상 `UNDER_REVIEW`다.

### 4.2 Care Plan 서비스 선택 (`p_care_plan_services` 등록)

- 요청 주체는 퇴원 예정자(`PATIENT`)다.
- 요청자가 해당 Care Plan의 소유자(`patientId`)여야 한다 (`CarePlanOwnerValidator`, 불일치 시 `AUTH_FORBIDDEN` 403).
- 동일 Care Plan에 동일 `provideServiceId`가 동일 요청자(`createdBy`)로 이미 선택되어 있으면 `CARE_PLAN_SERVICE_ALREADY_EXISTS`(409) 예외가
  발생한다.

### 4.3 서비스 희망 일정 등록 (`ServicePreferenceCreateCommand`)

- 요청 주체는 퇴원 예정자(`PATIENT`)다.
- 등록하려는 `planServiceId`가 존재해야 하며(`CARE_PLAN_SERVICE_NOT_FOUND`, 404), 그 상위 Care Plan도 존재해야 한다(`CARE_PLAN_NOT_FOUND`,
  404).
- 요청자가 해당 Care Plan의 소유자여야 한다 (`CarePlanOwnerValidator`, `AUTH_FORBIDDEN` 403).
- Care Plan의 `status`가 `UNDER_REVIEW`가 아니면 `SERVICE_PREFERENCE_NOT_ALLOWED`(409) 예외가 발생한다.
- `preferredDate`는 Care Plan의 `startDate`~`finishDate` 범위 내여야 한다 (`SERVICE_PREFERENCE_DATE_OUT_OF_RANGE`, 400).

### 4.4 서비스 희망 일정 수정

- `01_서비스 희망 일정 수정.md` 참고. 등록(4.3)과 동일하게 Care Plan `status`가 `UNDER_REVIEW`일 때만 가능하며, 요청자가 소유자여야 한다.
- 상세 Request/Response/에러코드는 별도 API 문서로 관리 중이며 일부 항목은 확인 필요 상태다.

### 4.5 Care Plan 상태 변경/확정

- 요청 주체는 `SERVICE_PROVIDER`, `SOCIAL_WORKER`, `ADMIN`, `MASTER` 중 하나다 (`PATIENT`/`HOSPITAL_STAFF`는 이 API를 호출할 수 없음).
- 3장의 상태 전이 규칙(`canTransitionTo`)을 따르며, 허용되지 않는 전이는 `CARE_PLAN_INVALID_STATUS_TRANSITION`(400)을 반환한다.
- 상태가 `CONFIRMED`로 전이되면:
    1. User-Service Internal API(`GET /internal/v1/users/{userId}`)로 환자(`patientId`)의 `regionId`를 조회한다.
    2. Care Plan에 속한 모든 `CarePlanService`/`CarePlanServicePreference`를 모아 `CarePlanConfirmedEvent`를 구성한다.
    3. `ApplicationEventPublisher`로 스프링 내부 이벤트를 발행하고, `@TransactionalEventListener(phase = AFTER_COMMIT)`가 트랜잭션 커밋 이후 실제
       RabbitMQ 발행(`CarePlanEventPublisher`)을 수행한다 — DB 저장과 이벤트 발행 사이의 정합성을 보장하기 위함 (커밋 전 발행 시 롤백된 상태가 발행될 위험 방지).

> ⚠️ 확인 필요 (코드 `TODO` 주석, `CarePlanCommandService.java:111`): "User-Service의 구현/머지 후 실제 연동 확인"이라는 TODO가 남아 있어,
> User-Service Internal API 연동이 실제 환경에서 검증되지 않은 상태다.

### 4.6 Care Plan 조회/검색

- **상세 조회** (`GET /api/v1/care-plans/{carePlanId}`): `PATIENT`, `ADMIN`, `SOCIAL_WORKER`, `MASTER`만 호출 가능. 조회된 Care
  Plan의 `patientId`와 요청자 `userId`가 일치해야 한다 (`AUTH_FORBIDDEN`, 403) — ⚠️ `ADMIN`/`SOCIAL_WORKER`/`MASTER`도 본인 소유가 아니면
  403이 발생하는 구조로 보여, 역할별 접근 범위가 실제 의도와 맞는지 확인이 필요하다 (아래 8장 참고).
- **목록 검색** (`GET /api/v1/care-plans`): `PATIENT`만 호출 가능. `status`/`startDate`/`finishDate` 필터와 페이지네이션을 지원하며, `page < 0`
  이면 `CARE_PLAN_BAD_REQUEST` 예외가 발생한다.
- **환자 기준 내부 조회**(`findByPatient`, Internal API): `SOCIAL_WORKER_VISIBLE_STATUSES`(`CONFIRMED`/`IN_PROGRESS`/
  `COMPLETED`)에 해당하는 Care Plan만 조회 대상이다 — `UNDER_REVIEW`(검토 중, 아직 확정 전) 상태는 제외된다.

> ⚠️ 확인 필요: `ErrorCode.CARE_PLAN_BAD_REQUEST`가 `page < 0` 검증에 쓰이는데 HTTP 상태가 `CONFLICT`(409)로 등록되어 있다 (
`ErrorCode.java:84-87`). 메시지("page는 0 이상이어야 합니다")와 성격상 `BAD_REQUEST`(400)가 맞아 보이나, 코드 원본을 임의로 바꾸지 않고 문서에만 기재해 둔다.

### 4.7 Care Plan 완료

- Schedule-Service가 발행하는 `CarePlanCompleted` 이벤트(RabbitMQ, `care-plan.completed.queue`)를 수신하면 해당 `carePlanId`의 Care
  Plan을 `complete()` 처리한다.
- 외부에 노출되는 REST API는 없다 (이벤트 수신 전용).

### 4.8 Care Plan 삭제 (논리삭제)

- `02_CarePlan삭제.md` 참고. 요청 주체는 `HOSPITAL_STAFF`, `ADMIN`, `MASTER`만 가능하며 (`PATIENT`/`SERVICE_PROVIDER`는 불가), 별도의 소유권
  검증은 없다 — 역할 기준으로만 판단한다.
- Care Plan의 `status`가 `UNDER_REVIEW`일 때만 삭제 가능하며, 그 외 상태(`CONFIRMED`/`IN_PROGRESS`/`COMPLETED`)에서 시도하면
  `CARE_PLAN_DELETE_NOT_ALLOWED`(409) 예외가 발생한다.
- 하위 리소스까지 함께 논리삭제하며, 자식 리소스부터 먼저 삭제한다: `CarePlanServicePreference` → `CarePlanService` → `CarePlan` 순.
- 물리 삭제가 아닌 `deletedAt`/`deletedBy` 컬럼을 채우는 논리삭제(`BaseCreateDeleteEntity.markDeleted`)이며, 삭제 후에도 감사 목적으로 레코드 자체는 유지된다.
  삭제 대상 조회에는 deletedAt IS NULL 조건을 적용하여 논리삭제된 데이터가 일반 조회 대상에 포함되지 않도록 한다.

---

## 5. 서비스 간 연동 (이벤트 & 내부 API)

메시징 브로커: **RabbitMQ** (확정)

### 5.1 전체 흐름

```
[discharge-service] 퇴원 완료
        ↓ (Internal API 조회, actualDate)
[care-plan-service] Care Plan 생성 (UNDER_REVIEW)
        ↓ 서비스 선택 / 희망 일정 등록·수정
[care-plan-service] Care Plan 상태 변경 → CONFIRMED
        ↓ (Internal API 조회, user-service: regionId)
[care-plan-service] CarePlanConfirmed 발행 (트랜잭션 커밋 후)
        ↓
[provider-service] 서비스 제공자 매칭 (schedule-service.md 5.4/5.6 참고)
        ↓ ... (schedule-service가 서비스 일정 생성/변경/완료 처리)
[schedule-service] 모든 서비스 수행 완료 → CarePlanCompleted 발행
        ↓
[care-plan-service] CarePlanCompleted 수신 → CarePlan.status = COMPLETED
```

### 5.2 발행(Publish) 이벤트

| 이벤트명                | 발행 시점                                                        | Exchange / Routing Key                           | 페이로드                                                                                                                                                                             |
|---------------------|--------------------------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CarePlanConfirmed` | Care Plan 상태가 `CONFIRMED`로 전이된 트랜잭션이 커밋된 직후 (`AFTER_COMMIT`) | `care-plan.exchange` / `care-plan.confirmed.key` | `carePlanId`, `regionId`, `services[]` (각 서비스의 `planServiceId`, `provideServiceId`, `preferences[]`), `preferences[]`는 `servicePreferenceId`/`preferredDate`/`preferredTimeSlot` |

### 5.3 수신(Consume) 이벤트

| 이벤트명                | 처리 내용                                        | Queue                       |
|---------------------|----------------------------------------------|-----------------------------|
| `CarePlanCompleted` | 수신한 `carePlanId`의 Care Plan을 `COMPLETED`로 전이 | `care-plan.completed.queue` |

> ⚠️ 확인 필요: `CarePlanCompleted` 이벤트의 발행 주체는 Schedule-Service(`schedule-service.md` 5.2절의 `CarePlanCompleted`)이나, 두 서비스
> 문서 모두 상세 페이로드 필드(`patientId`, `completedAt` 등)가 실제로 Schedule-Service 쪽 구현과 일치하는지 상호 검증이 필요하다. 현재
`CarePlanCompletedEvent`는 `carePlanId`, `patientId`, `completedAt` 3개 필드를 갖는다.

### 5.4 내부(Internal) API — Care-Plan-Service → 타 서비스 (호출 방향)

Care-Plan-Service가 자신의 비즈니스 로직 처리 중 호출하는 동기 내부 API 목록이다. 모두 `infrastructure/client/`의 Feign Client +
`infrastructure/adapter/`의 Port 구현체로 캡슐화되어 있다.

| 대상                | 엔드포인트                                       | 용도                                 | 호출 시점                             |
|-------------------|---------------------------------------------|------------------------------------|-----------------------------------|
| discharge-service | `GET /internal/v1/discharges/{dischargeId}` | 퇴원 건의 `patientId`, `actualDate` 조회 | Care Plan 생성 시 (4.1절)             |
| user-service      | `GET /internal/v1/users/{userId}`           | 사용자의 `role`, `regionId` 조회         | Care Plan `CONFIRMED` 전이 시 (4.5절) |

### 5.5 내부(Internal) API — 타 서비스 → Care-Plan-Service (수신 방향)

Care-Plan-Service가 제공하는 동기 내부 API 목록이다 (`CarePlanInternalController`, `/internal/v1` prefix).

| 엔드포인트                                                                  | Request                               | Response(`data`)                         | 비고                                                                                                                                                         |
|------------------------------------------------------------------------|---------------------------------------|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /internal/v1/care-plans/{patientId}`                              | `patientId` (Path Variable)           | `carePlanId`, `patientId`, `status`      | `CONFIRMED`/`IN_PROGRESS`/`COMPLETED` 상태의 Care Plan만 조회 대상 (`UNDER_REVIEW` 제외). ⚠️ 호출 주체(social-worker-service로 추정)가 코드 주석 등으로 명시되어 있지 않아 확인 필요            |
| `GET /internal/v1/service-preferences/{servicePreferenceId}/care-plan` | `servicePreferenceId` (Path Variable) | `carePlanId`, `patientId`, `finishDate`  | ✅ `schedule-service.md` 5.5절에서 "존재 여부 확인 필요"로 남아있던 Schedule-Service → Care-Plan-Service 방향 Internal API가 바로 이 엔드포인트다 (서비스 일정 변경 시 Care Plan 일정 범위/소유권 검증용) |
| `GET /internal/v1/service-preferences?patientId={patientId}`           | `patientId` (Query Parameter)         | `content`: `servicePreferenceId` UUID 배열 | ✅ `schedule-service.md` 5.7절의 "ID 목록 반환" Internal API. 응답은 `content`가 UUID 문자열 배열 (객체 배열 아님)                                                               |

> ⚠️ 확인 필요: 세 API 모두 인증 방식이 코드상 별도로 구현되어 있지 않다(`X-Internal-Api-Key` 검증 Interceptor/Filter가 `global/security`에 보이지 않음).
`docs/코드_컨벤션_구현용.md`의 컨벤션(Interceptor 기반 `X-Internal-Api-Key` 검증)이 이 서비스에도 아직 적용되지 않은 것으로 보인다.

---

## 6. API 목록

| #  | 기능                             | Method | URL                                                                | 사용자                           | 문서                       |
|----|--------------------------------|--------|--------------------------------------------------------------------|-------------------------------|--------------------------|
| -  | Care Plan 생성                   | POST   | `/api/v1/care-plans`                                               | 병원 담당자, 퇴원예정자                 | -                        |
| -  | Care Plan 상세 조회                | GET    | `/api/v1/care-plans/{carePlanId}`                                  | 퇴원예정자, 사회복지사, ADMIN, MASTER   | -                        |
| -  | Care Plan 목록 검색                | GET    | `/api/v1/care-plans`                                               | 퇴원예정자                         | -                        |
| -  | Care Plan 상태 변경                | PATCH  | `/api/v1/care-plans/{carePlanId}/status`                           | 서비스 제공자, 사회복지사, ADMIN, MASTER | -                        |
| -  | Care Plan 서비스 선택               | POST   | `/api/v1/care-plans/{carePlanId}/services`                         | 퇴원예정자                         | -                        |
| -  | 서비스 희망 일정 등록                   | POST   | `/api/v1/care-plan-services/{planServiceId}/service-preferences`   | 퇴원예정자                         | -                        |
| 01 | 서비스 희망 일정 수정                   | PATCH  | `/api/v1/service-preferences/{servicePreferenceId}`                | 퇴원예정자                         | `api/01_서비스 희망 일정 수정.md` |
| 02 | Care Plan 삭제                   | DELETE | `/api/v1/care-plans/{carePlanId}`                                  | 병원 담당자, ADMIN, MASTER         | `api/02_CarePlan삭제.md`   |
| -  | [내부 API] 환자 기준 Care Plan 조회    | GET    | `/internal/v1/care-plans/{patientId}`                              | (서비스 간)                       | -                        |
| -  | [내부 API] 희망 일정 기준 Care Plan 조회 | GET    | `/internal/v1/service-preferences/{servicePreferenceId}/care-plan` | (서비스 간, schedule-service)     | -                        |
| -  | [내부 API] 환자 소유 희망 일정 ID 목록 조회  | GET    | `/internal/v1/service-preferences?patientId=`                      | (서비스 간, schedule-service)     | -                        |
| -  | [이벤트 발행] CarePlanConfirmed     | -      | RabbitMQ Publish                                                   | -                             | -                        |
| -  | [이벤트 수신] CarePlanCompleted     | -      | RabbitMQ Consume                                                   | -                             | -                        |

> ⚠️ `schedule-service/api/`처럼 API별 번호 체계가 아직 없다. 이 문서 작성 시점(2026-09-04) 기준 `01_서비스 희망 일정 수정.md`만 별도 문서로 존재하며, 나머지는 상세
> API 문서 작성 전이라 이 표에서 번호를 비워두었다. 향후 API 문서를 추가할 때 이 표의 번호와 문서 파일명을 함께 채워나간다.

---

## 7. 인증/인가

- **인증(Authentication)**: API Gateway에서 수행 (JWT 검증). 인증된 요청자 정보는 `X-User-Id`, `X-User-Role` 헤더로 각 서비스에 전달된다.
- Controller는 이 헤더를 직접 읽지 않고, `HeaderAuthenticationFilter`(`OncePerRequestFilter`)가 두 헤더를 파싱해
  `UserContext(userId, role)`를 Spring Security `SecurityContext`에 주입한다. Controller는
  `@AuthenticationPrincipal UserContext user`로 주입받는다.
- 인가는 Controller 메서드의 `@PreAuthorize`(`hasRole`/`hasAnyRole`)로 1차 필터링하고, Service 계층에서 리소스 소유권(`CarePlanOwnerValidator`
  등)을 추가 검증한다.
- **내부 API**(`/internal/v1/*`)는 API Gateway를 거치지 않는 서비스 간 통신이다. `docs/코드_컨벤션_구현용.md` 컨벤션상 `X-Internal-Api-Key` 헤더 기반
  Interceptor 인증을 따라야 하나, 5.5절에서 언급했듯 현재 코드에는 해당 인증 처리가 보이지 않는다 (확인 필요).

---

## 8. 알려진 미확정/논의 사항 정리

| 구분                                               | 내용                                                                                                                                                                                                                                                                      | 상태                  |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| `CarePlan.complete()`의 상태 불일치 처리                 | `IN_PROGRESS`가 아닐 때 예외 없이 조용히 무시 — 코드 내 TODO(`이벤트 계약 이후 수정`) 존재                                                                                                                                                                                                         | 확인 필요               |
| `CONFIRMED` 전이 시 User-Service 연동                 | 코드 내 TODO(`User-Service의 구현/머지 후 실제 연동 확인`) 존재 — 실제 환경 검증 안 됨                                                                                                                                                                                                           | 확인 필요               |
| `CARE_PLAN_BAD_REQUEST`의 HTTP 상태                 | `page < 0` 검증(400 성격)에 쓰이는데 `HttpStatus.CONFLICT`(409)로 등록되어 있음                                                                                                                                                                                                         | 확인 필요               |
| Care Plan 상세 조회의 역할별 접근 범위                       | `PATIENT`, `ADMIN`, `SOCIAL_WORKER`, `MASTER` 모두 허용되나 소유권 검증은 `patientId == userId` 단일 기준이라, `SOCIAL_WORKER`/`ADMIN`/`MASTER`가 타인 소유 Care Plan을 조회할 때도 403이 발생하는 구조로 보임. 의도된 동작인지 확인 필요                                                                                 | 확인 필요               |
| 내부 API 인증(`X-Internal-Api-Key`) 미구현              | `global/security` 패키지에 내부 API 인증 처리가 보이지 않음 (컨벤션 문서 기준 Interceptor 방식이어야 함)                                                                                                                                                                                             | 확인 필요               |
| `GET /internal/v1/care-plans/{patientId}`의 호출 주체 | 코드/주석에 명시가 없어 social-worker-service로 추정만 가능                                                                                                                                                                                                                             | 확인 필요               |
| `01_서비스 희망 일정 수정.md`의 세부 항목                      | ✅ 확정 및 구현 완료 (2026-09-04): 요청자 `PATIENT`만 허용, Method/URL `PATCH /api/v1/service-preferences/{servicePreferenceId}`, 404는 신규 `SERVICE_PREFERENCE_NOT_FOUND`, `UNDER_REVIEW`가 아니면 409(`SERVICE_PREFERENCE_NOT_ALLOWED`), `preferredDate`는 등록 API와 동일하게 Care Plan 기간 범위 검증 | ✅ 확정                |
| `CarePlanCompleted` 이벤트 페이로드 상호 검증               | Schedule-Service가 발행하는 필드와 Care-Plan-Service가 기대하는 필드(`carePlanId`/`patientId`/`completedAt`)의 일치 여부 미검증                                                                                                                                                                | 확인 필요               |
| 서비스 간 Internal API 스펙 상호 확정                      | `GET /internal/v1/service-preferences/{servicePreferenceId}/care-plan`, `GET /internal/v1/service-preferences?patientId=` 두 API는 `schedule-service.md`에서 미확정으로 남아있던 항목이었음 — 실제 구현이 존재함을 이 문서에서 확인, 두 문서 간 스펙 동기화 필요                                                     | ✅ 존재 확인 / 문서 동기화 필요 |
| `CarePlan` 생성 기간 고정값(30일)                        | `CARE_PLAN_PERIOD_DAYS = 30`으로 하드코딩 — Care Plan마다 가변적일 수 있는지, 정책적으로 고정인지 확인 필요                                                                                                                                                                                          | 확인 필요               |
| 트랜잭션 커밋 후 이벤트 발행                                 | `CarePlanConfirmedEvent`를 `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`로 발행하도록 확정 적용됨 (커밋 전 발행 시 롤백된 상태가 새어나가는 것을 방지)                                                                                                                       | ✅ 확정 (구현 완료)        |

---

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-04 | 최초 작성 — Notion `Table 명세서`(3개 테이블) 및 실제 코드(`domain/entity`, `application`, `presentation`, `infrastructure`) 기반으로 전체 구조화. 원본 표의 셀 밀림으로 인해 `p_care_plan_service_preferences.plan_service_id`의 참조 대상, `p_care_plan_services`의 `care_plan_id`/`provide_service_id` 설명이 뒤바뀌어 있던 것을 엔티티 코드 기준으로 정정. 코드 내 `TODO` 주석 2건(`CarePlan.complete()`, User-Service 연동)과 `CARE_PLAN_BAD_REQUEST` 상태 코드 불일치를 8장에 확인 필요로 반영. `schedule-service.md`에서 미확정으로 남아있던 Internal API 2건(희망 일정→Care Plan 조회, 희망 일정 ID 목록 조회)이 이미 구현되어 있음을 확인하여 8장에 반영 |
| 2026-09-04 | 서비스 희망 일정 수정 API(`01_서비스 희망 일정 수정.md`) 구현 완료 — `SERVICE_PREFERENCE_NOT_FOUND` 에러 코드 신설, `CarePlanServicePreference.updatePreference()` 도메인 메서드, `ServicePreferenceCommandRepository.findById()`, `ServicePreferenceCommandService.updateServicePreference()`, `PATCH /api/v1/service-preferences/{servicePreferenceId}` 엔드포인트 추가. 8장 해당 항목을 확정으로 전환                                                                                                                                                                         |
| 2026-09-05 | Care Plan 삭제 API(`02_CarePlan삭제.md`) 구현 완료 — `CARE_PLAN_DELETE_NOT_ALLOWED` 에러 코드 신설, `CarePlan`/`CarePlanService`/`CarePlanServicePreference`에 `delete()` 도메인 메서드 추가, `CarePlanServiceCommandRepository.findAllByCarePlanId()`/`ServicePreferenceCommandRepository.findAllByPlanServiceIds()` 추가, `CarePlanCommandService.deleteCarePlan()`(하위 리소스 `preference → service → carePlan` 순 논리삭제), `DELETE /api/v1/care-plans/{carePlanId}` 엔드포인트(`HOSPITAL_STAFF`/`ADMIN`/`MASTER`만 허용) 추가. 4.8절 신설, API 목록에 02번 반영            |
