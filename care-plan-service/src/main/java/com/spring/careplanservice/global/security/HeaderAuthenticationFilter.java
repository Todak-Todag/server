package com.spring.careplanservice.global.security;

import com.spring.careplanservice.global.common.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");
        String userRoleHeader = request.getHeader("X-user-Role");

        /*
         * Gateway 사용자 인증 헤더가 없는 요청은
         * 인증 정보를 생성하지 않고
         * 다음 필터로 전달
         */
        if (userIdHeader == null || userRoleHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);
            UserRole userRole = UserRole.valueOf(userRoleHeader);

            UserContext userContext = new UserContext(userId, userRole);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userContext,
                    null,
                    List.of()
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (IllegalArgumentException e) {
            // UUID 또는 Role 형식이 잘못된 경우 인증 정보를 생성하지 않음
        }

        filterChain.doFilter(request, response);
    }
}
