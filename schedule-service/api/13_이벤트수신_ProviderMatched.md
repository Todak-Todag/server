# 13. [이벤트 수신] ProviderMatched - 매칭 결과 수신

| 항목 | 내용 |
| --- | --- |
| Method | None (비동기 이벤트 컨슈머) |
| 사용자 | None |
| 카테고리 | 수신 |
| 테이블명 | `p_service_matching_attempts`, `p_service_schedules` |

## 설명

Provider-Service가 발행한 `ProviderMatched` 이벤트를 RabbitMQ로 수신하여 매칭 이력을 남기고 서비스 일정을 생성한다. **초기 매칭과 재매칭이 같은 이벤트/같은 소비자를 공유한다.**

## 흐름

```
[초기 매칭] Care Plan 확정 → CarePlanConfirmed → Provider-Service 매칭 → ProviderMatched
[재매칭]   03번/16번 → ProviderRematched → Provider-Service 재매칭 → ProviderMatched
                                    ↓
                      Schedule-Service 수신 (ProviderMatchedEventListener)
```

## 처리 절차

한 트랜잭션(`ServiceMatchingCommandService.applyMatched`)에서 처리한다.

1. **멱등 검사** — 이미 처리된 이벤트면 아무것도 하지 않고 종료한다.
2. **매칭 이력 기록** — `p_service_matching_attempts`에 `status = MATCHED`로 append 한다.
   - 실패 관련 컬럼(`failure_reason`/`failed_at`)은 `null`
   - 페이로드에 시간대가 없으므로 `preferred_time_slot`도 `null`
3. **재매칭이면 기존 일정 마감** — 같은 `servicePreferenceId`의 `RESCHEDULING` 일정을 `CHANGED`로 전이한다.
4. **새 일정 생성** — `p_service_schedules`에 `status = SCHEDULED`로 신규 레코드를 만든다.

### 신규 매칭 / 재매칭 구분

같은 `servicePreferenceId`로 `RESCHEDULING` 상태인 일정이 있는지로 판단한다.

| `RESCHEDULING` 건수 | 판정 | 처리 |
| --- | --- | --- |
| 0건 | 신규 매칭 | 새 일정만 생성 |
| 1건 | 재매칭 | 기존 일정 → `CHANGED`, 새 일정 생성 |
| 2건 이상 | 데이터 이상 | `409 SERVICE_SCHEDULE_MULTIPLE_RESCHEDULING` (리스너가 로그 후 재시도 소진 → DLQ 이동) |

### 멱등 처리

페이로드에 이벤트 ID가 없어, 같은 매칭 결과를 특정하는 값들의 조합을 대체 키로 사용한다.

```
servicePreferenceId + serviceOfferingId + date + matchedAt  → 이미 MATCHED 이력이 있으면 skip
```

`matchedAt`(매칭 확정 일시)이 포함되어 있어, 같은 희망 일정이 나중에 다시 매칭되는 정상 케이스와는 구분된다.

방어선은 두 겹이다.

| 계층 | 수단 | 역할 |
| --- | --- | --- |
| 애플리케이션 | `ServiceMatchingCommandService.alreadyApplied` | 이미 처리된 재전송을 조용히 skip (정상 경로) |
| DB | `ux_p_service_matching_attempts_matched` (부분 유니크 인덱스) | 위 조회와 적재 사이를 파고든 동시 수신을 차단 |

애플리케이션 조회만으로는 **check-then-act** 사이에 끼어든 동시 수신을 막지 못해, 완전히 동일한 이벤트가 정확히 같은 시점에 두 번 들어오면 양쪽 다 조회를 통과해 이력과 일정이 중복 생성됐다. `V2__add_service_matching_attempt_unique_index.sql`이 같은 조합에 부분 유니크 인덱스를 걸어 DB를 최종 방어선으로 둔다.

```sql
CREATE UNIQUE INDEX ux_p_service_matching_attempts_matched
    ON schedule_schema.p_service_matching_attempts (service_preference_id, service_offering_id, date, matched_at)
    WHERE status = 'MATCHED' AND deleted_at IS NULL;
```

