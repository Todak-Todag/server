package com.todak_todag.schedule_service.global.security;

import com.todak_todag.schedule_service.global.common.SystemId;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

// JPA Auditing(@CreatedBy/@LastModifiedBy)이 현재 요청 주체를 조회할 때 사용하는 구현체
// - 외부 API: API Gateway를 거쳐 X-User-Id/X-User-Role이 오면 HeaderAuthenticationFilter가
//   SecurityContext에 UserContext를 채워두므로 그 userId를 사용
// - 내부 API(/internal/v1/**)처럼 X-User-Id가 없는 요청(SecurityContext의 principal이
//   UserContext가 아닌 경우 (ex: 인증되지 않은 요청) 은 SystemId.SYSTEM_USER_ID로 기록
public class SpringSecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserContext userContext) {
            return Optional.of(userContext.getUserId());
        }

        return Optional.of(SystemId.SYSTEM_USER_ID);
    }
}
