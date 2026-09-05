# 05. Care Plan 서비스 희망 일정 목록 조회

| 항목     | 내용                                                |
|--------|---------------------------------------------------|
| Method | `GET`                                              |
| URL    | `/api/v1/care-plans/{carePlanId}/service-preferences` |
| 사용자    | 퇴원예정자                                             |
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

> ⚠️ 확인 필요: 요청자 역할 — Request 표의 `X-User-Role` 예시값이 `SERVICE_PROVIDER`로 되어 있다. `01_서비스 희망 일정 수정.md`,
> `04_CarePlan희망일정삭제.md` 등 동일 자원(`CarePlanServicePreference`)을 다루는 다른 API들은 모두 이 예시값이 원본 스펙의 오기였고
> 실제로는 `PATIENT`(퇴원예정자)만 허용되는 것으로 확정된 전례가 있다. 다만 이번 API는 "목록 검색"(퇴원예정자만 허용) 성격과 "상세 조회"(`PATIENT`/
> `ADMIN`/`SOCIAL_WORKER`/`MASTER` 허용, `care-plan-service.md` 4.6절 참고) 성격 중 어느 쪽에 더 가까운지 원본 스펙만으로는 판단할 수 없어,
> `PATIENT` 단독 허용인지 다른 역할도 포함되는지 팀 확인이 필요하다.
>
> ⚠️ 확인 필요: `page` 음수 처리 방식 — Request 표에는 `page`의 제약사항이 "음수 불가능"으로 명시돼 있으나, Status 표에는 이에 대응하는 `400`/`409`
> 등의 실패 케이스가 없다. 기존 코드 기준으로는 두 가지 서로 다른 처리 방식이 이미 공존한다: (1) `CarePlanQueryService.searchCarePlan()`(목록
> 검색)처럼 `page < 0`이면 `CARE_PLAN_BAD_REQUEST` 예외를 명시적으로 던지는 방식, (2) `PageableFactory.resolvePage()`처럼 음수 `page`를
> 예외 없이 조용히 `0`으로 보정하는 방식(`size`가 허용 범위를 벗어났을 때와 동일한 처리). Status 표에 실패 케이스가 없다는 점은 (2)를 시사하지만,
> Request 표의 "음수 불가능"이라는 문구는 (1)에 가까워 보여 상충한다. 어느 쪽을 따를지 확인이 필요하다.
>
> ⚠️ 확인 필요: **404(`CARE_PLAN_SERVICE_NOT_FOUND`) 에러 코드** — 원본 스펙 Example의 실패 응답이 `CARE_PLAN_SERVICE_NOT_FOUND`를
> 사용하고 있으나, 조회 실패 대상은 Path Variable인 `carePlanId`(Care Plan)이지 `planServiceId`(Care Plan 서비스)가 아니다. `01`/`04`
> 문서에서 동일한 사유로 자원에 맞는 코드를 재사용했던 전례에 따라, 이번에도 기존에 등록된 `CARE_PLAN_NOT_FOUND`(404)를 재사용하는 것을
> 제안한다.
>
> ⚠️ 확인 필요: 소유권 검증 여부 — Status 표에 `403`(권한 없음)이 있으나 발생 조건이 명시돼 있지 않다. `care-plan-service.md` 4.6절의 "상세
> 조회"처럼 조회된 Care Plan의 `patientId`와 요청자 `userId`가 다르면 `AUTH_FORBIDDEN`(403)을 반환하는 소유권 검증(`CarePlanOwnerValidator`
> 등)이 필요한지, 아니면 역할 기반 필터링만으로 403이 발생하는 구조인지 확인이 필요하다.

## Request

| key            | 설명       | value 타입     | 위치              | 제약사항                                          | Nullable | 예시                                     |
|----------------|----------|--------------|-----------------|-----------------------------------------------|----------|-----------------------------------------|
| X-User-Id      | 요청자 Id   | UUID         | Header          | -                                              | X        | `5fa85f64-5717-4562-b3fc-2c963f66afa6` |
| carePlanId     | Care Plan ID | UUID     | Path Variable   | -                                              | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| page           | 페이지 번호   | Integer      | Query Parameter | 음수 불가능                                        | O        | `0`                                     |
| size           | 페이지 크기   | Integer      | Query Parameter | `10`, `30`, `50`만 가능하며 기본값은 `10`. 그 외 값이면 `10`으로 자동 변경 | O        | `10`                                    |
| preferredDate  | 희망 날짜    | LocalDate    | Query Parameter | -                                              | O        | `2026-09-10`                            |
| X-User-Role    | 요청자 권한   | String(ENUM) | Header          | Gateway가 인증 후 주입, 필수                          | X        | `PATIENT`                               |

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
| `403`  | 권한 없음                    |
| `404`  | 존재하지 않는 `carePlanId`의 Care Plan |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                 |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. 요청자 역할(`X-User-Role` 예시값), `page` 음수 처리 방식(예외 vs 자동 보정), 404 에러 코드(`CARE_PLAN_SERVICE_NOT_FOUND` vs `CARE_PLAN_NOT_FOUND`), 소유권 검증 여부가 원본 스펙에 명확히 명시되어 있지 않아 ⚠️ 확인 필요로 표시 |
