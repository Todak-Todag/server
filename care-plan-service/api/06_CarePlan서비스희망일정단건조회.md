# 06. Care Plan 서비스 희망 일정 단건 조회

| 항목     | 내용                                                  |
|--------|-----------------------------------------------------|
| Method | `GET`                                               |
| URL    | `/api/v1/service-preferences/{servicePreferenceId}` |
| 사용자    | 병원 담당자, 사회복지사, 퇴원예정자                                |
| 카테고리   | 조회                                                  |
| 테이블명   | `p_care_plan_service_preferences`                   |

## 설명

희망 일정 항목(`CarePlanServicePreference`) 하나를 상세 조회한다. `05_CarePlan서비스희망일정목록조회.md`가 Care Plan 하나에 속한 희망 일정
**목록**을 조회하는 API라면, 이 API는 희망 일정 **단건**의 상세 정보(소속 서비스 항목 `planServiceId`, 서비스 종류 `provideServiceId` 포함)를
조회한다.

## 비즈니스 규칙

- 요청자는 `HOSPITAL_STAFF`, `SOCIAL_WORKER`, `PATIENT` 세 역할을 허용한다 (원본 스펙 설명에 명시됨 — Request 표의 `X-User-Role` 예시값
  `SERVICE_PROVIDER`는 다른 문서들과 동일하게 오기로 보인다).
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.

> ⚠️ 확인 필요: 접근 범위(소유권/담당 관계) 검증 — `PATIENT`만 소유권을 검증하는 단순한 구조로 확정하지 않는다. 의도된 조회 범위는 다음과 같다.
> - `PATIENT`: 본인의 희망 일정만 조회 가능
> - `HOSPITAL_STAFF`: 본인이 등록한 퇴원예정자의 희망 일정만 조회 가능
> - `SOCIAL_WORKER`: 본인이 담당하는 퇴원예정자의 희망 일정만 조회 가능
>
> 다만 `HOSPITAL_STAFF`-퇴원예정자 등록 관계, `SOCIAL_WORKER`-퇴원예정자 담당 관계를 현재 어떤 데이터/내부 API로 검증할 수 있는지는 아직 확인
> 전이라 미확정으로 남겨둔다 (예: discharge-service의 등록자 정보, social-worker-service의 담당 배정 정보 등을 참조해야 할 가능성이 있으나
> 실제 연동 방식은 미확인). `05_CarePlan서비스희망일정목록조회.md`도 같은 자원·같은 역할 구성(`HOSPITAL_STAFF`/`SOCIAL_WORKER`/`PATIENT`)을
> 다루면서 `PATIENT`만 소유권 검증하는 것으로 이미 확정해 구현까지 마쳤는데, 위 의도를 반영하면 `05`도 동일한 접근 범위 정책 재검토가 필요한
> 상태다.
>
> ✅ 확정: 404 에러 코드는 `SERVICE_PREFERENCE_NOT_FOUND`(기존 등록된 코드)를 재사용한다. 원본 스펙 Example의 `CARE_PLAN_SERVIVE_NOT_FOUND`는
> 오타이자(`SERVIVE`), 조회 실패 대상이 `servicePreferenceId`(희망 일정)이지 `planServiceId`(Care Plan 서비스)가 아니므로 자원과도 맞지 않아
> 사용하지 않는다.

## Request

| key                 | 설명       | value 타입     | 위치            | 제약사항                                                                 | Nullable | 예시                                     |
|---------------------|----------|--------------|---------------|----------------------------------------------------------------------|----------|----------------------------------------|
| X-User-Id           | 요청자 Id   | UUID         | Header        | -                                                                    | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| servicePreferenceId | 희망 일정 ID | UUID         | Path Variable | -                                                                    | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| X-User-Role         | 요청자 권한   | String(ENUM) | Header        | Gateway가 인증 후 주입, 필수. `HOSPITAL_STAFF`/`SOCIAL_WORKER`/`PATIENT`만 허용 | X        | `PATIENT`                              |

## Response

| key                 | 설명        | value 타입     | 옵션                     | Nullable | 예시                                     |
|---------------------|-----------|--------------|------------------------|----------|----------------------------------------|
| servicePreferenceId | 희망 일정 ID  | UUID         | -                      | X        | `b7f3a1c2-0000-0000-0000-000000000001` |
| planServiceId       | 서비스 항목 ID | UUID         | -                      | X        | `c3d4e5f6-0000-0000-0000-000000000001` |
| provideServiceId    | 서비스 종류 ID | UUID         | -                      | X        | `a1b2c3d4-0000-0000-0000-000000000001` |
| preferredDate       | 희망 날짜     | LocalDate    | -                      | X        | `2026-09-10`                           |
| preferredTimeSlot   | 희망 시간대    | String(ENUM) | `MORNING`, `AFTERNOON` | X        | `MORNING`                              |
| createdAt           | 생성일       | Instant      | -                      | X        | `2026-08-28T03:30:00Z`                 |

**Example**

```javascript
// 성공
{
    "success"
:
    true,
        "code"
:
    200,
        "message"
:
    "서비스 희망 일정 상세 조회 성공",
        "data"
:
    {
        "servicePreferenceId"
    :
        "b7f3a1c2-0000-0000-0000-000000000001",
            "planServiceId"
    :
        "c3d4e5f6-0000-0000-0000-000000000001",
            "provideServiceId"
    :
        "a1b2c3d4-0000-0000-0000-000000000001",
            "preferredDate"
    :
        "2026-09-10",
            "preferredTimeSlot"
    :
        "MORNING",
            "createdAt"
    :
        "2026-08-28T03:30:00Z"
    }
}

// 실패 (404)
{
    "success"
:
    false,
        "code"
:
    "SERVICE_PREFERENCE_NOT_FOUND",
        "message"
:
    "Care Plan 서비스 희망 일정 단건 조회 실패",
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
```

## Status

| status | response content              |
|--------|-------------------------------|
| `200`  | 서비스 희망 일정 단건 조회 성공            |
| `403`  | 권한 없음                         |
| `404`  | 존재하지 않는 `servicePreferenceId` |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                                                                           |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — Notion 원본 스펙 반영. 요청자 역할(`X-User-Role` 예시값)은 설명에 명시된 대로 확정, 소유권 검증 여부와 404 에러 코드(`CARE_PLAN_SERVIVE_NOT_FOUND` vs `SERVICE_PREFERENCE_NOT_FOUND`)는 원본 스펙에 명확히 명시되어 있지 않거나 오타가 있어 ⚠️ 확인 필요로 표시                                            |
| 2026-09-05 | 접근 범위 검증 항목을 "`PATIENT` 소유권만 확인"이 아니라 역할별 3가지 범위(`PATIENT` 본인/`HOSPITAL_STAFF` 본인 등록/`SOCIAL_WORKER` 본인 담당)로 재정리 — 각 관계의 실제 검증 방식은 여전히 ⚠️ 미확정이며, `05_CarePlan서비스희망일정목록조회.md`도 동일한 재검토가 필요함을 명시. 404 에러 코드는 `SERVICE_PREFERENCE_NOT_FOUND`로 ✅ 확정 |
