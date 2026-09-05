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

> ⚠️ 확인 필요: 요청자 역할 — 원본 스펙 Request 표의 `X-User-Role` 예시값이 `SERVICE_PROVIDER`로 되어 있으나, 설명("퇴원예정자가 등록한 희망
> 일정")과 `01_서비스 희망 일정 수정.md`(동일 자원, 요청자 `PATIENT`로 확정)를 볼 때 `01` 문서와 동일하게 원본 스펙의 오기로 보인다.
> `PATIENT`로 확정하는 것을 제안하며, 팀 확인 후 반영한다.
>
> ⚠️ 확인 필요: Care Plan 상태(`UNDER_REVIEW`) 제약 여부 — 원본 스펙의 Status 표에는 "Care Plan이 `UNDER_REVIEW`가 아닌 경우"에 대한 케이스가
> 없다. 그러나 `01_서비스 희망 일정 수정.md`(수정), `03_CarePlanService신청취소.md`(취소)는 모두 동일 계열 API로서 Care Plan이
> `UNDER_REVIEW`일 때만 허용되도록 확정되어 있고, `care-plan-service.md` 3장에도 "희망 일정 등록/수정은 `UNDER_REVIEW`일 때만 가능"으로
> 명시되어 있다. 삭제 API만 이 제약에서 예외인지, 아니면 원본 스펙 작성 시 누락된 것인지 팀 확인이 필요하다.
>
> ⚠️ 확인 필요: **404(`CARE_PLAN_SERVICE_NOT_FOUND`) 에러 코드** — 원본 스펙 Example의 실패 응답이 `CARE_PLAN_SERVICE_NOT_FOUND`를
> 사용하고 있으나, 조회 대상은 `servicePreferenceId`(희망 일정)이지 `planServiceId`(Care Plan 서비스)가 아니다. `01_서비스 희망 일정
> 수정.md`에서 동일한 사유로 `CARE_PLAN_SERVICE_NOT_FOUND` 재사용을 명시적으로 배제하고 `SERVICE_PREFERENCE_NOT_FOUND`를 신규 등록했던
> 전례가 있어, 이번에도 기존에 등록된 `SERVICE_PREFERENCE_NOT_FOUND`(404)를 재사용하는 것을 제안한다. 원본 스펙의 코드명은 `03` 문서의
> 내용을 잘못 옮겨온 것으로 추정된다.
>
> ⚠️ 확인 필요: **409(`이미 삭제 됨`) 에러 코드명** — 현재 `ErrorCode`에 "이미 논리삭제된 희망 일정을 다시 삭제 시도"에 대응하는 코드가 없어 신규
> 등록이 필요하다. `SERVICE_PREFERENCE_ALREADY_DELETED`(409)로 제안하며(기존 네이밍 컨벤션상 `{도메인}_{상태}` 형태를 따름, `03` 문서의
> `CARE_PLAN_SERVICE_ALREADY_DELETED` 제안과 동일한 방식), 확정 시 반영 예정이다.

## Request

| key                 | 설명      | value 타입     | 위치            | 제약사항                            | Nullable | 예시                                     |
|---------------------|---------|--------------|---------------|---------------------------------|----------|-----------------------------------------|
| X-User-Id           | 요청자 Id  | UUID         | Header        | -                                | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| servicePreferenceId | 희망일정 ID | UUID         | Path Variable | -                                | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| X-User-Role         | 요청자 권한  | String(ENUM) | Header        | Gateway가 인증 후 주입, 필수. `PATIENT`만 허용 | X        | `PATIENT`                              |

## Response

없음 (`204 No Content`)

**Example**

```javascript
// 성공 (204, 본문 없음)

// 실패
{
    "success": false,
    "code": "SERVICE_PREFERENCE_NOT_FOUND",
    "message": "Care Plan 희망 일정 삭제 실패",
    "details": {
        "reason": "servicePreferenceId에 해당하는 희망 일정이 존재하지 않습니다."
    },
    "timestamp": "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content            |
|--------|------------------------------|
| `204`  | 희망 일정 삭제 성공                  |
| `403`  | 권한 없음                        |
| `404`  | 존재하지 않는 `servicePreferenceId` |
| `409`  | 이미 삭제 됨                      |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                       |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. 요청자 역할(`X-User-Role` 예시값), Care Plan `UNDER_REVIEW` 상태 제약 여부, 404 에러 코드(`CARE_PLAN_SERVICE_NOT_FOUND` vs `SERVICE_PREFERENCE_NOT_FOUND`), 409 에러 코드 신설 여부가 원본 스펙에 명확히 명시되어 있지 않거나 다른 문서와 불일치하여 ⚠️ 확인 필요로 표시 |
