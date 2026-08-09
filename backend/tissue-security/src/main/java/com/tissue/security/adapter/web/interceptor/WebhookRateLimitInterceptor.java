package com.tissue.security.adapter.web.interceptor;

import com.tissue.security.application.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Caps how fast anyone can hit the inbound webhook endpoints.
 *
 * <p>Those endpoints are the only unauthenticated write path into a project, and rejecting a bad caller
 * still costs a database lookup and an HMAC computation, so the cost is worth shedding before the
 * controller runs.
 *
 * <p>Keys on {@code getRemoteAddr()}, matching how login rate limiting identifies a caller. Behind a
 * reverse proxy that address is the proxy's, so all webhook traffic shares one bucket; the ceiling is set
 * high enough that real traffic never reaches it.
 */
@Component
@RequiredArgsConstructor
public class WebhookRateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        rateLimitService.checkWebhookRateLimit(request.getRemoteAddr());

        return true;
    }
}
