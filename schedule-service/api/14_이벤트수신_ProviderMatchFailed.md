# 14. [이벤트 수신] ProviderMatchFailed - 매칭 실패 수신

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 컨슈머) |
| 사용자 | None |
| 카테고리 | 수신 |
| 테이블명 | `p_service_matching_attempts`, `p_service_schedules` |

## 설명

Provider-Service가 발행한 `ProviderMatchFailed` 이벤트를 RabbitMQ로 수신하여 매칭 실패 이력을 남기고, 재매칭 실패인 경우 기존 일정을 원래 상태로 되돌린다. **초기 매칭 실패와 재매칭 실패가 같은 이벤트/같은 소비자를 공유한다.**

## 흐름

```
[초기 매칭 실패] CarePlanConfirmed → Provider-Service 매칭 → 가능 Provider 부재 → ProviderMatchFailed
[재매칭 실패]   03번/16번 → ProviderRematched → 재매칭 → 가능 Provider 부재 → ProviderMatchFailed
                                    ↓
                    Schedule-Service 수신 (ProviderMatchFailedEventListener)
```

**참고**: 이후 새 일정 생성은 자동으로 트리거되지 않으며, **사용자가 15번(매칭 실패 내역 조회)으로 확인한 뒤 16번(재매칭 시도) 또는 03번(일정 변경)을 호출해야** 시작된다. **SSE를 통한 실시간 알림 기능은 구현하지 않는다.**

## 처리 절차

한 트랜잭션(`ServiceMatchingCommandService.applyMatchFailed`)에서 처리한다.

1. **멱등 검사** — 이미 처리된 이벤트면 아무것도 하지 않고 종료한다.
2. **실패 이력 기록** — `p_service_matching_attempts`에 `status = FAILED`로 append 한다.
   - 매칭된 대상이 없으므로 `service_offering_id`는 `null`, `matched_at`도 `null`
   - `failure_reason`/`failed_at`/`preferred_time_slot`은 페이로드 값을 그대로 기록
3. **재매칭 실패면 기존 일정 복구** — 같은 `servicePreferenceId`의 `RESCHEDULING` 일정을 **`SCHEDULED`로 되돌린다.**

### 일정 상태 처리

> 이전 문서의 "`p_service_schedules` 일정 상태를 `CANCELED`로 변경하고 note에 실패 사유를 기록"은 **구현과 다르다.**
>
> - **초기 매칭 실패**: `p_service_schedules`에 레코드 자체가 아직 없으므로 **아무 일정도 건드리지 않는다.**
> - **재매칭 실패**: 변경 요청 이전 상태인 **`SCHEDULED`로 복구**한다. 사용자는 기존 날짜의 일정을 그대로 유지하게 되며, 원치 않으면 04번으로 직접 취소한다.
> - 어느 경우에도 `CANCELED`로 전이하지 않으며, `cancel_reason`(존재하지 않는 `note` 컬럼이 아니라)은 04번 사용자 취소 경로에서만 채워진다.

| `RESCHEDULING` 건수 | 판정 | 처리 |
| --- | --- | --- |
| 0건 | 초기 매칭 실패 | 이력만 기록 |
| 1건 | 재매칭 실패 | 해당 일정을 `SCHEDULED`로 복구 |
| 2건 이상 | 데이터 이상 | `409 SERVICE_SCHEDULE_MULTIPLE_RESCHEDULING` (리스너가 로그 후 메시지 폐기) |

### 멱등 처리

실패 페이로드에는 `serviceOfferingId`가 없으므로 `failedAt`을 대체 키에 포함한다.

```
servicePreferenceId + date + failedAt  → 이미 FAILED 이력이 있으면 skip
```

### 예외 처리

리스너는 `BusinessException`을 잡아 **에러 로그만 남기고 메시지를 폐기**한다. 그 외 예외는 리스너 컨테이너의 재시도 설정(3회)을 따른다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `provider.exchange` (Direct) |
| Routing Key | `provider.match-failed.key` |
| Queue | `schedule.provider-match-failed.queue` |
| 재시도 | 3회(리스너 컨테이너), DLQ 미운용 |

## 이벤트 페이로드 (Consume)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| regionId | 지역 ID | UUID | - | X | `6b2f9d41-c857-4e13-a690-5d8b2c7f1e34` |
| provideServiceId | 제공할 서비스 ID | UUID | - | X | `94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43` |
| servicePreferenceId | 사용자 희망 일정 ID | UUID | - | X | `3f2504e0-4f89-41d3-9a0c-0305e82c3301` |
| date | 매칭 시도한 날짜 | LocalDate | - | X | `2026-09-03` |
| preferredTimeSlot | 매칭 시도한 희망 시간대 | String(ENUM) | `MORNING`, `AFTERNOON` | O | `MORNING` |
| failureReason | 실패 사유 | String | - | X | `NO_AVAILABLE_PROVIDER` |
| failedAt | 실패 판정 일시 | Instant | - | X | `2026-08-29T10:00:00Z` |

> provider-service의 `ProviderMatchFailedEvent` 필드명이 이 표와 완전히 일치한다(선언 순서만 다르며 JSON이라 영향 없음). `failureReason`은 현재 **`NO_AVAILABLE_PROVIDER` 코드 문자열 한 종류**만 발행된다(`ProviderMatchFailedEvent.NO_AVAILABLE_PROVIDER`) — 15번 API가 이 값을 그대로 노출하므로 표시 문구 변환 주체를 정해야 한다.
>

**Example**

```jsx
{
  "carePlanId": "a83e5c17-2d69-47f4-b901-8c6a3e5d7f20",
  "regionId": "6b2f9d41-c857-4e13-a690-5d8b2c7f1e34",
  "provideServiceId": "94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43",
  "servicePreferenceId": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
  "date": "2026-09-03",
  "preferredTimeSlot": "MORNING",
  "failureReason": "NO_AVAILABLE_PROVIDER",
  "failedAt": "2026-08-29T10:00:00Z"
}
```

## 처리 결과

| 결과 | 내용 |
| --- | --- |
| 초기 매칭 실패 | `p_service_matching_attempts`에 `FAILED` 이력 추가. `p_service_schedules`는 변경 없음 |
| 재매칭 실패 | 위에 더해, 기존 `RESCHEDULING` 일정을 `SCHEDULED`로 복구 |
| 중복 수신 | 아무 작업도 하지 않고 로그만 남김 |

> **해소됨**: 초기 매칭 실패가 `p_service_schedules`에 레코드를 남기지 않아 `CarePlanCompleted`(11번)가 조기 발행되던 문제는, 11번의 완료 판정이 **미해소 `FAILED` 이력**까지 함께 보도록 보강해 해결했다. 상세 조건은 `11_이벤트발행_CarePlanCompleted.md`의 "매칭 기준이 필요한 이유" 참고.
>
