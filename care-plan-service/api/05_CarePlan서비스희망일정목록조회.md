# 05. Care Plan 서비스 희망 일정 목록 조회

| 항목     | 내용                                                |
|--------|---------------------------------------------------|
| Method | `GET`                                              |
| URL    | `/api/v1/care-plans/{carePlanId}/service-preferences` |
| 사용자    | 병원 담당자, 사회복지사, 퇴원예정자                                |
| 카테고리   | 조회                                                |
| 테이블명   | `p_care_plan_service_preferences`                  |

## 설명

해당 Care Plan(`carePlanId`)에 등록된 서비스 희망 일정(`CarePlanServicePreference`) 목록을 조회한다. 희망 서비스의 종류(`provideServiceId`),
희망 날짜, 희망 시간대를 희망 날짜(`preferredDate`)가 빠른 순서(오름차순)로 페이지네이션하여 반환하며, `preferredDate`로 필터링할 수 있다.

## 비즈니스 규칙

- 정렬 기준은 `preferredDate` 오름차순(빠른 날짜부터)으로 고정이다.
- `preferredDate` 쿼리 파라미터를 전달하면 해당 날짜의 희망 일정만 필터링하여 조회한다(생략 시 전체 조회).
- `size`는 `10`/`30`/`50`만 허용하며, 그 외 값이 들어오면 `10`으로 자동 대체한다.
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.

> ✅ 확정 (2026-09-05, 팀 확인 완료):
> - 요청자는 `HOSPITAL_STAFF`, `SOCIAL_WORKER`, `PATIENT` 세 역할을 허용한다 (Request 표의 `X-User-Role` 예시값 `SERVICE_PROVIDER`는
>   원본 스펙의 오기였음).
> - `PATIENT`는 본인 소유 Care Plan(`patientId == X-User-Id`)만 조회할 수 있다 (`AUTH_FORBIDDEN`, 403). `HOSPITAL_STAFF`/
>   `SOCIAL_WORKER`는 현재 별도의 소유권 검증 조건이 정의되어 있지 않으므로, 역할 검증(`@PreAuthorize`)만 통과하면 조회를 허용한다.
> - `page`가 음수여도 예외를 던지지 않고, 다른 API와 동일한 공통 페이지네이션 정책(`PageableFactory`)에 따라 `0`으로 보정한다. `size`도
>   `10`/`30`/`50`만 허용하고 그 외 값은 `10`으로 보정한다 — 기존 `PageableFactory`를 그대로 재사용한다 (`CarePlanQueryService.searchCarePlan()`
>   처럼 `page < 0`에서 별도 예외(`CARE_PLAN_BAD_REQUEST`)를 던지는 방식은 채택하지 않는다).
> - 404 에러 코드는 원본 스펙의 `CARE_PLAN_SERVICE_NOT_FOUND` 대신 기존에 등록된 `CARE_PLAN_NOT_FOUND`를 재사용한다 (조회 실패 대상이
>   `carePlanId`이기 때문).

## Request

| key            | 설명       | value 타입     | 위치              | 제약사항                                          | Nullable | 예시                                     |
|----------------|----------|--------------|-----------------|-----------------------------------------------|----------|-----------------------------------------|
| X-User-Id      | 요청자 Id   | UUID         | Header          | -                                              | X        | `5fa85f64-5717-4562-b3fc-2c963f66afa6` |
| carePlanId     | Care Plan ID | UUID     | Path Variable   | -                                              | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| page           | 페이지 번호   | Integer      | Query Parameter | 음수 불가능                                        | O        | `0`                                     |
| size           | 페이지 크기   | Integer      | Query Parameter | `10`, `30`, `50`만 가능하며 기본값은 `10`. 그 외 값이면 `10`으로 자동 변경 | O        | `10`                                    |
| preferredDate  | 희망 날짜    | LocalDate    | Query Parameter | -                                              | O        | `2026-09-10`                            |
| X-User-Role    | 요청자 권한   | String(ENUM) | Header          | Gateway가 인증 후 주입, 필수. `HOSPITAL_STAFF`/`SOCIAL_WORKER`/`PATIENT`만 허용 | X | `PATIENT` |

## Response

| key                 | 설명       | value 타입     | 옵션                  | Nullable | 예시                                     |
|---------------------|----------|--------------|---------------------|----------|-----------------------------------------|
| servicePreferenceId | 희망 일정 ID | UUID         | -                    | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| provideServiceId    | 서비스 종류 ID | UUID         | -                    | X        | `5fa85f64-5717-4562-b3fc-2c963f66afa6` |
| preferredDate       | 희망 날짜    | LocalDate    | -                    | X        | `2026-09-10`                            |
| preferredTimeSlot   | 희망 시간대   | String(ENUM) | `MORNING`, `AFTERNOON` | X        | `MORNING`                                |
| createdAt           | 생성일      | Instant      | -                    | X        | `2026-08-28T03:30:00Z`                  |

목록은 `data.content[]`에 위 필드들을 담고, `data.pageInfo`에 페이지 정보(`paginationType`, `page`, `size`, `totalElements`, `totalPages`)를
담는 공통 페이지네이션 응답 포맷(`PageResponse`/`PageInfo`, `docs/코드_컨벤션_구현용.md` 참고)을 그대로 따른다.

**Example**

```javascript
// 성공
{
    "success": true,
    "code": 200,
    "message": "서비스 희망 일정 목록 조회 성공",
    "data": {
        "content": [
            {
                "servicePreferenceId": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
                "provideServiceId": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
                "preferredDate": "2026-09-10",
                "preferredTimeSlot": "MORNING",
                "createdAt": "2026-08-28T03:30:00Z"
            }
        ],
        "pageInfo": {
            "paginationType": "OFFSET",
            "page": 0,
            "size": 10,
            "totalElements": 1,
            "totalPages": 1
        }
    }
}

// 실패 (404)
{
    "success": false,
    "code": "CARE_PLAN_NOT_FOUND",
    "message": "Care Plan 서비스 희망 일정 목록 조회 실패",
    "details": {
        "reason": "carePlanId에 해당하는 Care Plan이 존재하지 않습니다."
    },
    "timestamp": "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content         |
|--------|--------------------------|
| `200`  | 서비스 희망 일정 목록 조회 성공       |
| `403`  | 허용되지 않는 역할이거나, `PATIENT`가 본인 소유가 아닌 Care Plan을 조회 |
| `404`  | 존재하지 않는 `carePlanId`의 Care Plan |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                 |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. 요청자 역할(`X-User-Role` 예시값), `page` 음수 처리 방식(예외 vs 자동 보정), 404 에러 코드(`CARE_PLAN_SERVICE_NOT_FOUND` vs `CARE_PLAN_NOT_FOUND`), 소유권 검증 여부가 원본 스펙에 명확히 명시되어 있지 않아 ⚠️ 확인 필요로 표시 |
| 2026-09-05 | ✅ 확정 — 요청자는 `HOSPITAL_STAFF`/`SOCIAL_WORKER`/`PATIENT` 허용, `PATIENT`는 본인 소유 Care Plan만 조회 가능(소유권 검증), `HOSPITAL_STAFF`/`SOCIAL_WORKER`는 역할 검증만 적용. `page`/`size`는 기존 `PageableFactory`의 공통 페이지네이션 정책(음수/허용 범위 밖 값은 예외 없이 보정)을 그대로 재사용. 404는 `CARE_PLAN_NOT_FOUND`로 확정 |
