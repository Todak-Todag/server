# Care Plan Service - Improvements

지금 당장 장애로 이어지지는 않지만, 코드 품질·가독성·유지보수성·운영 편의성 관점에서 개선하면 좋은 항목이다.
`TECH_DEBT.md`와 겹치는 항목(트랜잭션/Feign/이벤트 신뢰성)은 여기서 다루지 않는다.

---

## 1. CarePlanQueryService에서 CarePlanOwnerValidator 미사용 (중복 코드 + 미사용 필드)

### 우선순위
MEDIUM

### 대상
- `CarePlanQueryService.findCarePlan()`, `validateOwner()` (private, `// TODO : 추후 분리` 주석 포함)
- `CarePlanQueryService`에 주입된 `carePlanOwnerValidator` 필드 (실제 사용처 없음)
- 비교 대상: `CarePlanServiceQueryService`, `ServicePreferenceQueryService`, `CarePlanServiceCommandService`, `ServicePreferenceCommandService`는 모두 `CarePlanOwnerValidator.validate(userId, patientId)`를 사용

### 현재 구조
`CarePlanQueryService`는 `CarePlanOwnerValidator`를 필드로 주입받지만 실제로는 호출하지 않고, 동일한 로직(`!carePlan.getPatientId().equals(userId)` → `AUTH_FORBIDDEN`)을 `validateOwner()`라는 private 메서드로 다시 구현해 사용 중이다. 코드에도 "추후 분리" TODO가 남아 있어 작성자도 이 상태를 임시로 인지하고 있었던 것으로 보인다.

### 문제 또는 개선 이유
동일한 소유자 검증 로직이 두 곳에 존재해 한쪽만 수정되면(예: 정책 변경, ADMIN 예외 추가 등) 나머지 한쪽은 그대로 남아 검증 로직이 서비스 계층별로 갈라질 위험이 있다. 주입된 필드가 사용되지 않는 것도 읽는 사람에게 혼란을 준다.

### 개선 방향
`CarePlanQueryService.findCarePlan()`에서 `validateOwner()` 대신 이미 주입된 `carePlanOwnerValidator.validate(userId, carePlan.getPatientId())`를 호출하도록 바꾸고, private `validateOwner()` 메서드는 제거한다.

---

## 2. "planService/preference → carePlan 조회 → 소유자 검증" 체인이 여러 메서드에 반복

### 우선순위
LOW

### 대상
- `ServicePreferenceCommandService.createServicePreference()`, `updateServicePreference()`, `deleteServicePreference()`
- `CarePlanServiceCommandService.cancelCarePlanService()`
- `ServicePreferenceQueryService.findServicePreference()`

### 현재 구조
위 5개 메서드 모두 "`CarePlanService`(또는 `CarePlanServicePreference`)를 id로 조회 → 그 안의 `carePlanId`로 `CarePlan` 조회 → `carePlanOwnerValidator.validate()`" 순서를 각자 메서드 안에서 동일하게 반복 구현하고 있다.

### 문제 또는 개선 이유
로직 자체는 잘못되지 않았지만, 동일한 3단계 조회/검증 코드가 5곳에 복사되어 있어 예를 들어 소유자 검증 예외 처리 방식이 바뀌면 5곳을 모두 찾아 고쳐야 한다. 메서드 하나하나는 짧지만 반복으로 인해 서비스 클래스 전체 가독성이 떨어진다.

### 개선 방향
"planServiceId 또는 servicePreferenceId로 CarePlan을 찾고 소유자를 검증한 뒤 반환"하는 공통 조회 헬퍼(예: 기존 `CarePlanOwnerValidator` 옆에 작은 resolver 컴포넌트, 혹은 각 커맨드 서비스 내부의 private 메서드 통합)를 만들어 재사용한다. 과도한 추상화(인터페이스화 등)까지는 필요 없고, 반복되는 3줄짜리 조회 체인을 메서드 하나로 묶는 정도로 충분하다.

---

## 3. ServicePreferenceCommandService의 상태 검증 메서드 두 개가 사실상 동일

### 우선순위
LOW