> 인덱스의 컬럼 구성과 `status`/`deleted_at` 조건은 `existsMatched` 쿼리와 **반드시 같아야 한다.** 어긋나면 애플리케이션은 통과시키는데 DB가 막는(혹은 그 반대의) 건이 생긴다.
>

유니크 인덱스 위반 시에는 `DataIntegrityViolationException`을 잡아 원인이 드러나는 에러 로그를 남기고 **그대로 다시 던진다** (11번 아웃박스 유니크 인덱스와 같은 처리). Postgres는 제약 위반 시 트랜잭션 전체를 abort하므로 정상 흐름으로 되돌릴 수 없기 때문이다. 중복이라 아무것도 이중 기록되지 않은 상태이므로, 재시도를 소진해 DLQ로 간 메시지는 폐기해도 안전하다.

#### 남아있는 한계 — 타임스탬프가 어긋난 재전송

대체 키에 `matchedAt`이 들어가는 한, **마이크로초 이상 타임스탬프가 어긋난 재전송은 별개 매칭으로 처리된다.** 수신 측 단독으로는 이를 정상 재매칭과 구별할 방법이 없다.

다만 실제 재전송 경로에서는 이 상황에 닿지 않는다.

- provider-service는 트랜잭션 아웃박스를 쓰고 `p_provider_outbox_events.payload`가 `updatable = false`라, **릴레이 재시도·브로커 재전달·DLQ 재투입 모두 저장된 페이로드를 그대로 재발행**한다 → `matchedAt`이 바뀌지 않는다.
- provider-service 쪽도 `CarePlanConfirmed` 재수신은 `processedPreferenceIds`로, 재매칭 재전달은 `alreadyRematched()`(10분 윈도우)로 매칭 재실행을 막는다.
- `matched_at`은 `TIMESTAMPTZ`(마이크로초)라 `Instant`의 나노초 자리는 DB에 닿기 전에 잘린다 → **마이크로초 미만 드리프트는 이미 중복으로 걸러진다.**

근본 해결은 provider-service가 페이로드에 `eventId`(아웃박스의 `outbox_event_id`를 그대로 쓰면 된다)를 실어주고, 수신 측이 그 값 단독으로 판정하는 것이다. **후속 작업으로 남아있다.**

### 예외 처리

리스너는 `BusinessException`을 잡아 에러 로그를 남긴 뒤 **그대로 다시 던진다.** 그 외 예외와 동일하게 리스너 컨테이너의 재시도 설정(`spring.rabbitmq.listener.simple.retry`, 3회)을 따르고, 소진하면 메시지를 폐기하지 않고 DLQ(`schedule.provider-matched.dlq.queue`)로 옮긴다.

`RESCHEDULING` 중복이나 상태 불일치는 **메시지가 아니라 수신 측 DB 상태의 문제**라, 데이터를 정리한 뒤 DLQ의 메시지를 원래 큐로 되돌리면 정상 처리된다. 아래 멱등 대체 키가 중복 적재를 막으므로 재투입이 안전하다.

삼키지 않는 이유는 `applyMatched`가 한 트랜잭션이기 때문이다 — 실패하면 일정도 매칭 이력도 남지 않아, 메시지를 버리면 provider-service와 영구 불일치가 된다.

## 메시징 정보

| 항목 | 값 |
| --- | --- |
| Exchange | `provider.exchange` (Direct) |
| Routing Key | `provider.matched.key` |
| Queue | `schedule.provider-matched.queue` |
| 재시도 | 3회(리스너 컨테이너), 소진 시 DLQ로 이동 |
| DLX | `schedule.dlx.exchange` (Direct) |
| DLQ | `schedule.provider-matched.dlq.queue` (routing key `schedule.provider-matched.dlq.key`) |

