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

---

## 6. API 목록

| # | 기능 | Method | URL | 문서 |
| --- | --- | --- | --- | --- |
| 01 | 서비스 일정 목록 조회 | GET | `/api/v1/service-schedules` | 01 |
| 02 | 서비스 일정 상세 조회 | GET | `/api/v1/service-schedules/{serviceScheduleId}` | 02 |
| 03 | 서비스 일정 변경 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/status` | 03 |
| 04 | 서비스 일정 취소 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/cancel` | 04 |
| 05 | 서비스 수행 완료 상태 변경 | PATCH | `/api/v1/service-schedules/{serviceScheduleId}/complete` | 05 |
| 06 | [내부 API] 서비스 제공자 일정 조회 | GET | `/internal/v1/service-schedules` | 06 |
| 07 | 서비스 수행 결과 등록 | POST | `/api/v1/service-results/{serviceScheduleId}` | 07 |
| 08 | 서비스 수행 결과 목록 조회 | GET | `/api/v1/service-results` | 08 |
| 09 | 서비스 수행 결과 상세 조회 | GET | `/api/v1/service-results/{serviceResultId}` | 09 |
| 10 | [이벤트 발행] CarePlanCompleted | - | RabbitMQ Publish | 10 |
| 11 | [이벤트 발행] ProviderReMatched | - | RabbitMQ Publish | 11 |
| 12 | [이벤트 수신] ProviderMatched | - | RabbitMQ Consume | 12 |
| 13 | [이벤트 수신] ProviderMatchFailed | - | RabbitMQ Consume | 13 |

> ⚠️ 참고: API 목록에는 Schedule-Service가 **호출하는** Care-Plan-Service Internal API(5.5절, `carePlanId`/`finishDate`/`patientId` 단건 조회), Provider-Service Internal API(5.6절, `providerId` 단건 조회), 그리고 목록 조회용 ID 목록 반환 API(5.7절)가 아직 별도 번호로 등록되어 있지 않다. 세 API 모두 상대 서비스 쪽 명세이므로 이 목록에는 포함하지 않되, 각각 03/04번, 05번, 01번 문서와 5.5/5.6/5.7절에서 연동 대상으로 참조만 한다.
>

---

## 7. 인증/인가

- **인증(Authentication)**: API Gateway에서 수행 (JWT 검증). 인증된 요청자 정보는 `X-User-Id`, `X-User-Role` 헤더로 각 서비스에 전달된다.
  - Controller는 이 헤더를 직접 읽지 않고 **`@AuthenticationPrincipal UserContext user`*로 주입받는다 (Spring Security 기반, 06번 내부 API의 Interceptor 처리와 유사한 패턴). 헤더→`UserContext` 변환 로직(Filter/Resolver)은 아직 코드에 없음.
  -
- **인가(Authorization)**: 각 API를 처리하는 Schedule-Service 내부에서 수행한다 (예: 본인 소유 일정인지, 역할이 일치하는지 검증).
- **내부 API**(`/internal/v1/*`)는 API Gateway를 거치지 않는 서비스 간 통신이며, `X-Internal-Api-Key` 헤더 기반으로 별도 인증한다. 5.5절의 Schedule-Service → Care-Plan-Service 호출도 동일한 인증 패턴을 따른다

---

## 8. 알려진 미확정/논의 사항 정리

