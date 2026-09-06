# 12. [이벤트 발행] ProviderRematched - 재매칭 알림

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 발행) |
| 사용자 | None |
| 카테고리 | 발행 |
| 테이블명 | `p_service_schedules` |

## 설명

Care Plan 확정 후 첫 매칭에서 실패했거나 확정되어있던 서비스 일정을 변경할 경우 `ProviderRematched` 이벤트를 RabbitMQ에 발행한다.

## 발행 시나리오

1. **Care Plan 확정 후 첫 매칭에서 실패했을 경우**: Care Plan 생성 및 희망 일정 선택 후 매칭 진행 → 매칭 실패 → 실패 내역을 보고 희망 일정 및 시간대를 재선택하여 재매칭을 시작할 때 발행한다.
2. **확정되어있던 서비스 일정을 변경할 경우**: 확정된 서비스 일정을 하루 앞당기거나 미룰 때(03번 API) → 희망 일정만 선택해 재매칭을 시작할 때 발행한다. 이때 시간대는 선택하지 않으므로 `preferredTimeSlot`은 `null`로 전송한다.

> ⚠️ 확인 필요 (신규, 중요): 시나리오 1번의 "재매칭을 시작"하는 주체가 되는 **"재매칭 시도 API"**는 01~10번 어디에도 문서화되어 있지 않은 완전히 새로운 API다 (`schedule-service.md` 8장 참고). 이 API의 실제 명세(Method/URL/Request/Response)를 별도로 확인해야 한다.
>

## 발행 시점

**서비스 일정 변경 API**(03번)와 **재매칭 시도 API**(미문서화, 위 참고) 로직 내에서 아웃박스 패턴을 사용하여 발행한다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `schedule.exchange` |
| Routing Key | `schedule.rematched.key` |
| Queue | `provider.schedule-rematched.queue` |
| 재시도 | 3회, DLQ 미운용 |

> **재처리(Reprocessing) 기능**: 트래픽이 증가하면 현재의 "운영자가 FAILED 건을 직접 확인 후 DB로 수동 처리"하는 방식은 유지가 어렵다. FAILED 이벤트를 재발행하거나 재처리할 수 있는 기능을 별도로 추가 개발할 예정이다 (현재 범위 밖, 백로그로 관리).
>

## 이벤트 페이로드 (Publish)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| regionId | 지역 ID | UUID | - | X | `6b2f9d41-c857-4e13-a690-5d8b2c7f1e34` |
| provideServiceId | 제공할 서비스 ID | UUID | - | X | `94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43` |
| servicePreferenceId | 사용자 희망 일정 ID | UUID | - | X | `3f2504e0-4f89-41d3-9a0c-0305e82c3301` |
| date | 재매칭을 원하는 날짜 | LocalDate | - | X | `2026-09-02` |
| preferredTimeSlot | 재매칭을 원하는 시간대 | Enum(String) | `MORNING`, `AFTERNOON` | O | `MORNING` (03번 API로 발행 시 `null`) |

**Example**

```jsx
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "regionId": "6b2f9d41-c857-4e13-a690-5d8b2c7f1e34",
  "provideServiceId": "94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43",
  "servicePreferenceId": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "date": "2026-09-02",
  "preferredTimeSlot": null
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 발행 후 | Provider-Service가 페이로드의 데이터와 해당 날짜에 서비스 제공이 가능한 서비스 제공자를 조회하여 매칭 진행 → 매칭 성공 시 `ProviderMatched` 이벤트 발행 / 매칭 실패 시 `ProviderMatchFailed` 이벤트 발행 |
