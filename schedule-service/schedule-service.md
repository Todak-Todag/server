# 🗓️ Schedule-Service 기능 명세

> 담당 서비스: `schedule-service` (Port `19004`)
출처: 아키텍처 다이어그램, Notion `Table 명세서` / `API 명세서`(13개 엔드포인트), 팀 대화 확인 내용
API 상세 스펙은 `api/` 하위 개별 문서를 참고한다.
>

---

## 1. 서비스 개요

Schedule-Service는 **확정된 Care Plan에 포함된 서비스의 실제 일정(예약)을 관리**하는 도메인 서비스다. Care-Plan-Service에서 Care Plan이 확정되면 Provider-Service의 매칭 결과를 이벤트로 수신하여 일정을 생성하고, 퇴원 예정자의 일정 변경/취소, 서비스 제공자의 수행 완료 처리, 수행 결과 등록·조회를 담당한다.

담당 테이블: `p_service_schedules`(서비스 일정), `p_care_plan_service_results`(서비스 수행 결과)

---

## 2. 도메인 모델

### `p_service_schedules` — 서비스 일정

| 컬럼명 | 타입 | PK | FK/참조 | Nullable | 제약조건/기본값 | 설명 |
| --- | --- | --- | --- | --- | --- | --- |
| service_schedule_id | UUID | O |  | X |  | 서비스 일정 ID |
| service_preference_id | UUID |  | 논리 FK → p_care_plan_service_preferences.service_preference_id | X |  | 서비스 희망 ID |
| service_offering_id | UUID |  | 논리 FK → p_provide_service_offerings.service_offering_id | X |  | 제공 서비스 ID |
| status | ENUM |  |  | X | SCHEDULED / RESCHEDULING / CHANGED / COMPLETED / CANCELED / NO_SHOW, 기본값 SCHEDULED | 일정 상태 |
| date | DATE |  |  | X |  | 날짜 |
| started_at | TIMESTAMP |  |  | X |  | 시작 시간 |
| finished_at | TIMESTAMP |  |  | X |  | 종료 시간 |
| cancel_reason | TEXT |  |  | O |  | 취소 사유 |
| canceled_at | TIMESTAMP |  |  | O |  | 취소 일시 |
| created_at / created_by | TIMESTAMPZ / UUID |  |  | X |  | 생성 정보 |
| updated_at / updated_by | TIMESTAMPZ / UUID |  |  | X |  | 수정 정보 |
| deleted_at / deleted_by | TIMESTAMPZ / UUID |  |  | O |  | 논리 삭제 정보 |

### `p_care_plan_service_results` — 서비스 수행 결과

| 컬럼명 | 타입 | PK | FK/참조 | Nullable | 설명 |
| --- | --- | --- | --- | --- | --- |
| service_result_id | UUID | O |  | X | 케어플랜 내의 서비스 결과 ID |
| service_schedule_id | UUID |  | 논리 FK → p_service_schedules.service_schedule_id | X | 서비스 일정 ID |
| started_at | TIMESTAMP |  |  | X | 서비스 시작 일시 |
| finished_at | TIMESTAMP |  |  | X | 서비스 종료 일시 |
| note | TEXT |  |  | O | 비고 |
| created_at / created_by | TIMESTAMPZ / UUID |  |  | X | 생성 정보 |
| updated_at / updated_by | TIMESTAMPZ / UUID |  |  | X | 수정 정보 |
| deleted_at / deleted_by | TIMESTAMPZ / UUID |  |  | O | 논리 삭제 정보 |

---

## 3. 상태(status) 정의

| 상태 | 의미 |
| --- | --- |
| `SCHEDULED` | 예정 (기본값) |
| `RESCHEDULING` | **변경 중** (일정 변경 요청 후 재매칭 진행 중인 중간 상태) |
| `CHANGED` | **변경 완료** (재매칭이 성공하여 변경이 확정된 최종 상태) |
| `CANCELED` | 취소됨 (`cancel_reason`, `canceled_at` 기록) |
| `COMPLETED` | 수행 완료 |
| `NO_SHOW` | 예약 부도(미수행) |

