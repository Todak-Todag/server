# 06. [내부 API] 서비스 제공자 일정 조회

| 항목 | 내용 |
| --- | --- |
| Method | `GET` |
| URL | `/internal/v1/service-schedules` |
| 호출 주체 | Provider-Service (서비스 간 내부 호출) |
| 카테고리 | 검색 |
| 관련 테이블 | `p_service_schedules` |

## 설명

Provider-Service가 매칭 가능한 Provider를 조회할 때, 해당 Provider의 후보 제공 항목(`serviceOfferingId`)에 대해 **특정 기간 동안 이미 확정된 서비스 일정이 존재하는지 확인**하기 위해 Schedule-Service를 호출하는 내부 API다.

Provider-Service는 이 API로 조회된 기존 일정과 Provider의 제공 가능 요일/시간대(`p_provide_works`)를 비교하여 실제 매칭 가능한 일정을 자체적으로 판단한다.

**인증**: `X-Internal-Api-Key` 헤더 사용 (API Gateway 미경유). 검증은 `InternalResponseInterceptor`가 `/internal/v1/**` 전체에 대해 수행하며, Controller는 이 헤더를 직접 처리하지 않는다. 키는 `internal.key` 설정값과 상수 시간 비교(`MessageDigest.isEqual`)하며, 없거나 불일치하면 `401 UNAUTHORIZED_INTERNAL_REQUEST`.

### 설계 근거

- 서비스별 DB가 분리되어 있고 서비스 간 데이터는 논리 FK로만 연결되어 있어, Provider-Service와 Schedule-Service 간 직접 SQL JOIN이 불가능하다.
- `p_provide_works`(Provider-Service)는 제공 가능 요일/시간대만 관리하고, 실제 예약된 일정은 `p_service_schedules`(Schedule-Service)가 관리한다.
- 이 API는 "가능/불가능"을 판단하지 않고 기존 일정 목록을 그대로 반환한다. **시간대 겹침 판단은 Provider-Service의 책임이다.**

### 조회 대상 상태

| 상태 | 반환 여부 | 사유 |
| --- | --- | --- |
| `SCHEDULED` | ✅ 반환 | Provider가 배정되어 확정된 일정 |
| `RESCHEDULING` | ✅ 반환 | 일정 변경에 따라 새 Provider 매칭이 진행 중인 일정 |
| `CHANGED`, `COMPLETED`, `NO_SHOW`, `CANCELED` | ❌ 제외 | Provider의 향후 일정과 충돌하지 않음 |

두 상태(`SCHEDULED`, `RESCHEDULING`)의 일정은 해당 시간대를 **예약 불가능한 시간**으로 간주한다. 논리 삭제(`deleted_at IS NOT NULL`)된 일정도 제외한다.

### Batch 조회 전략

개별 일정마다 반복 호출하지 않고, Care Plan 전체 일정 범위를 한 번에 조회한다. `startDate`부터 **30일간**(`BATCH_QUERY_DAYS = 30` 고정) 일정을 반환하며, 조회 범위는 `startDate ~ startDate + 29일`(양끝 포함)이다.

```
startDate = 2026-09-01
조회 범위 = 2026-09-01 ~ 2026-09-30
```

### 호출 측 구현 확인

provider-service의 `ScheduleClient`가 `serviceOfferingIds`(콤마 구분)와 `startDate`(ISO DATE 명시)로 정확히 호출하며, 응답 DTO도 6개 필드를 그대로 받는다. 계약 불일치는 없다.

> ⚠️ **확인 필요 (신규)**: provider-service는 이 API를 **두 가지 용도**로 쓴다.
>
> 1. **매칭 판정** — `startDate`를 Care Plan 희망 일정 중 가장 이른 날짜로 넘긴다. 30일 창 설계 의도와 일치한다.
> 2. **제공 서비스 삭제 / 제공 가능 일정 수정 가드**(`existsConfirmedSchedule`) — `startDate = 오늘`로 호출해 결과가 비어 있지 않으면 차단한다. 이 경우 **오늘로부터 30일을 넘어가는 일정은 조회되지 않아** 가드를 통과할 수 있다. Care Plan은 퇴원일 다음 날부터 시작하므로, 퇴원 예정일이 먼 케어플랜의 일정이 여기에 해당한다. 30일 고정 창을 유지할지, 가드용 조회를 별도로 둘지 확인이 필요하다.

## Request

> `X-Internal-Api-Key`는 Interceptor 단에서 검증되고 Controller/DTO 레벨에서는 다루지 않으므로 아래 Request 표에서 제외했다. (헤더 자체는 여전히 필수이며 HTTP 요청에는 포함되어야 한다.)
>

| key | 설명 | 타입 | 위치 | 제약사항 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- | --- |
| serviceOfferingIds | 조회할 서비스 제공 항목 ID 목록 | List\<UUID\> | Query Parameter | 콤마(`,`) 구분, `@NotEmpty`(1개 이상) | X | `d3e4f5a6-1234-5678-9abc-def012345678` |
| startDate | 조회 시작 날짜 (여기서 30일간 조회) | LocalDate | Query Parameter | `yyyy-MM-dd`(ISO DATE), 생략 불가 | X | `2026-09-01` |

## Response

응답은 페이지네이션 없이 `data.content` 배열로 전체를 반환한다.

| key | 설명 | 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| serviceScheduleId | 서비스 일정 ID | UUID | - | X | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| serviceOfferingId | 서비스 제공 항목 ID | UUID | - | X | `d3e4f5a6-1234-5678-9abc-def012345678` |
| date | 일정 날짜 | LocalDate | - | X | `2026-09-01` |
| startedAt | 시작 시각 | LocalDateTime | - | X | `2026-09-01T09:00:00` |
| finishedAt | 종료 시각 | LocalDateTime | - | X | `2026-09-01T10:00:00` |
| status | 서비스 일정 상태 | String(ENUM) | `SCHEDULED`, `RESCHEDULING` | X | `SCHEDULED` |

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

| HTTP Status | ErrorCode | 설명 |
| --- | --- | --- |
| `200` | - | 조회 성공 (일정이 없으면 빈 배열 반환) |
| `400` | `INVALID_PARAMETER` | `serviceOfferingIds` 누락/빈 값, `startDate` 누락/형식 오류 |
| `401` | `UNAUTHORIZED_INTERNAL_REQUEST` | `X-Internal-Api-Key`가 없거나 유효하지 않음 |
