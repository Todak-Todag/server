# 08. Care Plan 신청 서비스 단건 조회

| 항목     | 내용                                                         |
|--------|------------------------------------------------------------|
| Method | `GET`                                                      |
| URL    | `/api/v1/care-plans/{carePlanId}/services/{planServiceId}` |
| 사용자    | 퇴원예정자 (MVP 범위 — 아래 참고)                                     |
| 카테고리   | 조회                                                         |
| 테이블명   | `p_care_plan_services`, `p_care_plan_service_preferences`  |

## 설명

Care Plan에 신청된 특정 서비스 항목(`CarePlanService`, `care-plan-service.md` 4.2절 "Care Plan 서비스 선택") 한 건을 상세 조회하는 API다.
`07_CarePlan신청서비스목록조회.md`가 서비스 항목 **목록**을 조회하는 API라면, 이 API는 `carePlanId`와 `planServiceId`로 특정한 서비스 **단건**의
상세 정보(서비스 종류 이름/설명, 해당 서비스에 등록된 희망 일정 목록)를 조회한다.

이 API는 Provider-Service의 매칭 여부나 Schedule-Service의 실제 확정 일정과는 관계없이, Care Plan에 신청된 특정 서비스와 그 서비스에 등록한
희망 일정 정보를 조회하는 API다.

## 비즈니스 규칙

- 조회 대상은 `carePlanId`에 속한 `CarePlanService` 중 `planServiceId`와 일치하는 항목 한 건이다.
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.
- `provideServiceName`/`provideServiceContent`는 `CarePlanService`가 직접 보유하지 않는 값으로, Provider-Service 조회를 통해 채운다.
- `preferences`는 해당 서비스 항목(`planServiceId`)에 등록된 `CarePlanServicePreference` 전체를, 등록 시점(`CarePlanServicePreference.createdAt`)
  기준 **내림차순(최근 등록 순)** 으로 배열에 담아 반환한다.