>
>
>
> ⚠️ **확인 필요 (미해소)**:
>
> 1. `ProviderReMatched`는 현재 "Schedule-Service가 발행만 함"으로 기록되어 있는데(11번 문서), 재매칭 결과를 Schedule-Service가 다시 알아야 `CHANGED`/`SCHEDULED` 전환이 가능하다. 이 결과를 어떤 이벤트로 수신하는지 불명확하다 (임의로 만들어내지 않음, 11번 문서 참고).
> 2. **문서 간 상충**: `03_서비스일정변경.md`(2026-09-02 갱신, 최신 확정본)는 "재매칭 실패 시 `SCHEDULED`로 복구"라고 명시하는데, `13_이벤트수신_ProviderMatchFailed.md`(기존)는 "매칭 실패 시 `CANCELED`로 변경"이라고 되어 있다. 두 문서 모두 임의로 통일하지 않았으니 팀 확인 필요.

---

## 4. 핵심 비즈니스 규칙

### 4.1 서비스 일정 변경(연기)

- **요청 주체는 퇴원 예정자다** (확정).
- 일정 **시작 24시간 전까지만** 변경 가능하다.
- 본인에게 배정된 일정만 변경 가능하며, `status`가 `SCHEDULED`인 경우에만 가능하다.
- 기존 일정 날짜 `D` 기준 **하루 앞당기기(D-1)** 또는 **하루 미루기(D+1)** 만 가능하다.
  - 하루 앞당기기: 변경일이 오늘(당일)인 경우 불가
  - 하루 미루기: Care Plan의 일정 범위(`finishDate`)를 초과할 수 없음
- "하루 미루기" 범위 검증 및 소유권 검증을 위해 `servicePreferenceId`를 기준으로 **care-plan-service Internal API(Feign)를 호출**하여 `carePlanId`, `finishDate`, `patientId`를 조회한다. schedule-service와 care-plan-service는 데이터 소유권이 분리되어 있으므로 DB 직접 조인이 아닌 Internal API 호출로 처리한다 (상세: 5.5절 참고).
- 변경 요청이 접수되면 `status`는 `RESCHEDULING`(**변경 중**, 중간 상태)으로 바뀌고, 변경된 날짜에 대해 `ProviderReMatched` 이벤트를 발행한다.
- 재매칭 **성공** 시 `status`는 `CHANGED`(**변경 완료**, 최종 상태)로 전환된다.
- 재매칭 **실패** 시 `status`는 `SCHEDULED`로 복구된다 — ⚠️ 단, `13_이벤트수신_ProviderMatchFailed.md`(기존)는 "`CANCELED`로 전환"이라고 되어 있어 상충 상태다 (3장, 8장 참고 — 확인 필요).

### 4.2 서비스 일정 취소

- 요청 주체는 퇴원 예정자다.
- **일정 시작 24시간 전까지만** 취소 가능하다.
- 취소 시 `status`는 `CANCELED`로 변경되고 `cancelReason`, `canceledAt`이 기록된다.
- 이미 완료되었거나 취소된 서비스는 `409 CONFLICT`를 반환한다.

### 4.3 서비스 수행 완료 처리

- 요청 주체는 서비스 제공자다.
- `status`가 `SCHEDULED`일 때만 변경 가능하며, 요청 시점이 일정의 `finishedAt` 이후여야 한다.
- 이미 취소/연기/변경완료/완료/부도 처리된 서비스는 `409 CONFLICT`를 반환한다.
- 결과 값은 `COMPLETED` 또는 `NO_SHOW` 중 하나다.
- URL: `PATCH /api/v1/service-schedules/{serviceScheduleId}/complete`
- "본인이 배정된 서비스 제공자인지" 검증은 **provider-service Internal API**(`GET /internal/v1/service-offerings/{serviceOfferingId}`)를 호출해 `providerId`를 조회하고 요청자 `userId`와 비교하는 방식으로 확정됨 (상세: 5.6절 참고).

### 4.4 서비스 수행 결과 등록