| 구분 | 내용 | 상태 |
| --- | --- | --- |
| 이벤트 페이로드 | `CarePlanCompleted`, `ProviderReMatched`, `ProviderMatched`, `ProviderMatchFailed` 4종 전체 | 미확정 (TBD) |
| Exchange/Queue/Routing Key | 4개 이벤트 전부 실제 이름 미정 | 미확정 (TBD) |
| 재시도/DLQ 정책 | 수신 이벤트(`ProviderMatched`, `ProviderMatchFailed`) | 미확정 (TBD) |
| 멱등성 처리 | 수신 이벤트 2종 | 미확정 (TBD) |
| `CarePlanCompleted` 발행 조건 | 케어플랜 내 모든 서비스 완료 판단을 위한 정확한 집계 로직 | 미확정 (TBD) |
| `status` 명칭 | `DELAY` → `RESCHEDULING` 확정 완료 (2026-09-01, 코드/문서 전체 반영) | ✅ 확정 |
| Error Response 포맷 | `success/code/message/details/timestamp` (details는 고정 레코드 `ErrorDetail(reason)`, reason은 항상 errorCode 기본 메시지)로 확정, 코드와 Notion API 문서 일치 | ✅ 확정 |
| 인증 헤더 | 전체 9개 REST API Request에 `X-User-Role` 헤더 존재 확인 | ✅ 반영 완료 |
| 외부 API 인증 주입 방식 | 03번 문서 기준 `@AuthenticationPrincipal UserContext user`로 확정. 나머지 8개 API 문서에도 동일 적용되는지, 각 문서의 Request 표에서 `X-User-Id`/`X-User-Role` 행을 제거해야 하는지 확인 필요 | 확인 필요 (03번만 반영됨) |
| `ProviderMatchFailed` 처리 시 기록 위치 | 문서상 "note"에 실패 사유 기록으로 되어 있으나 테이블엔 `note` 컬럼이 없고 `cancel_reason`만 존재 | 확인 필요 |
| SSE 알림 | 매칭 실패 시 SSE 알림 전송 여부 | 확인 필요 (범위 포함 여부 미정) |
| 내부 API Batch 조회 범위 | `startDate`부터 30일간 고정 조회 (Care Plan마다 가변적이지 않음, 06번 API 문서 참고) | ✅ 확정 |
| `CHANGED` 상태 신규 추가 | Table 명세서 재확인 결과 `RESCHEDULING`(변경 중)과 별도로 `CHANGED`(변경 완료) 상태가 존재. ✅ 확정: 신규 생성=`ProviderMatched`, 기존 건 갱신(`RESCHEDULING`→`CHANGED`)=`ProviderReMatched` 흐름으로 역할 분리됨 | ✅ 확정 (역할 분리) / ⚠️ 확인 필요 (아래 참고) |
| `ProviderReMatched`의 수신(확인) 측 누락 | 현재 11번 문서는 Schedule-Service가 "발행"만 하는 것으로 기록되어 있으나, "기존 건 갱신"을 수행하려면 Provider-Service의 재매칭 성공 응답을 Schedule-Service가 다시 **수신**하는 과정이 필요해 보임. 같은 이벤트명이 반대 방향으로도 쓰이는지, 별도 이벤트가 있는지 확인 필요 (임의로 만들어내지 않음) | 확인 필요 |
| 재매칭 실패 시 상태 전환 상충 | `03_서비스일정변경.md`(2026-09-02 갱신, 최신 확정본)는 "실패 시 `SCHEDULED`로 복구", `13_이벤트수신_ProviderMatchFailed.md`(기존)는 "실패 시 `CANCELED`로 변경"이라고 서로 다르게 기재됨. 03번이 더 최근에 팀 확인을 거쳤으나, 13번 문서를 아직 그에 맞춰 갱신하지 않았으므로 두 문서 중 하나로 임의 통일하지 않음 | 확인 필요 (상충) |
| 내부 API(06) Batch 조회 대상에 `CHANGED` 포함 여부 | ✅ 확정 (2026-09-01): `CHANGED`는 Batch 조회 대상에 **포함하지 않음** | ✅ 확정 |
| **Care Plan 범위 검증용 Internal API (신규)** | Schedule-Service → Care-Plan-Service 방향의 Internal API(`servicePreferenceId` → `carePlanId`/`finishDate`/`patientId`, 5.5절)가 `03_서비스일정변경.md`/`04_서비스일정취소.md` 최신화로 신규 확인됨. `carePlanId`/`finishDate`/`patientId` 3개 필드를 모두 반환하는 것은 확정됐으나 (1) 재조회 필요 시점, (2) 실패 시 에러 처리, (3) Care-Plan-Service 측 실제 엔드포인트 스펙 3가지가 미정 | 확인 필요 (신규) |
| **서비스 제공자 본인 확인 메커니즘** | ✅ 확정 (2026-09-03): provider-service Internal API(`GET /internal/v1/service-offerings/{serviceOfferingId}`)로 `providerId`를 조회해 요청자 `userId`와 비교하는 방식으로 확정됨 (5.6절 참고). 다만 이 API의 정확한 Response 필드 구조, `serviceOfferingId` 미존재 시 에러 처리는 미정 | ✅ 확정 (메커니즘) / 확인 필요 (세부 스펙) |
| **05번 API의 400 Validation 케이스 부재 (신규, 2026-09-03)** | `status`(COMPLETED/NO_SHOW) Enum 검증 실패 시의 400 케이스가 Status 표에 없음. 다른 API와 달리 의도적으로 뺀 것인지 누락인지 확인 필요 | 확인 필요 (신규) |
| **status 필터 목록의 CHANGED 누락** | ✅ 해소 (2026-09-03): Notion 원본이 갱신되어 `01_서비스일정목록조회.md`의 `status` 옵션 목록에 `CHANGED`가 추가됨 | ✅ 해소 |
| **01번 Response의 servicePreferenceId/serviceOfferingId 노출 여부** | 한때 Notion 원본에 추가됐다가 다시 제거됨 — ✅ 확정 (2026-09-03, 팀 확인 완료): 내부 필터링에만 쓰이는 ID라 클라이언트 응답에는 노출하지 않는 것으로 최종 확정 | ✅ 확정 |
| **01번 목록 조회 시 소유권 검증 방식** | ✅ 확정 (2026-09-03, 팀 논의 완료): 레코드별 개별 검증 대신, 요청자가 소유한 `servicePreferenceId`/`serviceOfferingId` 전체 목록을 Internal API로 1회 조회한 뒤 `IN` 조건으로 필터링하는 방식으로 확정 (5.7절 참고) | ✅ 확정 (방식) |
| **ID 목록 반환용 Internal API 스펙** | ✅ 확정 (2026-09-03): care-plan-service(`GET /internal/v1/service-preferences?patientId=`), provider-service(`GET /internal/v1/service-offerings?providerId=`) 엔드포인트/Request/Response 스펙 확정 (5.7절 참고) | ✅ 확정 |
| **01번 API의 잘못된 status 필터 처리** | ✅ 확정 (2026-09-03): 허용되지 않는 `status` 값이 들어오면 `400`을 반환하는 것으로 확정 | ✅ 확정 |
| **02번 API의 status 옵션 목록 CHANGED** | ✅ 확정 (2026-09-03): `02_서비스일정상세조회.md`의 Response `status` 옵션 목록에 `CHANGED` 포함 확정 (표에는 이미 있었으나 주석 표기 실수 정정) | ✅ 확정 |

