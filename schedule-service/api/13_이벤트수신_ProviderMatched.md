# 13. [이벤트 수신] ProviderMatched - 매칭 결과 수신

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 컨슈머) |
| 사용자 | None |
| 카테고리 | 수신 |
| 테이블명 | `p_service_schedules` |

## 설명

Provider-Service가 발행한 `ProviderMatched` 이벤트를 RabbitMQ로 수신하여 서비스 일정을 생성한다.

## 흐름

Care Plan 확정 → `CarePlanConfirmed` 발행 → Provider-Service가 매칭 가능 Provider 조회 → 매칭 성공 → `ProviderMatched` 발행 → Schedule-Service가 수신하여 `p_service_matching_attempts`, `p_service_schedules` 테이블에 새 데이터를 추가한다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `provider.exchange` |
| Routing Key | `provider.matched.key` |
| Queue | `schedule.provider-matched.queue` |
| 재시도 | 3회, DLQ 미운용 |

> **재처리(Reprocessing) 기능**: 트래픽이 증가하면 현재의 "운영자가 FAILED 건을 직접 확인 후 DB로 수동 처리"하는 방식은 유지가 어렵다. FAILED 이벤트를 재발행하거나 재처리할 수 있는 기능을 별도로 추가 개발할 예정이다 (현재 범위 밖, 백로그로 관리).
>

## 이벤트 페이로드 (Consume)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| regionId | 지역 ID | UUID | - | X | `6b2f9d41-c857-4e13-a690-5d8b2c7f1e34` |
| provideServiceId | 매칭된 서비스 종류 ID | UUID | - | X | `94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43` |
| servicePreferenceId | 사용자 희망 일정 ID | UUID | - | X | `3f2504e0-4f89-41d3-9a0c-0305e82c3301` |
| serviceOfferingId | 매칭된 제공자의 서비스 ID | UUID | - | X | `9b2f1c3a-1111-4a2b-8c3d-abcdef123456` |
| date | 서비스 수행 날짜 | Date | - | X | `2026-09-03` |
| startedAt | 서비스 시작 일시 | LocalDateTime | - | X | `2026-09-03T10:00:00` |
| matchedAt | 매칭 확정 일시 | Instant | - | X | `2026-08-29T10:00:00Z` |

> ⚠️ 확인 필요 (신규, 중요): `p_service_schedules`의 NOT NULL 컬럼인 **`finished_at`에 대응하는 필드(`finishedAt`)가 페이로드에 없다.** `date`/`startedAt`은 포함되어 있어 이전에 지적했던 문제가 부분 해소됐으나, `finishedAt` 하나가 여전히 빠져 있어 이 값 없이는 `p_service_schedules`에 레코드를 생성할 수 없다. Provider-Service 팀과 확인 필요.
>

**Example**

```jsx
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "regionId": "6b2f9d41-c857-4e13-a690-5d8b2c7f1e34",
  "provideServiceId": "94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43",
  "servicePreferenceId": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "serviceOfferingId": "9b2f1c3a-1111-4a2b-8c3d-abcdef123456",
  "date": "2026-09-03",
  "startedAt": "2026-09-03T10:00:00",
  "matchedAt": "2026-08-29T10:00:00Z"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 정상 처리 | `p_service_schedules`에 새 레코드 생성, 초기 `status`는 `SCHEDULED` |

> ⚠️ 확인 필요: 재매칭으로 인한 `ProviderMatched` 수신 시(기존 일정이 있는 경우)에는 새 레코드를 또 생성하는 것인지, 아니면 기존 레코드의 `status`를 `CHANGED`로 갱신하는 것인지 이 문서만으로는 불명확하다. "정상 처리"란이 "새 레코드 생성"만 기술하고 있어, `schedule-service.md` 3장의 "성공 시 CHANGED 전환"과 어떻게 연결되는지 확인 필요 (신규 매칭과 재매칭 시 처리 로직이 실제로 다른지).
>