- 요청 주체는 서비스 제공자다.
- 실제 서비스 시작/종료 일시와 비고(`note`)를 등록한다.

### 4.5 조회 권한 공통 규칙

- 퇴원 예정자는 **본인이 받은** 일정/결과만 조회할 수 있다.
- 서비스 제공자는 **본인이 제공한** 일정/결과만 조회할 수 있다.

---

## 5. 서비스 간 연동 (이벤트 & 내부 API)

메시징 브로커: **RabbitMQ** (확정)

### 5.1 전체 흐름

```
Care-Plan-Service (CarePlanConfirmed 발행)
        ↓
Provider-Service (매칭 가능 Provider 조회 시, 내부 API로 Schedule-Service 호출)
        ↓
Provider-Service (ProviderMatched 발행) ──▶ Schedule-Service 수신 ──▶ 일정 생성 (SCHEDULED)
        ↓ (매칭 실패 시)
Provider-Service (ProviderMatchFailed 발행) ──▶ Schedule-Service 수신 ──▶ 일정 상태 변경

Schedule-Service (일정 연기, 퇴원예정자 요청)
   → Care-Plan-Service Internal API 호출 (carePlanId/finishDate 조회, 5.5절)
   → ProviderReMatched 발행 ──▶ Provider-Service 재매칭

Schedule-Service (모든 서비스 수행 완료) ──▶ CarePlanCompleted 발행 ──▶ Care-Plan-Service 수신 (p_care_plans.status → COMPLETED)
```

### 5.2 발행(Publish) 이벤트

| 이벤트명 | 발행 시점 | 상세 문서 |
| --- | --- | --- |
| `CarePlanCompleted` | 서비스 수행 결과 등록으로 케어플랜의 서비스가 모두 완료되었을 때 | `api/10_이벤트발행_CarePlanCompleted.md` |
| `ProviderReMatched` | 서비스 일정이 연기(변경)되었을 때 (**기존 건 갱신 전담**, `RESCHEDULING`→`CHANGED`) | `api/11_이벤트발행_ProviderReMatched.md` |

### 5.3 수신(Consume) 이벤트

| 이벤트명 | 처리 내용 | 상세 문서 |
| --- | --- | --- |
| `ProviderMatched` | Provider-Service의 매칭 결과를 수신하여 `p_service_schedules`에 **새 일정 생성 전담** (신규 생성만, 확정) | `api/12_이벤트수신_ProviderMatched.md` |
| `ProviderMatchFailed` | Provider-Service의 매칭 실패를 수신하여 일정 상태를 변경 | `api/13_이벤트수신_ProviderMatchFailed.md` |

> ⚠️ **이벤트 전체 미확정 (2026-09-01 팀 확인)**: 4개 이벤트의 실제 페이로드 필드, Exchange/Queue/Routing Key 이름, 재시도·DLQ 정책, 멱등성 처리 방식은 **모두 미확정 상태**다. 특히 `ProviderMatched` 수신 이벤트는 `p_service_schedules`의 NOT NULL 컬럼(`date`, `started_at`, `finished_at`)에 대응하는 필드가 페이로드에 없어, 실제 개발 착수 전 Provider-Service 팀과 페이로드를 재정의해야 한다. 각 이벤트 문서에 TBD로 표시했다.
>

### 5.4 내부(Internal) API — Provider-Service → Schedule-Service (수신 방향)

Schedule-Service는 Provider-Service가 매칭 가능 Provider를 판단할 때 호출하는 동기 내부 API를 제공한다.