### 대상
- `ServicePreferenceCommandService.validateCarePlanStatus()` (생성/수정용, `SERVICE_PREFERENCE_NOT_ALLOWED`)
- `ServicePreferenceCommandService.validateCarePlanStatusForDelete()` (삭제용, `SERVICE_PREFERENCE_DELETE_NOT_ALLOWED`)

### 현재 구조
두 메서드 모두 `carePlan.getStatus() != CarePlanStatus.UNDER_REVIEW`를 검사하는 동일한 조건이며, 차이는 던지는 `ErrorCode`뿐이다.

### 문제 또는 개선 이유
검증 조건 자체(UNDER_REVIEW 여부)가 바뀌면 두 메서드를 모두 수정해야 한다는 점만 제외하면 심각한 문제는 아니지만, 하나의 조건에 이름이 다른 메서드 두 개가 존재하는 것은 불필요한 중복이다.

### 개선 방향
조건 검사를 공통 private 메서드로 묶고 `ErrorCode`만 파라미터로 받도록 정리한다(예: `validateUnderReview(carePlan, errorCode)`).

---

## 4. completeCarePlan의 이벤트 검증 로직 분리 필요 (코드 내 TODO 명시)

### 우선순위
MEDIUM

### 대상
- `CarePlanCommandService.validateCompletedEvent()` (`// TODO : validate 분리 시급` 주석)

### 현재 구조
`ScheduleStatus.CANCELED`가 아니면 `serviceResultId`가 null인지만 확인하는 단순한 검증이 `CarePlanCommandService` 내부 private 메서드로 존재한다. 작성자가 직접 "분리 시급"이라는 TODO를 남겨둔 상태다.

### 문제 또는 개선 이유
지금은 검증 항목이 하나뿐이라 문제가 드러나지 않지만, 향후 완료 이벤트 검증 항목이 늘어나면 커맨드 서비스 안에 검증 로직이 계속 쌓여 책임이 커진다.

### 개선 방향
`CarePlanOwnerValidator`, `ServicePreferenceDateValidator`처럼 별도의 `support` 패키지 validator(예: `CarePlanCompletedEventValidator`)로 분리한다. 지금 당장 급한 리팩터링은 아니며, 검증 항목이 하나 더 추가되는 시점에 함께 진행해도 무방하다.

---

## 5. 도메인 계층에 비즈니스 이벤트 로깅 부재

### 우선순위
LOW

### 대상
- `CarePlanCommandService` (상태 전이, Care Plan 생성/삭제)
- `CarePlanEventPublisher` / `CarePlanEventConsumer` (이벤트 발행/소비)

### 현재 구조
현재 로그는 `GlobalExceptionHandler`(예외 발생 시 warn/error)와 `InternalApiKeyInterceptor`(인증 실패 시 warn) 정도에만 존재한다. Care Plan 상태 전이, 이벤트 발행/소비 성공 등 정상 흐름에 대한 비즈니스 로그는 없다.

### 문제 또는 개선 이유
운영 중 "특정 Care Plan이 언제 CONFIRMED 되었는지", "이벤트가 언제 발행/소비되었는지"를 추적하려면 현재는 DB 감사 컬럼(`updatedAt` 등) 외에는 근거가 없다. 장애 상황을 사후에 재구성하기 어렵다.

### 개선 방향
상태 전이(`updateCarePlanStatus`), 이벤트 발행(`CarePlanEventPublisher`), 이벤트 소비(`CarePlanEventConsumer`) 시점에 `carePlanId` 등 식별자를 포함한 info 레벨 로그를 추가한다. 로깅 프레임워크 교체나 구조화 로깅 도입까지는 필요 없고, 기존 `@Slf4j` + 핵심 식별자 로그면 충분하다.

---

## 6. 외부 서비스 호출 실패 시 예외가 뭉뚱그려 500으로 처리됨

### 우선순위
LOW

### 대상
- `GlobalExceptionHandler` (Feign 관련 전용 핸들러 없음, `Exception.class` 핸들러로 흡수됨)
- `DischargeClientAdapter`, `UserClientAdapter`, `ProviderServiceClientAdapter`, `ScheduleResultClientAdapter`

