# Care Plan Service - Technical Debt

현재는 정상 동작하지만, 방치 시 장애·정합성·보안 문제로 이어질 수 있는 항목만 기록한다.
스타일/취향 차이나 이미 잘 처리된 부분(AFTER_COMMIT 이벤트 발행 등)은 제외했다.

---

## 1. Feign 클라이언트 timeout / 서킷브레이커 부재

### 우선순위
HIGH

### 대상
- `global/config/FeignConfig.java`
- `careplan/infrastructure/client/DischargeFeignClient.java`, `ProviderServiceFeignClient.java`, `ScheduleFeignClient.java`, `UserFeignClient.java`
- `src/main/resources/application*.yml`, `config-repo/care-plan-service-*.yml`
- `build.gradle` (resilience4j/circuit breaker 의존성 없음)

### 현재 구조
`FeignConfig`는 내부 API 키 헤더를 붙이는 `RequestInterceptor`만 등록한다. `feign.client.config.*.connectTimeout` / `readTimeout` 설정이 프로젝트 어디에도 없고, resilience4j 등 서킷브레이커 의존성도 추가되어 있지 않다.

### 문제 또는 개선 이유
discharge-service / user-service / provider-service / schedule-service 중 하나가 느려지거나 응답하지 않으면, Feign 기본 타임아웃(수 초~수십 초)까지 호출 스레드가 그대로 대기한다. 아래 2번 항목(트랜잭션 내부 Feign 호출)과 결합되면 그 시간만큼 DB 커넥션도 함께 점유되어, 단일 서비스 장애가 care-plan-service 전체의 커넥션 풀 고갈로 전파될 수 있다(cascading failure). 현재는 실패를 격리할 서킷브레이커도 없어 반복 호출이 계속 같은 지연을 유발한다.

### 개선 방향
`feign.client.config.default.connectTimeout` / `readTimeout`을 짧게(예: 2~3초) 설정한다. 서비스별로 응답 특성이 다르면 client name별 설정을 추가한다. 우선 timeout만 넣어도 충분하며, 서킷브레이커는 장애가 실제로 반복되는 시점에 resilience4j 도입을 검토하는 것으로 미뤄도 된다.

---

## 2. 트랜잭션 내부에서 외부 서비스 HTTP 호출 수행

### 우선순위
HIGH

### 대상
- `CarePlanCommandService.updateCarePlanStatus()` — `@Transactional` 내부에서 CONFIRMED 전이 시 `userQueryPort.findById()` 호출
- `CarePlanCommandService.completeCarePlan()` — `@Transactional` 내부에서 `scheduleResultQueryPort.findById()` 호출 (RabbitMQ 리스너 스레드에서 실행)
- `CarePlanServiceQueryService` — 클래스 레벨 `@Transactional(readOnly = true)` 상태에서 `searchCarePlanServices()` / `findCarePlanService()`가 `providerServiceQueryPort.findAllByIds()` 호출
- `application-local.yml` / `config-repo/care-plan-service-*.yml` (`hikari.maximum-pool-size: 10`)

### 현재 구조
위 메서드들은 DB 트랜잭션(= HikariCP 커넥션 점유)이 열려 있는 상태에서 Feign을 통해 다른 서비스에 네트워크 호출을 보낸다. 반대로 `CarePlanFacade.createCarePlan()`은 `dischargeQueryPort.findById()`를 트랜잭션 시작 **전**(파사드)에서 호출하고, 그 결과만 `@Transactional` 메서드로 넘기도록 이미 분리되어 있어 프로젝트 안에 올바른 패턴이 존재한다.

### 문제 또는 개선 이유
HikariCP `maximum-pool-size`가 10으로 제한되어 있는 상황에서, 외부 서비스 응답 지연 시간만큼 DB 커넥션이 대기 상태로 묶인다. 동시 요청이 10건을 조금만 넘어도(특히 `IN_PROGRESS`→`COMPLETED` 이벤트가 몰리는 시간대) 커넥션 풀이 고갈되어 다른 API 요청까지 전부 타임아웃될 수 있다. 1번 항목(타임아웃 미설정)과 결합되면 영향 시간이 더 길어진다.

### 개선 방향
`CarePlanFacade.createCarePlan()`과 동일한 패턴으로, Feign 조회를 트랜잭션 시작 전(파사드 또는 컨트롤러 계층)으로 옮기고 `@Transactional` 메서드에는 조회 결과만 파라미터로 전달한다. `CarePlanServiceQueryService`도 `carePlanQueryRepository`/`carePlanServiceQueryRepository` 조회와 `providerServiceQueryPort` 조회를 분리해, 후자는 읽기 전용 트랜잭션 바깥에서 수행하도록 재구성한다.

---

## 3. Care Plan 생성/서비스 선택 시 provideServiceId 존재 여부 미검증

### 우선순위
MEDIUM

### 대상
- `CarePlanCommandService.createCarePlan()` (요청받은 `provideServiceIds`를 그대로 `CarePlanService`로 저장)
- `CarePlanServiceCommandService.selectCarePlanService()`
- 대조: `CarePlanServiceQueryService.findProvideServiceInfos()` / `validateAllFound()` — 조회 시점에는 provider-service에 실제로 존재하는지 검증하고 없으면 `PROVIDER_SERVICE_DATA_MISMATCH`(502)를 던짐

### 현재 구조
쓰기 경로(생성/선택)는 클라이언트가 보낸 `provideServiceId`를 provider-service에 존재하는지 확인하지 않고 그대로 저장한다. 반면 읽기 경로는 조회 시마다 provider-service에서 해당 ID들을 조회해 하나라도 없으면 예외를 던진다.

