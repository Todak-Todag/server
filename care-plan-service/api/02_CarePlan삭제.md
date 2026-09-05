# 02. CarePlan 삭제

| 항목     | 내용                              |
|--------|---------------------------------|
| Method | `DELETE`                        |
| URL    | `/api/v1/care-plans/{carePlanId}` |
| 사용자    | 병원 담당자, ADMIN, MASTER            |
| 카테고리   | 삭제                               |
| 테이블명   | `p_care_plans`                  |

## 설명

확정 전 퇴원 취소 또는 잘못 등록된 Care Plan에 대해 논리삭제를 수행한다.

## 비즈니스 규칙

- `UNDER_REVIEW` 상태에서만 삭제할 수 있으며, `CONFIRMED`/`IN_PROGRESS`/`COMPLETED` 상태에서는 삭제할 수 없다 (`UNDER_REVIEW`가 아니면 모두 거부).
- 삭제 시 해당 Care Plan에 연결된 하위 리소스도 함께 논리삭제한다. 삭제 순서는 `p_care_plan_service_preferences` → `p_care_plan_services` → `p_care_plans` 순이다 (자식 리소스부터 먼저 삭제).
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.
- 삭제 권한은 역할(`role`) 기준으로만 판단하며(`HOSPITAL_STAFF`/`ADMIN`/`MASTER`), `patientId` 등 별도의 소유권 검증은 적용하지 않는다.

> ✅ 확정 (2026-09-05, 팀 확인 완료):
> - Method/URL은 `DELETE /api/v1/care-plans/{carePlanId}`로 확정한다.
> - 요청자는 `HOSPITAL_STAFF`, `ADMIN`, `MASTER`만 가능하다 (`PATIENT`, `SERVICE_PROVIDER`는 Care Plan 자체 삭제 권한 없음. `X-User-Role` 예시값 `SERVICE_PROVIDER`는 원본 스펙의 오기였음).
> - 소유권 검증은 별도로 두지 않고 역할 기준으로만 제한한다 (병원 소속 단위 등 추가 검증 없음).
> - `UNDER_REVIEW`가 아닐 때(`CONFIRMED`/`IN_PROGRESS`/`COMPLETED`)는 `409 Conflict`(`CARE_PLAN_DELETE_NOT_ALLOWED`, 신규 등록)를 반환한다.

## Request

| key         | 설명            | value 타입    | 위치            | 제약사항                | Nullable | 예시                                       |
|-------------|---------------|-------------|---------------|---------------------|----------|------------------------------------------|
| carePlanId  | 삭제할 Care Plan Id | UUID        | Path Variable | -                   | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6`   |
| X-User-Id   | 요청자 Id        | UUID        | Header        | -                   | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6`   |
| X-User-Role | 요청자 권한        | String(ENUM) | Header        | Gateway가 인증 후 주입, 필수. `HOSPITAL_STAFF`/`ADMIN`/`MASTER`만 허용 | X        | `HOSPITAL_STAFF`                       |

## Response

없음 (`204 No Content`)

**Example**

```javascript
// 성공 (204, 본문 없음)

// 실패
{
  "success": false,
  "code": "CARE_PLAN_NOT_FOUND",
  "message": "Care Plan 삭제 실패",
  "details": {
    "reason": "carePlanId에 해당하는 Care Plan이 존재하지 않습니다."
  },
  "timestamp": "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content        |
|--------|--------------------------|
| `204`  | 삭제 성공 (No Content)       |
| `403`  | 권한 없음                    |
| `404`  | 존재하지 않는 `carePlanId`     |
| `409`  | `UNDER_REVIEW` 상태가 아닌 Care Plan을 삭제 시도 |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                          |
|------------|----------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. Method/URL, `X-User-Role`/사용자 값, `UNDER_REVIEW`가 아닐 때의 응답 코드(409 누락 추정) 등은 원본 스펙에 명시되어 있지 않아 ⚠️ 확인 필요로 표시 |
| 2026-09-05 | ✅ 확정 및 구현 완료 — 요청자는 `HOSPITAL_STAFF`/`ADMIN`/`MASTER`만 허용(소유권 검증 없음), Method/URL `DELETE /api/v1/care-plans/{carePlanId}` 확정, `UNDER_REVIEW`가 아니면 409(`CARE_PLAN_DELETE_NOT_ALLOWED`, 신규) 반환, 하위 리소스는 `p_care_plan_service_preferences` → `p_care_plan_services` → `p_care_plans` 순으로 논리삭제. Status 표/비즈니스 규칙에 반영 |
