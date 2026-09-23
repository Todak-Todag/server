# 07. Care Plan 신청 서비스 목록 조회

| 항목     | 내용                                         |
|--------|--------------------------------------------|
| Method | `GET`                                      |
| URL    | `/api/v1/care-plans/{carePlanId}/services` |
| 사용자    | 퇴원예정자 (MVP 범위 — 아래 참고)                     |
| 카테고리   | 조회                                         |
| 테이블명   | `p_care_plan_services`                     |

## 설명

Care Plan에 신청된 서비스 목록을 조회하는 API다. 해당 Care Plan에 포함된 서비스 항목(`CarePlanService`, `care-plan-service.md` 4.2절
"Care Plan 서비스 선택")을 기준으로 조회하며, Provider-Service의 매칭 여부와는 관계없이 퇴원예정자가 신청한 서비스 목록을 그대로 반환한다. 서비스가
Care Plan에 추가된 시점(`createdAt`)을 기준으로 정렬된다.

> 2026-09-06 회의 반영: 최초 초안은 Provider-Service의 `ProviderMatched` 이벤트 수신 후 "매칭 확정된" 서비스만 보여주는 API로 잘못
> 작성되어 있었으나(코드/문서 어디에도 그런 이벤트 수신 로직이 없어 실제로는 구현 불가능한 전제였음), 이번 변경으로 매칭 여부와 무관하게 기존
> `CarePlanService`(4.2절에서 이미 생성되는 리소스) 목록을 그대로 보여주는 API로 확정되었다. 이에 따라 `ProviderMatched` 관련 불확실성은
> 모두 해소되었다.

## 비즈니스 규칙

- 정렬 기준은 Care Plan에 추가된 시점(`createdAt`) 오름차순으로 고정이다 (원본 스펙에 내림차순 여부가 명시되어 있지 않아 오름차순으로 가정 —
  ⚠️ 확인 필요).
- 요청자의 `userId`와 `role`은 `X-User-Id`/`X-User-Role` 헤더(Gateway가 인증 후 주입)로 확인한다.
- `size`는 `10`/`30`/`50`만 허용하며, 그 외 값이 들어오면 `10`으로 자동 대체한다 (`PageableFactory` 공통 정책, `05_CarePlan서비스희망일정목록조회.md`
  참고).