---

## 변경 이력

| 날짜 | 변경 내용 |
| --- | --- |
| 2026-09-02 | 4.1절의 `DELAY` 잔존 표현을 `RESCHEDULING`/`CHANGED`로 정정 (3장과의 불일치 해소) |
| 2026-09-02 | `03_서비스일정변경.md` 최신화 내용 반영 — Care Plan 범위 검증용 Internal API(Schedule-Service → Care-Plan-Service, Feign) 호출을 4.1절 및 신규 5.5절로 추가 |
| 2026-09-02 | 8장 미확정 사항 테이블에 Care Plan 범위 검증 Internal API 관련 신규 확인 필요 항목 추가 |
| 2026-09-03 | `05_서비스수행완료.md` 최신화 반영 — API 목록(6장)의 05번 URL 오기 정정(`/status`→`/complete`), 4.3절에 서비스 제공자 본인 확인 메커니즘 미정 사항 반영, Care Plan Internal API 필드에 `patientId` 반영(03/04번 확정 내용 동기화), 8장에 신규 확인 필요 항목 2건 추가 |
| 2026-09-03 | `05_서비스수행완료.md` 2차 최신화 — 서비스 제공자 본인 확인 메커니즘 확정 반영(provider-service Internal API `GET /internal/v1/service-offerings/{serviceOfferingId}`), 신규 5.6절 추가, 8장 해당 항목을 확정으로 전환 |
| 2026-09-03 | `01_서비스일정목록조회.md` 최신화 반영 — 목록 조회 소유권 검증 방식 불명확, `status` 필터의 `CHANGED` 누락 등 신규 확인 필요 항목 8장에 추가 |
| 2026-09-03 | `01_서비스일정목록조회.md` 2차 최신화 — Notion 원본 갱신(`CHANGED` 추가, `servicePreferenceId`/`serviceOfferingId` 응답 추가) 반영, 잘못된 `status` 필터 시 `400` 확정, 목록 조회 소유권 필터링을 "ID 목록 기반 배치 조회" 방식으로 확정(팀 논의 완료), 신규 5.7절 추가 |
| 2026-09-03 | `01_서비스일정목록조회.md` 3차 최신화 — Notion 원본 오타 정정(퇴원 예정자 조회 시 호출 대상이 "Schedule-Service"로 잘못 기재되어 있던 것을 care-plan-service로 정정), Response의 `servicePreferenceId`/`serviceOfferingId`를 최종적으로 제외하는 것으로 확정(팀 확인 완료, 내부 필터링 전용 ID는 응답에 노출하지 않음). 8장 관련 항목 갱신 |
| 2026-09-03 | ✅ 확정: 5.7절의 ID 목록 반환 Internal API 엔드포인트 스펙 확정 — care-plan-service `GET /internal/v1/service-preferences?patientId=`, provider-service `GET /internal/v1/service-offerings?providerId=`. 마지막 블로커 해소, 8장 해당 항목 확정으로 전환 |
| 2026-09-03 | `02_서비스일정상세조회.md` 최신화 반영 — JSON 예시 문법 오류 수정, 비즈니스 규칙 구조화, 03/04/05번의 기존 Internal API(점검증)를 재사용하는 방식임을 명시. `status` 옵션 목록의 `CHANGED` 누락을 8장에 신규 확인 필요로 추가 |
| 2026-09-03 | ✅ 확정: `02_서비스일정상세조회.md`의 `status` 옵션에 `CHANGED` 포함 확정 (표기 실수 정정). 8장 해당 항목 확정으로 전환 |