- **엔드포인트**: `GET /internal/v1/service-schedules`
- **인증**: `X-Internal-Api-Key` 헤더 (서비스별 환경변수로 보유한 Key와 대조하여 검증). 검증은 **Interceptor**에서 수행하며 Controller는 이 헤더를 직접 처리하지 않는다.
- **설계 근거**: 서비스 간 관계가 모두 "논리 FK"로만 연결되어 있어 DB가 분리되어 있다. Provider-Service의 `p_provide_works`는 "제공 가능한 요일/시간대"만 알고 있고, 실제 예약 현황은 Schedule-Service의 `p_service_schedules`에만 있어 SQL JOIN이 불가능하므로 동기 내부 호출로 대체한다.
- **응답 설계 원칙**: 이 API는 "가능/불가능" boolean을 직접 판단하지 않고 해당 기간의 기존 일정 목록을 그대로 반환한다. 시간대 겹침 판단은 Provider-Service가 직접 수행한다(책임 분리).
- **조회 대상 상태** : `SCHEDULED`, `RESCHEDULING` 상태 일정만 반환 (기존엔 `SCHEDULED`만이었으나 확장됨). `COMPLETED`/`NO_SHOW`/`CANCELED`는 향후 일정과 충돌하지 않아 제외.
- **Batch 조회 전략** : 개별 호출 대신 `startDate`부터 **30일간(고정값)**의 일정을 한 번에 반환 (Care Plan 최대 일정 범위 기준, Care Plan마다 가변적이지 않음). 파라미터도 `from`/`to`에서 `startDate` 단일 필수값으로 변경됨.
- 상세 스펙: `api/06_내부API_서비스제공자일정조회.md`

### 5.5 내부(Internal) API — Schedule-Service → Care-Plan-Service (호출 방향)

Schedule-Service가 서비스 일정 변경(4.1) 처리 중 Care Plan의 일정 범위를 검증하기 위해 호출하는 동기 내부 API. `03_서비스일정변경.md` 최신화 과정에서 팀 확인을 거쳐 반영되었다.

- **호출 방향**: Schedule-Service → Care-Plan-Service (5.4의 Provider-Service → Schedule-Service 호출과 반대 방향)
- **조회 기준**: `servicePreferenceId`
- **조회 결과**: `carePlanId`, `finishDate`, `patientId`
- **용도**: "하루 미루기" 요청 시 변경하려는 날짜가 Care Plan의 일정 범위(`finishDate`)를 초과하지 않는지 검증(03번), 일정 소유권(`patientId`) 검증(03/04번 공통)
- **구현 위치**: `infrastructure/client/` (Feign)
- **인증**: `X-Internal-Api-Key` 헤더 기반 (5.4와 동일 패턴)

> ⚠️ **확인 필요 (신규)**:
>
> 1. 이 호출이 연기 요청 접수 시점(사전 검증)에만 1회 이뤄지는지, 재매칭 실패로 `SCHEDULED` 복구 시에도 재조회가 필요한지 명시되어 있지 않음.
> 2. Care-Plan-Service Internal API 자체가 실패(서비스 장애, Care Plan 미존재 등)했을 때의 에러 처리/상태 코드가 정해져 있지 않음.
> 3. Care-Plan-Service 측에 이 조회를 위한 Internal API 엔드포인트가 실제로 존재하는지, 엔드포인트 URL/응답 필드 스펙이 무엇인지 아직 문서화되어 있지 않음 (Care-Plan-Service 쪽 API 명세서 확인 필요).

### 5.6 내부(Internal) API — Schedule-Service → Provider-Service (호출 방향)

Schedule-Service가 서비스 수행 완료 처리(4.3) 시 요청자가 해당 일정에 배정된 서비스 제공자 본인인지 검증하기 위해 호출하는 동기 내부 API. `05_서비스수행완료.md` 최신화 과정에서 확인되었다.

- **호출 방향**: Schedule-Service → Provider-Service (5.4의 Provider-Service → Schedule-Service 호출과 반대 방향, 5.5와는 별개의 서비스/API)
- **엔드포인트**: `GET /internal/v1/service-offerings/{serviceOfferingId}`
- **조회 결과**: `providerId`
- **용도**: 조회한 `providerId`와 요청자(`UserContext`)의 `userId`를 비교하여 서비스 일정에 배정된 서비스 제공자 본인인지 검증
- **구현 위치**: `infrastructure/client/` (Feign)
- **인증**: `X-Internal-Api-Key` 헤더 기반 (5.4, 5.5와 동일 패턴)

