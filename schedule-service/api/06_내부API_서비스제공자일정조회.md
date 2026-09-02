# 06. [내부 API] 서비스 제공자 일정 조회

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/internal/v1/service-schedules` |
| 호출 주체 | Provider-Service (서비스 간 내부 호출) |
| 카테고리 | 검색 |
| 관련 테이블 | `p_service_schedules` |

> ℹ️ 2026-09-01 최신 Notion 페이지 기준으로 재갱신 (Batch 조회 전략 도입, 조회 대상 상태 확장, 쿼리 파라미터 변경, **`X-Internal-Api-Key` 검증을 Interceptor에서 수행하도록 확정**).
>

## 설명

Provider-Service가 매칭 가능한 Provider를 조회할 때, 해당 Provider의 후보 제공 항목(`serviceOfferingId`)에 대해 **특정 기간 동안 이미 확정된 서비스 일정이 존재하는지 확인**하기 위해 Schedule-Service를 호출하는 내부 API다.

Provider-Service는 이 API로 조회된 기존 일정과 Provider의 제공 가능 요일/시간대(`p_provide_works`)를 비교하여 실제 매칭 가능한 일정을 자체적으로 판단한다.

**인증**: `X-Internal-Api-Key` 헤더 사용. 각 서비스가 환경변수로 보유한 Key와 대조하여 검증한다 (API Gateway 미경유).

> ✅ **확정 (2026-09-01)**: 검증은 **Interceptor**에서 수행하며, Controller는 이 커스텀 헤더를 직접 처리하지 않는다. (`docs/코드_컨벤션_구현용.md`의 "인증 헤더 처리 코드 미구현" 확인 필요 항목 중 구현 방식이 이걸로 해소됨 — 단, `global/security` 패키지 및 실제 Interceptor 클래스는 아직 코드에 없어 신규 구현 필요.)
>

### 설계 근거

- 서비스별 DB가 분리되어 있으며 서비스 간 데이터는 논리 FK로만 연결되어 있어, Provider-Service와 Schedule-Service 간 직접 SQL JOIN이 불가능하다.
- `p_provide_works`(Provider-Service)는 제공 가능 요일/시간대만 관리하고, 실제 예약된 일정은 `p_service_schedules`(Schedule-Service)에서 관리한다.
- 따라서 매칭 가능 여부 판단을 위해 동기 내부 API 호출로 일정 정보를 조회한다.
- `X-Internal-Api-Key`의 검증은 Interceptor에서 수행하며, Controller에서는 해당 커스텀 헤더를 직접 검증하지 않는다.

### 조회 대상 상태

> ⚠️ **변경**: 기존에는 `SCHEDULED`만 반환했으나, `RESCHEDULING`도 포함하도록 확장됨.
>

| 상태 | 반환 여부 | 사유 |
| --- | --- | --- |
| `SCHEDULED` | ✅ 반환 | Provider가 배정되어 확정된 일정 |
| `RESCHEDULING` | ✅ 반환 | 일정 변경에 따라 새 Provider 매칭이 진행 중인 일정 |
| `CHANGED`, `COMPLETED`, `NO_SHOW`, `CANCELED` | ❌ 제외 | Provider의 향후 일정과 충돌하지 않음 |

두 상태(`SCHEDULED`, `RESCHEDULING`)의 일정은 해당 시간대를 **예약 불가능한 시간**으로 간주한다.

### Batch 조회 전략

> ⚠️ **신규 도입**: 개별 일정마다 반복 호출하지 않고, Care Plan 전체 일정 범위를 한 번에 조회하는 방식으로 변경됨.
>

Care Plan의 서비스 일정은 최대 30일 범위로 구성되므로, `startDate`부터 **30일간**(고정값, ✅ 2026-09-01 확정)의 `SCHEDULED`/`RESCHEDULING` 상태 일정을 한 번에 반환한다.

```
startDate = 2026-09-01
조회 범위 = 2026-09-01 ~ 2026-09-30
```

> ✅ **확정 (2026-09-01)**: 조회 기간은 **30일 고정값**이다 (Care Plan마다 가변적이지 않음). 구현 시 상수(`BATCH_QUERY_DAYS = 30`)로 정의한다.
>

## Request

> ℹ️ **변경**: `X-Internal-Api-Key`는 Interceptor 단에서 검증되고 Controller/DTO 레벨에서는 다루지 않으므로, 최신 Notion 문서 기준 아래 Request 표에서 제외했다. (헤더 자체는 여전히 필수이며, HTTP 요청에는 포함되어야 한다 — 다만 Controller의 Request 스펙에는 나타나지 않는다.)
>

| key | 설명 | 타입 | 위치 | 제약사항 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- | --- |
| serviceOfferingIds | 조회할 서비스 제공 항목 ID 목록 | List<UUID> | Query Parameter | 콤마(`,`) 구분, 1개 이상 | X | d3e4f5a6-1234-5678-9abc-def012345678 |
| startDate | 조회 시작 날짜 (여기서 30일간 조회) | LocalDate | Query Parameter | 조회 시작일, 생략 불가 | X | 2026-09-01 |

> ⚠️ **변경**: 기존 `from`/`to`(선택 입력, 범위 지정) 방식에서 `startDate`(필수, 시작일만 지정, 30일 고정 조회) 방식으로 변경됨.
>

## Response

| key | 설명 | 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| serviceScheduleId | 서비스 일정 ID | UUID | - | X | 3fa85f64-5717-4562-b3fc-2c963f66afa6 |
| serviceOfferingId | 서비스 제공 항목 ID (✅ 신규 추가) | UUID | - | X | d3e4f5a6-1234-5678-9abc-def012345678 |
| date | 일정 날짜 | LocalDate | - | X | 2026-09-01 |
| startedAt | 시작 시각 | LocalDateTime | - | X | 2026-09-01T09:00:00 |
| finishedAt | 종료 시각 | LocalDateTime | - | X | 2026-09-01T10:00:00 |
| status | 서비스 일정 상태 (✅ 신규 추가) | String(ENUM) | SCHEDULED, RESCHEDULING | X | `SCHEDULED` |

### Example

```json
{
  "success": true,
  "code": 200,
  "message": "서비스 제공자 일정 조회 성공",
  "data": {
    "content": [
      {
        "serviceScheduleId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "serviceOfferingId": "d3e4f5a6-1234-5678-9abc-def012345678",
        "date": "2026-09-01",
        "startedAt": "2026-09-01T09:00:00",
        "finishedAt": "2026-09-01T10:00:00",
        "status": "SCHEDULED"
      }
    ]
  }
}
```

## Status

| HTTP Status | 설명 |
| --- | --- |
| 200 | 조회 성공 (일정이 없으면 빈 배열 반환) |
| 400 | Query Parameter 형식 또는 Validation 오류 (✅ 신규 추가) |
| 401 | `X-Internal-Api-Key`가 없거나 유효하지 않음 (✅ 신규 추가) |