### 현재 구조
Feign 호출이 실패(`FeignException`, 연결 실패 등)하면 별도 핸들링 없이 `GlobalExceptionHandler.handleException(Exception e)`로 흡수되어 `COMMON_INTERNAL_SERVER_ERROR`(500)로 응답한다.

### 문제 또는 개선 이유
클라이언트 입장에서 "care-plan-service 자체 오류"와 "연동 서비스 장애로 인한 오류"를 구분할 수 없다. 이미 `PROVIDER_SERVICE_DATA_MISMATCH`처럼 502를 쓰는 사례가 있으므로, 외부 서비스 호출 실패도 유사하게 502/503으로 구분하면 원인 파악이 쉬워진다.

### 개선 방향
`GlobalExceptionHandler`에 `FeignException` 전용 핸들러를 추가해 `HttpStatus.BAD_GATEWAY` 계열로 응답하고, 로그에 어떤 서비스 호출이었는지 남긴다. 전체 예외 구조를 바꾸는 대규모 작업은 아니고 핸들러 하나 추가로 충분하다.

---

## 7. PageableFactory의 sort 파라미터가 실제로는 방향(direction)만 반영

### 우선순위
LOW

### 대상
- `PageableFactory.resolveSort(String sort)`

### 현재 구조
`sort` 문자열을 `,`로 나눠 `parts[1]`(방향)만 사용하고, `parts[0]`(정렬 필드명)은 무시한 채 항상 `SORT_PROPERTY = "createdAt"`로 고정 정렬한다.

### 문제 또는 개선 이유
호출부에서 `"createdAt,desc"`처럼 필드명을 명시적으로 넘기고 있어 지금 당장 오작동은 아니지만, 메서드 시그니처만 보면 임의의 필드로 정렬 가능한 것처럼 보여 향후 다른 필드명으로 호출하면(예: `"updatedAt,asc"`) 조용히 무시되고 `createdAt` 기준으로 정렬되는 혼란을 줄 수 있다.

### 개선 방향
정렬 필드가 항상 고정이라면 메서드 파라미터를 `String sort` 대신 `Sort.Direction` 하나만 받도록 시그니처를 단순화하거나, 최소한 필드명 파라미터를 실제로 사용하도록 반영한다.

---

## 8. 테스트 보강 여지

### 우선순위
LOW

### 대상
- `presentation/controller/api/*Controller` (컨트롤러 계층 테스트 없음 — `@PreAuthorize` 권한 매핑, 요청 검증(`@Valid`) 등은 서비스 단위 테스트로는 커버되지 않음)
- `CarePlanEventConsumer` (성공 케이스 통합 테스트만 존재, 예외 발생 시 재큐잉/DLQ 동작을 확인하는 테스트 없음)

### 현재 구조
`application/service`, `facade`, 이벤트 발행/소비 성공 시나리오에 대한 테스트는 이미 잘 갖춰져 있다(`CarePlanCommandServiceTest`, `CarePlanConfirmedEventPublishIntegrationTest`, `CarePlanCompletedEventConsumeIntegrationTest` 등). 다만 컨트롤러 계층과 컨슈머 실패 경로는 테스트가 없다.

### 문제 또는 개선 이유
`@PreAuthorize` 롤 매핑이나 요청 DTO의 `@Valid` 제약은 컨트롤러 슬라이스 테스트(`@WebMvcTest`) 없이는 리팩터링 중 조용히 깨지기 쉽다. 위 `TECH_DEBT.md` 3번(재시도/DLQ) 개선을 진행할 때도 실패 경로 테스트가 있어야 회귀를 방지할 수 있다.

### 개선 방향
전체 컨트롤러를 한 번에 커버하기보다, 권한 검증이 실제로 중요한 엔드포인트(상태 변경, 삭제) 위주로 `@WebMvcTest` 기반 테스트를 우선 추가한다. DLQ 도입 시점에 맞춰 컨슈머 실패 경로 테스트를 함께 추가한다.
