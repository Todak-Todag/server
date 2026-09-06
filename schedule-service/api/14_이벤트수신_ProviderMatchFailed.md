# 14. [이벤트 수신] ProviderMatchFailed - 매칭 실패 수신

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 컨슈머) |
| 사용자 | None |
| 카테고리 | 수신 |
| 테이블명 | `p_service_schedules` |

## 설명

Provider-Service가 발행한 `ProviderMatchFailed` 이벤트를 RabbitMQ로 수신하여 서비스 일정을 변경한다.

## 흐름

Service Schedule 일정 변경 → `ProviderRematched` 발행 → Provider-Service가 매칭 가능 Provider 조회 → 매칭 가능한 Provider 부재 → `ProviderMatchFailed` 발행 → Schedule-Service가 수신하여 `p_service_schedules`의 해당 일정 상태를 변경한다.

**참고**: 이후 새 일정 생성은 자동으로 트리거되지 않으며, **사용자가 재매칭 시도 API를 호출해야 시작**된다. 매칭 실패 여부는 사용자가 직접 조회하여 확인하며, **SSE를 통한 실시간 알림 기능은 구현하지 않는다**.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `provider.exchange` |
| Routing Key | `provider.match-failed.key` |
| Queue | `schedule.provider-match-failed.queue` |
| 재시도 | 3회, DLQ 미운용 |

> **재처리(Reprocessing) 기능**: 트래픽이 증가하면 현재의 "운영자가 FAILED 건을 직접 확인 후 DB로 수동 처리"하는 방식은 유지가 어렵다. FAILED 이벤트를 재발행하거나 재처리할 수 있는 기능을 별도로 추가 개발할 예정이다 (현재 범위 밖, 백로그로 관리).
>

## 이벤트 페이로드 (Consume)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| regionId | 지역 ID | UUID | - | X | `6b2f9d41-c857-4e13-a690-5d8b2c7f1e34` |
| provideServiceId | 제공할 서비스 ID | UUID | - | X | `94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43` |
| servicePreferenceId | 사용자 희망 일정 ID | UUID | - | X | `3f2504e0-4f89-41d3-9a0c-0305e82c3301` |
| date | 매칭 시도한 날짜 | LocalDate | - | X | `2026-09-03` |
| preferredTimeSlot | 매칭 시도한 희망 시간대 | String(ENUM) | `MORNING`, `AFTERNOON` | O | `MORNING` |
| failureReason | 실패 사유 | String | - | X | `...` |
| failedAt | 실패 판정 일시 | Instant | - | X | `2026-08-29T10:00:00Z` |

**Example**

```jsx
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "regionId": "6b2f9d41-c857-4e13-a690-5d8b2c7f1e34"
  "provideServiceId": "94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43",
  "servicePreferenceId": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "date": "2026-09-03",
  "preferredTimeSlot": "MORNING",
  "failureReason": "...",
  "failedAt": "2026-08-29T10:00:00Z"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 정상 처리 | `p_service_schedules` 일정 상태를 `CANCELED`로 변경, note에 매칭 실패 사유 기록. 새 일정 생성은 사용자의 재매칭 시도 API 호출로만 시작됨 |

>
>
>
> ⚠️ 확인 필요 (기존, 재확인): "note에 매칭 실패 사유를 기록"한다고 되어 있으나 `p_service_schedules`에는 `note` 컬럼이 없고 `cancel_reason`만 존재한다(`schedule-service.md` 2장 참고). `cancel_reason`을 쓰는 것으로 추정되나 문서 표현이 정확하지 않아 확인 필요.
>
> ⚠️ 확인 필요 (신규): "재매칭 시도 API"가 01~10번 어디에도 문서화되어 있지 않다 (`schedule-service.md` 8장 참고).
>