> ✅ 확정 (`07_CarePlan신청서비스목록조회.md`와 동일 자원 — 아래 항목은 07의 확정 사항을 그대로 재사용):
> - 요청자는 퇴원예정자(`PATIENT`)만 허용한다. 실제 의도된 접근 범위는 `HOSPITAL_STAFF`/`SOCIAL_WORKER`/`PATIENT`이지만, 현재 구조로는 병원 담당자·사회복지사의 접근 범위(담당/등록 관계) 검증 기준이 없어 MVP 범위에서는 `PATIENT`만 우선 허용한다(컨트롤러에 `// TODO: (MVP 이후) HOSPITAL_STAFF`/`SOCIAL_WORKER` 접근 허용` 주석으로 확장 지점 표시). `PATIENT`는 본인 소유 Care Plan(`patientId == X-User-Id`)만 조회 가능하다(`CarePlanOwnerValidator`).
> - `provideServiceId`의 Nullable: `p_care_plan_services.provide_service_id`는 필수 값(`CarePlanService.provideServiceId` 엔티티상 `nullable = false`)이므로, 원본 스펙 Response 표의 `O`(nullable) 표기 대신 `X`로 정정한다.
> - `provideServiceName`/`provideServiceContent` 조회 경로: `07` 작성 시점에는 Provider-Service용 Feign Client가 없어 미확정이었으나, 이후 `ProviderServiceQueryPort` → `ProviderServiceClientAdapter` → `ProviderServiceFeignClient`(`GET /internal/v1/provide-services`) 연동이 실제로 구현되어 있다. `provideServiceContent`도 같은 Internal API 응답의 `content` 필드로 함께 채울 수 있다(`ProvideServiceInfoResult.content()`).
> - 404 에러 코드: `carePlanId`에 해당하는 Care Plan이 존재하지 않는 경우는 `CARE_PLAN_NOT_FOUND`(07과 동일 재사용)를, `planServiceId`가 존재하지 않거나 해당 `planServiceId`가 요청한 `carePlanId`에 속하지 않는 경우는 `CARE_PLAN_SERVICE_NOT_FOUND`(기존 등록된 코드, `CarePlanServiceCommandService` 등에서 이미 사용 중)를 사용한다. 원본 스펙 Example은 `carePlanId` 미존재 케이스만 예시로 들고 있어, `planServiceId` 미존재/불일치 케이스 Example을 아래에 추가했다.
> - `preferences` 정렬 기준: 이 API는 실제 확정 일정 조회가 아니라 Care Plan 신청 시 등록한 희망 일정 목록을 보여주는 API다. 희망 일정은 이후 매칭 실패·취소·수정 가능성이 있고 `preferredDate`가 실제 수행 예정 순서를 의미하지 않으므로, 다가오는 날짜순(`preferredDate` 오름차순)이 아니라 최근에 등록한 희망 일정이 먼저 보이도록 `CarePlanServicePreference.createdAt` 내림차순으로 정렬한다(`05_CarePlan서비스희망일정목록조회.md`의 `preferredDate` 오름차순 정렬과는 별개의 기준이다).
> - Provider-Service 데이터 불일치: `CarePlanService`에는 `provideServiceId`가 존재하지만 Provider-Service 응답에 해당 ID가 없는 경우(Provider-Service 측 데이터 불일치), 목록 조회(`07`)와 동일하게 `PROVIDER_SERVICE_DATA_MISMATCH`(`502 Bad Gateway`)를 반환하고 정상 응답으로 처리하지 않는다.

## Request

| key           | 설명           | value 타입     | 위치            | 제약사항                                | Nullable | 예시                                     |
|---------------|--------------|--------------|---------------|-------------------------------------|----------|----------------------------------------|
| X-User-Id     | 요청자 Id       | UUID         | Header        | -                                   | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| carePlanId    | Care Plan ID | UUID         | Path Variable | -                                   | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| planServiceId | 서비스 항목 ID    | UUID         | Path Variable | -                                   | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| X-User-Role   | 요청자 권한       | String(ENUM) | Header        | Gateway가 인증 후 주입, 필수. `PATIENT`만 허용 | X        | `PATIENT`                              |

## Response

| key                   | 설명                   | value 타입     | 옵션 | Nullable | 예시                                        |
|-----------------------|----------------------|--------------|----|----------|-------------------------------------------|
| planServiceId         | 서비스 항목 ID            | UUID         | -  | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6`    |
| provideServiceId      | 서비스 종류 ID            | UUID         | -  | X        | `5fa85f64-5717-4562-b3fc-2c963f66afa6`    |
| provideServiceName    | 서비스 이름               | String       | -  | X        | `방문 간호`                                   |
| provideServiceContent | 서비스 설명               | String       | -  | X        | `간호사가 가정을 방문하여 건강 상태 확인 및 간호 서비스를 제공합니다.` |
| preferences           | 해당 서비스에 등록한 희망 일정 목록(`createdAt` 내림차순) | List<Object> | -  | X        | -                                         |
| createdAt             | 생성일                  | Instant      | -  | X        | `2026-08-28T03:30:00Z`                    |

`preferences[]` 배열의 각 원소는 아래 필드를 갖는다.

| key                 | 설명      | value 타입     | 옵션                     | Nullable | 예시                                     |
|---------------------|---------|--------------|------------------------|----------|-----------------------------------------|
| servicePreferenceId | 희망 일정 ID | UUID         | -                      | X        | `5fa85f64-5717-4562-b3fc-2c963f66afa6` |
| preferredDate       | 희망 날짜   | LocalDate    | -                      | X        | `2026-09-10`                            |
| preferredTimeSlot   | 희망 시간대  | String(ENUM) | `MORNING`, `AFTERNOON` | X        | `MORNING`                               |

**Example**

```json
// 성공
{
  "success": true,
  "code": 200,
  "message": "신청 서비스 조회 성공",
  "data": {
    "planServiceId": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
    "provideServiceId": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
    "provideServiceName": "방문 간호",
    "provideServiceContent": "간호사가 가정을 방문하여 건강 상태 확인 및 간호 서비스를 제공합니다.",
    "preferences": [
      {
        "servicePreferenceId": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
        "preferredDate": "2026-09-12",
        "preferredTimeSlot": "AFTERNOON"
      },
      {
        "servicePreferenceId": "6fa85f64-5717-4562-b3fc-2c963f66afa6",
        "preferredDate": "2026-09-10",
        "preferredTimeSlot": "MORNING"
      }
    ],
    "createdAt": "2026-08-28T03:30:00Z"
  }
}

// 실패 (404 - carePlanId 미존재)
{
  "success": false,
  "code": "CARE_PLAN_NOT_FOUND",
  "message": "Care Plan 신청 서비스 조회 실패",
  "details": {
    "reason": "carePlanId에 해당하는 Care Plan이 존재하지 않습니다."
  },
  "timestamp": "2026-08-28T03:30:00Z"
}

// 실패 (404 - planServiceId 미존재/불일치)
{
  "success": false,
  "code": "CARE_PLAN_SERVICE_NOT_FOUND",
  "message": "Care Plan 신청 서비스 조회 실패",
  "details": {
    "reason": "해당 Care Plan에 planServiceId에 해당하는 서비스 항목이 존재하지 않습니다."
  },
  "timestamp": "2026-08-28T03:30:00Z"
}

// 실패 (502 - Provider-Service 데이터 불일치)
{
  "success": false,
  "code": "PROVIDER_SERVICE_DATA_MISMATCH",
  "message": "Care Plan 신청 서비스 조회 실패",
  "details": {
    "reason": "Provider-Service의 서비스 정보와 Care Plan 데이터가 일치하지 않습니다."
  },
  "timestamp": "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content                                                          |
|--------|-----------------------------------------------------------------------------|
| `200`  | 신청 서비스 단건 조회 성공                                                           |
| `403`  | 권한 없음                                                                     |
| `404`  | 존재하지 않는 `carePlanId`의 Care Plan, 또는 해당 Care Plan에 존재하지 않는 `planServiceId` |
| `502`  | Provider-Service 응답에 `provideServiceId`가 없는 데이터 불일치(`PROVIDER_SERVICE_DATA_MISMATCH`) |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                                                                                                                                     |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-06 | 최초 작성 — 전달받은 API 스펙 반영. `07_CarePlan신청서비스목록조회.md`와 동일 자원(`CarePlanService`)의 단건 조회 API여서 요청자 역할(`PATIENT`만 MVP 허용)/`provideServiceId` Nullable 정정/404 에러 코드(`CARE_PLAN_NOT_FOUND`, `CARE_PLAN_SERVICE_NOT_FOUND`) 확정 사항을 그대로 재사용해 반영. `provideServiceName`/`provideServiceContent`는 이후 실제 구현된 `ProviderServiceQueryPort` 연동으로 채울 수 있음을 확인. `preferences` 정렬 기준과 Provider-Service 데이터 불일치 시 처리 방식(502 `PROVIDER_SERVICE_DATA_MISMATCH` 적용 여부)은 ⚠️ 확인 필요로 표시 |
| 2026-09-06 | ✅ 확정 반영 — `preferences` 정렬 기준을 `preferredDate` 오름차순이 아닌 `CarePlanServicePreference.createdAt` 내림차순(최근 등록 순)으로 확정(희망 일정은 매칭 실패/취소/수정 가능성이 있어 `preferredDate`가 실제 수행 순서를 의미하지 않음). Provider-Service 데이터 불일치 시 07과 동일하게 `PROVIDER_SERVICE_DATA_MISMATCH`(502 Bad Gateway)로 확정, Status 표/Example에 502 케이스 추가. 404 사유를 `planServiceId` 미존재 또는 요청한 `carePlanId`에 속하지 않는 경우로 구체화. API 성격(Provider-Service 매칭·Schedule-Service 확정 일정과 무관) 설명 보강 |