> ✅ 확정 (2026-09-06, 회의 반영): 실제 원하는 접근 범위는 `HOSPITAL_STAFF`/`SOCIAL_WORKER`/`PATIENT` 세 역할이지만, 현재 구조로는 병원
> 담당자·사회복지사의 접근 범위(담당/등록 관계) 검증 기준이 없어(`06_CarePlan서비스희망일정단건조회.md`에서도 동일하게 미확정으로 남아있는
> 문제) MVP 범위에서는 우선 `PATIENT`만 허용한다. 컨트롤러의 `@GetMapping` 바로 위에 `// TODO: (MVP 이후) HOSPITAL_STAFF`/`SOCIAL_WORKER`
> 접근 허용` 주석을 남겨 향후 확장 지점을 표시한다. `PATIENT`는 본인 소유 Care Plan(`patientId == X-User-Id`)만 조회 가능하다
> (`CarePlanOwnerValidator`, 다른 `PATIENT` 전용 API와 동일 패턴).

> ⚠️ 확인 필요 — 원본 스펙 기준으로 여전히 남아있는 항목들:
> - 404 에러 코드: 원본 스펙 Example은 `CARE_PLAN_SERVICE_NOT_FOUND`를 사용하지만, 실패 사유("carePlanId에 해당하는 Care Plan이 존재하지
    > 않습니다")는 `carePlanId` 조회 실패이지 `planServiceId`(Care Plan 서비스) 조회 실패가 아니다. `05`에서 동일한 사유로 이미
    > `CARE_PLAN_NOT_FOUND`(기존 등록된 코드)로 확정한 선례가 있어 이 문서도 `CARE_PLAN_NOT_FOUND`를 재사용하는 쪽으로 잠정 기재했다.
> - `provideServiceId`의 Nullable: 원본 스펙 Request 표는 `O`(nullable)로 표기되어 있으나, `CarePlanService.provideServiceId`는
    > 엔티티상 `nullable = false`이며 다른 필드와 비교해도 nullable일 이유가 없어 원본 표기 오류로 보고 `X`로 정정했다.
> - `provideServiceName`: `p_care_plan_services`/`CarePlanService`는 `provideServiceId`(UUID)만 보유하고 서비스 이름은 저장하지
    > 않는다. 이름을 채우려면 Provider-Service 조회가 필요한데, 현재 `infrastructure/client/`에는 Provider-Service용 Feign Client가
    > 없다(`DischargeFeignClient`/`UserFeignClient`만 존재). 신규 Internal API 연동이 필요한지, 아니면 응답에서 이 필드를 제외할지 확인이
    > 필요하다.

## Request

| key         | 설명           | value 타입     | 위치              | 제약사항                                                   | Nullable | 예시                                     |
|-------------|--------------|--------------|-----------------|--------------------------------------------------------|----------|----------------------------------------|
| X-User-Id   | 요청자 Id       | UUID         | Header          | -                                                      | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| carePlanId  | Care Plan ID | UUID         | Path Variable   | -                                                      | X        | `3fa85f64-5717-4562-b3fc-2c963f66afa6` |
| page        | 페이지 번호       | Integer      | Query Parameter | 음수 불가능                                                 | O        | `0`                                    |
| size        | 페이지 크기       | Integer      | Query Parameter | `10`, `30`, `50`만 가능하며 기본값은 `10`. 그 외 값이면 `10`으로 자동 변경 | O        | `10`                                   |
| X-User-Role | 요청자 권한       | String(ENUM) | Header          | Gateway가 인증 후 주입, 필수                                   | X        | `PATIENT`                              |

## Response

| key                | 설명        | value 타입 | 옵션 | Nullable | 예시                                     |
|--------------------|-----------|----------|----|----------|----------------------------------------|
| planServiceId      | 서비스 항목 ID | UUID     | -  | X        | `4fa85f64-5717-4562-b3fc-2c963f66afa6` |
| provideServiceId   | 서비스 종류 ID | UUID     | -  | X        | `5fa85f64-5717-4562-b3fc-2c963f66afa6` |
| provideServiceName | 서비스 이름    | String   | -  | X        | `방문 간호`                                |
| createdAt          | 생성일       | Instant  | -  | X        | `2026-08-28T03:30:00Z`                 |

목록은 `data.content[]`에 위 필드들을 담고, `data.pageInfo`에 페이지 정보(`paginationType`, `page`, `size`, `totalElements`,
`totalPages`)를
담는 공통 페이지네이션 응답 포맷(`PageResponse`/`PageInfo`, `docs/코드_컨벤션_구현용.md` 참고)을 그대로 따른다.

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
    "신청 서비스 목록 조회 성공",
        "data"
:
    {
        "content"
    :
        [
            {
                "planServiceId": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
                "provideServiceId": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
                "provideServiceName": "방문 간호",
                "createdAt": "2026-08-28T03:30:00Z"
            }
        ],
            "pageInfo"
    :
        {
            "paginationType"
        :
            "OFFSET",
                "page"
        :
            0,
                "size"
        :
            10,
                "totalElements"
        :
            1,
                "totalPages"
        :
            1
        }
    }
}

// 실패 (404)
{
    "success"
:
    false,
        "code"
:
    "CARE_PLAN_NOT_FOUND",
        "message"
:
    "Care Plan 신청 서비스 목록 조회 실패",
        "details"
:
    {
        "reason"
    :
        "carePlanId에 해당하는 Care Plan이 존재하지 않습니다."
    }
,
    "timestamp"
:
    "2026-08-28T03:30:00Z"
}
```

## Status

| status | response content                |
|--------|---------------------------------|
| `200`  | 신청 서비스 목록 조회 성공                 |
| `403`  | 권한 없음                           |
| `404`  | 존재하지 않는 `carePlanId`의 Care Plan |

## 변경 이력

| 날짜         | 변경 내용                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-09-05 | 최초 작성 — 전달받은 API 스펙 반영. 이 API가 전제하는 "매칭 확정"(`ProviderMatched` 이벤트 수신) 개념이 현재 `CarePlanService`/`care-plan-service.md`에 존재하지 않는 점, 요청 허용 역할, 404 에러 코드(`CARE_PLAN_SERVICE_NOT_FOUND` vs `CARE_PLAN_NOT_FOUND`), `provideServiceId` Nullable 표기 오류, `provideServiceName` 조회 방법(Provider-Service 연동 부재) 모두 ⚠️ 확인 필요로 표시                                                                                                                                |
| 2026-09-06 | 회의 결과 반영 — API 성격이 "매칭 확정된 서비스 조회"(`ProviderMatched` 이벤트 기반, 구현 불가능한 전제였음)에서 "Care Plan에 신청된 서비스 조회"(매칭 여부 무관, 기존 `CarePlanService` 그대로 반환)로 변경됨에 따라 문서 제목/설명/Example 메시지를 전면 수정. `ProviderMatched` 관련 불확실성 해소. 요청 허용 역할은 `HOSPITAL_STAFF`/`SOCIAL_WORKER`의 접근 범위 검증 기준이 아직 없어 MVP 범위에서 `PATIENT`만 우선 허용하는 것으로 ✅ 확정(컨트롤러에 `TODO: (MVP 이후)` 주석으로 확장 지점 표시 예정). 404 에러 코드/`provideServiceId` Nullable 표기/`provideServiceName` 조회 방법은 여전히 ⚠️ 확인 필요로 유지 |
