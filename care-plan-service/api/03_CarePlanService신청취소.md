# 03. CarePlanService 신청 취소

| 항목     | 내용                                           |
|--------|----------------------------------------------|
| Method | `DELETE`                                     |
| URL    | `/api/v1/care-plan-services/{planServiceId}` |
| 사용자    | 퇴원예정자                                        |
| 카테고리   | 삭제                                           |
| 테이블명   | `p_care_plan_services`                       |

## 설명

Care Plan에 포함된 확정 서비스 항목(`CarePlanService`)을 논리삭제(Soft Delete)한다.

## 비즈니스 규칙

- 해당 `planServiceId`가 속한 Care Plan의 `status`가 `UNDER_REVIEW`인 경우에만 취소할 수 있다.
- 본인(퇴원 예정자) 소유 Care Plan에 속한 서비스 항목만 취소할 수 있다.
- 취소 시 해당 서비스 항목에 연결된 하위 리소스(`p_care_plan_service_preferences`, 서비스 희망 일정)도 함께 논리삭제한다.
- 이미 취소(삭제)된 서비스 항목을 다시 취소하려고 하면 거부한다 (`409`).
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.

> ✅ 확정 (2026-09-05, 팀 확인 완료):
> - Care Plan이 `UNDER_REVIEW` 상태일 때만 취소 가능하다 (`01_서비스 희망 일정 수정.md`와 동일 기준).
> - 취소 시 하위 희망 일정(`CarePlanServicePreference`)도 함께 논리삭제한다 (cascade).
>
> ⚠️ 확인 필요: **409(`이미 삭제 됨`) 에러 코드명** — 현재 `ErrorCode`에 "이미 논리삭제된 리소스를 다시 삭제 시도"에 대응하는 코드가 없어 신규 등록이 필요하다.
`CARE_PLAN_SERVICE_ALREADY_DELETED`(409)로 제안하며(기존 네이밍 컨벤션상 `{도메인}_{상태}` 형태를 따름), 확정 시 반영 예정이다.
>
> 참고: 404 에러 코드는 `CARE_PLAN_SERVICE_NOT_FOUND`를 그대로 재사용한다 — 현재 코드에 이미 존재하며 의미도 정확히 일치한다 (`planServiceId`로
`CarePlanService` 조회 실패).

## Request

| key           | 설명        | value 타입     | 위치            | 제약사항                                | Nullable | 예시                                     |
|---------------|-----------|--------------|---------------|-------------------------------------|----------|----------------------------------------|
| X-User-Id     | 요청자 Id    | UUID         | Header        | -                                   | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| planServiceId | 서비스 항목 ID | UUID         | Path Variable | -                                   | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| X-User-Role   | 요청자 권한    | String(ENUM) | Header        | Gateway가 인증 후 주입, 필수. `PATIENT`만 허용 | X        | `PATIENT`                              |

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
    "CARE_PLAN_SERVICE_NOT_FOUND",
        "message"
:
    "Care Plan 서비스 일정 취소 실패",
        "details"
:
    {
        "reason"
    :
        "planServiceId에 해당하는 확정 서비스 항목이 존재하지 않습니다."
    }
,
    "timestamp"
:
    "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content        |
|--------|-------------------------|
| `204`  | 서비스 신청 취소 성공            |
| `403`  | 권한 없음                   |
| `404`  | 존재하지 않는 `planServiceId` |
| `409`  | 이미 삭제 됨                 |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                |
|------------|--------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. Care Plan 상태 제약 여부, 하위 희망 일정 cascade 처리, 409 에러 코드 신설 여부는 원본 스펙에 명시되어 있지 않아 ⚠️ 확인 필요로 표시                    |
| 2026-09-05 | ✅ 확정: Care Plan `UNDER_REVIEW` 상태에서만 취소 가능, 하위 희망 일정 cascade 삭제 확정. 409 에러 코드명은 `CARE_PLAN_SERVICE_ALREADY_DELETED`로 제안 — 최종 확정 대기 중 |