> ⚠️ **확인 필요 (신규)**:
>
> 1. 이 API의 정확한 Response 필드 구조(`providerId`가 최상위 필드인지, 다른 객체에 중첩되어 있는지)가 명시되어 있지 않음.
> 2. 존재하지 않는 `serviceOfferingId`로 조회했을 때의 에러 처리/상태 코드가 정해져 있지 않음.

### 5.7 내부(Internal) API — Schedule-Service → Care-Plan-Service / Provider-Service (ID 목록 조회)

Schedule-Service가 서비스 일정 목록 조회(01번 API)에서 요청자 소유 레코드만 필터링하기 위해 호출하는 동기 내부 API. `01_서비스일정목록조회.md` 최신화 과정에서 팀 논의를 거쳐 확정되었다.

- **배경**: 5.5/5.6절의 Internal API는 모두 특정 레코드 하나의 소유권을 검증하는 **단건(point) 조회**다. 목록 조회에 이 방식을 그대로 적용하면 레코드 수만큼 개별 호출이 필요해 (1) 네트워크 호출이 N번으로 늘어나고 (2) 필터링이 애플리케이션 레벨에서 끝난 뒤에야 가능해 DB 레벨 페이지네이션이 불가능해진다. 따라서 목록 조회 전용으로 "소유 ID 전체 목록"을 한 번에 반환하는 별도의 배치(batch) 조회 방식이 필요하다.
- **퇴원 예정자가 조회하는 경우**: care-plan-service Internal API에 `patientId`(요청자 `userId`)를 전달하여 해당 환자의 Care Plan에 속한 **모든 `servicePreferenceId` 목록**을 조회한다.
- **서비스 제공자가 조회하는 경우**: provider-service Internal API에 `providerId`(요청자 `userId`)를 전달하여 해당 제공자의 **모든 `serviceOfferingId` 목록**을 조회한다.
- 조회한 ID 목록으로 `p_service_schedules WHERE service_preference_id IN (:ids)` 또는 `WHERE service_offering_id IN (:ids)`를 적용하고, `status`/`date` 필터와 페이지네이션을 함께 적용한다.
- **구현 위치**: `infrastructure/client/` (Feign, 5.5/5.6과 동일 위치)
- **인증**: `X-Internal-Api-Key` 헤더 기반 (5.4~5.6과 동일 패턴)

### 엔드포인트 스펙

| 대상 | Method/URL | Request | Response |
| --- | --- | --- | --- |
| care-plan-service | `GET /internal/v1/service-preferences?patientId={patientId}` | `patientId` (UUID, Query Parameter) | `data.content`: `servicePreferenceId` UUID 배열 |
| provider-service | `GET /internal/v1/service-offerings?providerId={providerId}` | `providerId` (UUID, Query Parameter) | `data.content`: `serviceOfferingId` UUID 배열 |

두 API 모두 공통 `ApiResponse` 포맷(`success`/`code`/`message`/`data`)을 따르며, `data.content`는 UUID 문자열 배열이다 (객체 배열이 아님).

```jsx
// GET /internal/v1/service-offerings?providerId={providerId} 응답 예시
{
  "success": true,
  "code": 200,
  "message": "제공 서비스 목록 조회 성공",
  "data": {
    "content": ["9b2f1c3a-1111-4a2b-8c3d-abcdef123456", "5d8a7e2b-2222-4c3d-9e4f-fedcba654321"]
  }
}
```

### 5.8 내부(Internal) API — Care-Plan-Service → Schedule-Service (수신 방향)

Care-Plan-Service가 `CarePlanCompleted` 이벤트(5.2절)를 수신한 뒤, 이벤트 페이로드에 담긴 `serviceResultId`가 실제 존재하는 데이터인지 검증하기 위해 호출하는 동기 내부 API. 5.4절과 같은 **수신(inbound) 방향**이지만 호출 주체가 Provider-Service가 아닌 Care-Plan-Service라는 점이 다르다.

