# 04. Care Plan 희망 일정 삭제

| 항목     | 내용                                                  |
|--------|-----------------------------------------------------|
| Method | `DELETE`                                            |
| URL    | `/api/v1/service-preferences/{servicePreferenceId}` |
| 사용자    | 퇴원예정자                                               |
| 카테고리   | 삭제                                                  |
| 테이블명   | `p_care_plan_service_preferences`                   |

## 설명

Care Plan에 포함된 서비스(`CarePlanService`)는 유지한 채, 퇴원 예정자가 등록한 희망 일정(`CarePlanServicePreference`) 중 특정 일정만 논리삭제한다.

여러 개의 희망 일정이 등록된 경우, 더 이상 희망하지 않는 특정 날짜 또는 시간대의 일정만 삭제할 때 사용한다. 서비스 신청 자체를 취소하는 경우에는
`03_CarePlanService신청취소.md`(서비스 신청 취소 API)를 사용한다.

## 비즈니스 규칙

- 본인(퇴원 예정자) 소유 Care Plan에 속한 희망 일정만 삭제할 수 있다.
- 이미 논리삭제된 희망 일정을 다시 삭제하려고 하면 거부한다 (`409`).
- 삭제 대상은 희망 일정(`CarePlanServicePreference`) 하나이며, 상위 `CarePlanService`/`CarePlan`은 삭제되지 않는다.
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.

> ✅ 확정 (2026-09-05, 팀 확인 완료):
> - 요청자는 퇴원 예정자(`PATIENT`)만 가능하다 (`X-User-Role` 예시값 `SERVICE_PROVIDER`는 원본 스펙의 오기였음).
> - Care Plan이 `UNDER_REVIEW` 상태일 때만 삭제 가능하다 (`01`/`03` 문서와 동일 기준).
> - 404 에러 코드는 `CARE_PLAN_SERVICE_NOT_FOUND`를 재사용하지 않고 기존에 등록된 `SERVICE_PREFERENCE_NOT_FOUND`를 재사용한다
>   (조회 대상이 `servicePreferenceId`이기 때문 — 원본 스펙 Example의 `CARE_PLAN_SERVICE_NOT_FOUND`는 `03` 문서 내용이 잘못 옮겨진 것으로
>   확인됨).
> - 이미 논리삭제된 희망 일정을 다시 삭제 시도하면 409(`SERVICE_PREFERENCE_ALREADY_DELETED`, 신규 등록)를 반환한다. 삭제 여부와 무관하게
>   조회 가능한 별도 리포지토리 메서드(`findByIdIncludingDeleted`)를 추가해 404와 409를 구분한다.
> - Care Plan이 `UNDER_REVIEW`가 아닐 때는 기존 `SERVICE_PREFERENCE_NOT_ALLOWED`를 재사용하지 않고, 삭제 전용 신규 코드
>   `SERVICE_PREFERENCE_DELETE_NOT_ALLOWED`(409)를 등록해 사용한다 (`SERVICE_PREFERENCE_NOT_ALLOWED`의 고정 메시지가 "등록할 수
>   없습니다"로 되어 있어 삭제 상황과 맞지 않음).

## Request

| key                 | 설명      | value 타입     | 위치            | 제약사항                                | Nullable | 예시                                     |
|---------------------|---------|--------------|---------------|-------------------------------------|----------|----------------------------------------|
| X-User-Id           | 요청자 Id  | UUID         | Header        | -                                   | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| servicePreferenceId | 희망일정 ID | UUID         | Path Variable | -                                   | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| X-User-Role         | 요청자 권한  | String(ENUM) | Header        | Gateway가 인증 후 주입, 필수. `PATIENT`만 허용 | X        | `PATIENT`                              |

## Response

없음 (`204 No Content`)

**Example**

```javascript
// 성공 (204, 본문 없음)

// 실패
{
    "success"
:
    false,
        "code"
:
    "SERVICE_PREFERENCE_NOT_FOUND",
        "message"
:
    "Care Plan 희망 일정 삭제 실패",
        "details"
:
    {
        "reason"
    :
        "servicePreferenceId에 해당하는 희망 일정이 존재하지 않습니다."
    }
,
    "timestamp"
:
    "2026-08-28T03:30:00Z"
}

// 실패 (409, 이미 삭제됨)
{
  "success": false,
  "code": "SERVICE_PREFERENCE_ALREADY_DELETED",
  "message": "Care Plan 희망 일정 삭제 실패",
  "details": {
    "reason": "이미 삭제된 희망 일정입니다."
  },
  "timestamp": "2026-08-28T03:30:00Z"
}

// 실패 (409, Care Plan이 UNDER_REVIEW 아님)
{
  "success": false,
  "code": "SERVICE_PREFERENCE_DELETE_NOT_ALLOWED",
  "message": "Care Plan 희망 일정 삭제 실패",
  "details": {
    "reason": "현재 Care Plan 상태에서는 희망 일정을 삭제할 수 없습니다."
  },
  "timestamp": "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content              |
|--------|-------------------------------|
| `204`  | 희망 일정 삭제 성공                   |
| `403`  | 권한 없음                         |
| `404`  | 존재하지 않는 `servicePreferenceId` |
| `409`  | 이미 삭제 됨 또는 Care Plan이 `UNDER_REVIEW` 상태가 아님 |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                                                                |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. 요청자 역할(`X-User-Role` 예시값), Care Plan `UNDER_REVIEW` 상태 제약 여부, 404 에러 코드(`CARE_PLAN_SERVICE_NOT_FOUND` vs `SERVICE_PREFERENCE_NOT_FOUND`), 409 에러 코드 신설 여부가 원본 스펙에 명확히 명시되어 있지 않거나 다른 문서와 불일치하여 ⚠️ 확인 필요로 표시 |
| 2026-09-05 | ✅ 확정 및 구현 완료 — 요청자는 `PATIENT`만 허용, Care Plan `UNDER_REVIEW`일 때만 삭제 가능. 404는 기존 `SERVICE_PREFERENCE_NOT_FOUND` 재사용, 409는 신규 등록한 `SERVICE_PREFERENCE_ALREADY_DELETED`(이미 삭제됨)/`SERVICE_PREFERENCE_DELETE_NOT_ALLOWED`(UNDER_REVIEW 아님) 두 코드로 구분(`SERVICE_PREFERENCE_NOT_ALLOWED`는 메시지가 "등록할 수 없습니다"로 고정돼 있어 재사용하지 않음). 삭제 여부와 무관하게 조회하는 `ServicePreferenceCommandRepository.findByIdIncludingDeleted()` 신설, `ServicePreferenceCommandService.deleteServicePreference()`, `DELETE /api/v1/service-preferences/{servicePreferenceId}` 엔드포인트 추가 |
