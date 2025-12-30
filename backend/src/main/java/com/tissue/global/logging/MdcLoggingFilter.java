package com.tissue.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // TODO: traceId 생성 로직 개선
            String traceId = UUID.randomUUID().toString().substring(0, 8);

            MDC.put("traceId", traceId);
            // MDC.put("clientIp", ClientIpUtils.getClientIp(request));
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}