- **엔드포인트**: `GET /internal/v1/service-results/{serviceResultId}`
- **용도**: `CarePlanCompleted` 이벤트 페이로드의 `serviceResultId` 존재 검증
- **응답 필드**: `serviceResultId`, `serviceScheduleId`, `startedAt`, `finishedAt`
- **필터**: `deletedAt IS NULL`인 수행 결과만 반환
- **인증**: `X-Internal-Api-Key` 헤더, Interceptor에서 검증 (Controller는 직접 처리하지 않음 — 5.4절과 동일 패턴)
- 상세 스펙: `10_내부API_서비스수행결과조회.md` 참고

---

## 6. API 목록

| # | 기능 | Method | URL | 문서 |
| --- | --- | --- | --- | --- |
| 01 | 서비스 일정 목록 조회 | GET | `/api/v1/service-schedules` | 01 |
| 02 | 서비스 일정 상세 조회 | GET | `/api/v1/service-schedules/{serviceScheduleId}` | 02 |
| 03 | 서비스 일정 변경 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/status` | 03 |
| 04 | 서비스 일정 취소 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/cancel` | 04 |
| 05 | 서비스 수행 완료 상태 변경 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/complete` | 05 |
| 06 | \[내부 API\] 서비스 제공자 일정 조회 | GET | `/internal/v1/service-schedules` | 06 |
| 07 | 서비스 수행 결과 등록 | POST | `/api/v1/service-results/{serviceScheduleId}` | 07 |
| 08 | 서비스 수행 결과 목록 조회 | GET | `/api/v1/service-results` | 08 |
| 09 | 서비스 수행 결과 상세 조회 | GET | `/api/v1/service-results/{serviceResultId}` | 09 |
| 10 | \[내부 API\] 서비스 수행 결과 조회 | GET | `/internal/v1/service-results/{serviceResultId}` | 10 |
| 11 | \[이벤트 발행\] CarePlanCompleted | - | RabbitMQ Publish | 11 |
| 12 | \[이벤트 발행\] ProviderReMatched | - | RabbitMQ Publish | 12 |
| 13 | \[이벤트 수신\] ProviderMatched | - | RabbitMQ Consume | 13 |
| 14 | \[이벤트 수신\] ProviderMatchFailed | - | RabbitMQ Consume | 14 |

> ⚠️ 참고: API 목록에는 Schedule-Service가 **호출하는** Care-Plan-Service Internal API(5.5절, `carePlanId`/`finishDate`/`patientId` 단건 조회), Provider-Service Internal API(5.6절, `providerId` 단건 조회), 그리고 목록 조회용 ID 목록 반환 API(5.7절)가 아직 별도 번호로 등록되어 있지 않다. 세 API 모두 상대 서비스 쪽 명세이므로 이 목록에는 포함하지 않되, 각각 03/04번, 05번, 01번 문서와 5.5/5.6/5.7절에서 연동 대상으로 참조만 한다.
>

---

## 7. 인증/인가

- **인증(Authentication)**: API Gateway에서 수행 (JWT 검증). 인증된 요청자 정보는 `X-User-Id`, `X-User-Role` 헤더로 각 서비스에 전달된다.
  - Controller는 이 헤더를 직접 읽지 않고 **`@AuthenticationPrincipal UserContext user`*로 주입받는다 (Spring Security 기반, 06번 내부 API의 Interceptor 처리와 유사한 패턴). 헤더→`UserContext` 변환 로직(Filter/Resolver)은 아직 코드에 없음.
  - ⚠️ **확인 필요**: 이 패턴이 03번 문서에서만 확인됐다. 나머지 8개 REST API(01,02,04,05,07,08,09 + 06은 내부용이라 별개)도 동일하게 `@AuthenticationPrincipal UserContext user`를 쓰는지, 각 API 문서에 개별적으로 반영이 필요한지 확인 필요.
- **인가(Authorization)**: 각 API를 처리하는 Schedule-Service 내부에서 수행한다 (예: 본인 소유 일정인지, 역할이 일치하는지 검증).
- **내부 API**(`/internal/v1/*`)는 API Gateway를 거치지 않는 서비스 간 통신이며, `X-Internal-Api-Key` 헤더 기반으로 별도 인증한다. 5.5절의 Schedule-Service → Care-Plan-Service 호출도 동일한 인증 패턴을 따른다.