### 문제 또는 개선 이유
잘못되었거나 이미 삭제된 `provideServiceId`로 Care Plan 서비스가 한 번 저장되면, 이후 해당 Care Plan을 조회할 때마다 `PROVIDER_SERVICE_DATA_MISMATCH`가 반복적으로 발생해 정상적인 조회 자체가 막힌다. 쓰기 시점 검증 부재가 읽기 시점 장애로 이어지는 구조다.

### 개선 방향
생성/선택 시점에 `providerServiceQueryPort.findAllByIds()`로 존재 여부를 검증한 뒤 저장하도록 한다(이미 조회 계층에 동일 검증 로직이 있으므로 재사용 가능).

---

## 4. 동시 요청 시 중복 생성 가능 (exists 체크 + DB 유니크 제약 부재)

### 우선순위
MEDIUM

### 대상
- `CarePlanCommandService.validateDuplicateCarePlan()` (`existsByDischargeId`)
- `CarePlanServiceCommandService.validateDuplicateCarePlanService()` (`existsByCarePlanIdAndProvideServiceIdAndCreatedBy`)
- `CarePlan`, `CarePlanService` 엔티티 (DB 유니크 제약 미정의, `ddl-auto: validate`로 운영 스키마는 코드 외부에서 관리)

### 현재 구조
중복 생성 방지는 애플리케이션 레벨의 `exists` 조회 후 `save`하는 방식으로만 이루어지며, 두 동작 사이의 원자성을 보장하는 DB 유니크 제약은 코드 어디에도 정의되어 있지 않다.

### 문제 또는 개선 이유
동일 `dischargeId` 또는 동일 `(carePlanId, provideServiceId, createdBy)` 조합으로 두 요청이 거의 동시에 들어오면, 두 트랜잭션 모두 `exists=false`를 볼 수 있어(TOCTOU) 중복 Care Plan/서비스 항목이 생성될 수 있다. 발생 빈도는 낮지만 한 번 발생하면 데이터 정합성 문제로 이어진다.

### 개선 방향
`discharge_id`, `(care_plan_id, provide_service_id, created_by)`에 DB 유니크 인덱스를 추가해 최후 방어선을 만들고, 제약 위반 시 발생하는 `DataIntegrityViolationException`을 기존 `BusinessException(CARE_PLAN_ALREADY_EXISTS / CARE_PLAN_SERVICE_ALREADY_EXISTS)`로 매핑한다.

---

## 5. 이벤트 소비 멱등성이 상태 가드에 우연히 의존

### 우선순위
LOW

### 대상
- `CarePlanEventConsumer.consumeCarePlanCompleted()`
- `CarePlan.complete()` (`status != IN_PROGRESS`이면 조용히 무시)

### 현재 구조
`CarePlanCompletedEvent`를 중복 수신해도 `complete()`가 `IN_PROGRESS`가 아니면 아무 것도 하지 않으므로 결과적으로 안전하다. 다만 이는 이벤트 자체에 멱등키(예: eventId)가 있어서가 아니라, 우연히 상태 전이 가드 하나로 막히는 구조다.

### 문제 또는 개선 이유
현재는 문제없이 동작하지만, 향후 완료 이벤트 처리에 알림 발송 등 상태와 무관한 부수효과가 추가되면 상태 가드만으로는 멱등성이 보장되지 않는다. 구조적으로 멱등키가 없다는 점 자체가 잠재 위험이다.

### 개선 방향
지금 별도 처리 이력 테이블을 추가하는 것은 과설계이므로 보류하되, 완료 이벤트 처리에 새로운 부수효과가 추가될 때는 반드시 멱등성을 재검토해야 한다는 점을 기록으로 남긴다.

---

## 6. Outbox FAILED 재처리 및 보관 정책 미정

### 우선순위
MEDIUM

### 대상
- `CarePlanOutboxCommandService.recordFailure()`
- `CarePlanOutboxQueryService.findPending()` / `CarePlanOutboxRelayFacade.relay()`
- `p_care_plan_outbox_events` 테이블 (`care_plan_schema`)

### 현재 구조
Outbox 발행이 실패하면 `recordFailure()`가 `retryCount`를 올리며 3회째 실패 시 FAILED로 전환한다. `FAILED` 상태는 `findPending()`(따라서 `CarePlanOutboxRelayFacade.relay()`의 자동 재시도 대상)에서 제외되며, row 자체는 삭제되지 않고 `p_care_plan_outbox_events` 테이블에 그대로 보존된다. 최종 실패 시점에는 `outboxEventId`/`eventType`/`aggregateId`/`retryCount`/`lastErrorMessage`를 담은 ERROR 로그가 남는다.

현재 `FAILED` 이벤트를 되돌리는 수동/자동 재처리 수단이 없고, `SENT`/`FAILED`를 포함한 Outbox row에 대한 보관 기간·삭제(정리) 정책도 없다.

### 문제 또는 개선 이유
`FAILED`로 전환된 이벤트는 로그로만 확인 가능할 뿐 되살릴 방법이 DB 직접 조작 외에는 없다. 또한 처리 완료(`SENT`)된 row까지 포함해 Outbox 테이블에 데이터가 무기한 누적되므로, 장기적으로 테이블/인덱스 크기가 계속 커질 수 있다.

### 개선 방향
- `FAILED` 이벤트 수동/자동 재처리 방식 검토
- `FAILED` 운영 알림/모니터링 검토
- `SENT`/`FAILED` 데이터 보관 기간 정의
- 오래된 Outbox row Bulk Delete 또는 배치 정리 정책 검토
- 실제 운영 중 필요성이 확인될 때 별도 이슈로 구현
