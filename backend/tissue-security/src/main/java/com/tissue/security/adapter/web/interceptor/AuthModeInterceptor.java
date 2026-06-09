package com.tissue.security.adapter.web.interceptor;

import com.tissue.security.config.TissueAuthProperties;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.auth.OidcAuthOnly;
import com.tissue.shared.exception.base.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Gates endpoints by authentication mode.
 *
 * <p>An endpoint marked {@link LocalAuthOnly} is rejected when the instance runs in OIDC mode.
 * One marked {@link OidcAuthOnly} is rejected in LOCAL mode.
 * Both kinds are only rejected at runtime, so the endpoints for OpenAPI is registered across deployments.
 */
@Component
@RequiredArgsConstructor
public class AuthModeInterceptor implements HandlerInterceptor {

    private final TissueAuthProperties authProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        boolean oidcMode = authProperties.getMode() == TissueAuthProperties.Mode.OIDC;

        if (oidcMode && isAnnotated(handlerMethod, LocalAuthOnly.class)) {
            throw new ForbiddenException(AuthenticationErrorCode.LOCAL_AUTH_ONLY);
        }
        if (!oidcMode && isAnnotated(handlerMethod, OidcAuthOnly.class)) {
            throw new ForbiddenException(AuthenticationErrorCode.OIDC_AUTH_ONLY);
        }
        return true;
    }

    private static boolean isAnnotated(HandlerMethod handlerMethod, Class<? extends Annotation> type) {
        return handlerMethod.hasMethodAnnotation(type)
                || handlerMethod.getBeanType().isAnnotationPresent(type);
    }
}