> Exchange/Queue/Binding은 수신 측(schedule-service)에서도 선언해 두어, 발행 측보다 먼저 뜨더라도 메시지가 유실되지 않게 한다.
>

## 이벤트 페이로드 (Consume)

| key | 설명 | value 타입 | 옵션 | Nullable | 예시 |
| --- | --- | --- | --- | --- | --- |
| carePlanId | 케어플랜 ID | UUID | - | X | `a83e5c17-2d69-47f4-b901-8c6a3e5d7f20` |
| regionId | 지역 ID | UUID | - | X | `6b2f9d41-c857-4e13-a690-5d8b2c7f1e34` |
| provideServiceId | 매칭된 서비스 종류 ID | UUID | - | X | `94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43` |
| servicePreferenceId | 사용자 희망 일정 ID | UUID | - | X | `3f2504e0-4f89-41d3-9a0c-0305e82c3301` |
| serviceOfferingId | 매칭된 제공자의 서비스 ID | UUID | - | X | `9b2f1c3a-1111-4a2b-8c3d-abcdef123456` |
| date | 서비스 수행 날짜 | LocalDate | - | X | `2026-09-03` |
| startedAt | 서비스 시작 일시 | LocalDateTime | - | X | `2026-09-03T10:00:00` |
| matchedAt | 매칭 확정 일시 | Instant | - | X | `2026-08-29T10:00:00Z` |

> `p_service_schedules`의 NOT NULL 컬럼인 `finished_at`에 대응하는 필드가 페이로드에 없어 schedule은 **소요 시간 1시간 고정**(`ServiceMatchingCommandService.DEFAULT_SERVICE_DURATION`)으로 `finishedAt = startedAt + 1h`를 계산한다. 이는 임시방편이 아니라 **양쪽이 같은 값을 쓰고 있는 상태**다 — provider-service도 `MatchingService.SERVICE_HOURS = 1`로 1시간 단위로만 배정하고, 비어 있는 시간을 찾을 때도 1시간이 들어가는지로 판정한다. 따라서 두 서비스가 인식하는 일정 길이는 어긋나지 않는다.
>
>
> 다만 상수가 두 서비스에 흩어져 있으므로, **서비스 종류별 소요 시간이 도입되면 페이로드에 `finishedAt`을 추가하고 양쪽 상수를 함께 제거**해야 한다.
>

> ℹ️ **참고 (provider-service 매칭 규칙)**: `startedAt`은 provider가 `date.atTime(match.startedAt())`로 만든다. 후보는 제공 가능 시간(`p_provide_works`)과 희망 시간대(`MORNING` 09:00~13:00 / `AFTERNOON` 13:00~18:00, 미지정 시 09:00~18:00 전체)의 교집합에서 기존 일정을 뺀 뒤 1시간이 들어가는 가장 빠른 시각이며, 일정이 몰리지 않도록 배정 건수가 적은 제공자를 우선한다.
>

### 일정 생성 시 불변식

`ServiceSchedule.confirm`이 검증하며, 위반 시 `400 INVALID_PARAMETER`가 발생해 재시도 소진 후 메시지가 DLQ로 이동한다. 대부분 발행 측 버그이므로 원본 메시지를 보존해 조사한다.

- `date`는 **오늘 이후**여야 한다 (당일 일정 생성 불가)
- `startedAt`/`finishedAt`은 `date`와 같은 날짜여야 한다
- `finishedAt > startedAt`

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
| 신규 매칭 | `p_service_matching_attempts`에 `MATCHED` 이력 추가 + `p_service_schedules`에 새 일정 생성(`SCHEDULED`) |
| 재매칭 | 위에 더해, 기존 `RESCHEDULING` 일정을 `CHANGED`로 전이 |
| 중복 수신 | 아무 작업도 하지 않고 로그만 남김 |
| 동일 이벤트 동시 수신 | 한쪽만 반영되고, 늦은 쪽은 유니크 인덱스에 막혀 롤백 → 에러 로그 후 재시도 소진 시 DLQ (이중 기록 없음) |
