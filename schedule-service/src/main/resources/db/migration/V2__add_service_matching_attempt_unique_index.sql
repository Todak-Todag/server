-- 같은 매칭 결과/실패가 두 번 기록되는 것을 막는 DB 불변식
-- alreadyApplied의 조회와 recordAttempt의 적재 사이에 제약이 없어, 동일 이벤트가 정확히 동시에
-- 들어오면 양쪽 다 조회를 통과해 이력과 일정이 중복 생성됐다. 애플리케이션 체크는 정상 경로의
-- 빠른 skip으로 두고, 그 바깥으로 들어온 중복은 DB가 막는다
--
-- 컬럼 구성은 alreadyApplied의 대체 키와 같아야 하고, status/deleted_at 조건도 두 exists 쿼리와
-- 같아야 한다 (FAILED가 스윕으로 EXPIRED가 되면 조회 대상에서 빠지므로 인덱스에서도 빠져야 함)
CREATE UNIQUE INDEX IF NOT EXISTS ux_p_service_matching_attempts_matched
    ON schedule_schema.p_service_matching_attempts (service_preference_id, service_offering_id, date, matched_at)
    WHERE status = 'MATCHED' AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_p_service_matching_attempts_failed
    ON schedule_schema.p_service_matching_attempts (service_preference_id, date, failed_at)
    WHERE status = 'FAILED' AND deleted_at IS NULL;
