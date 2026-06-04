package com.tissue.security.adapter.web.interceptor;

import com.tissue.security.config.TissueAuthProperties;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.exception.base.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects {@link LocalAuthOnly} endpoints when the instance runs in OIDC mode. In LOCAL mode (the default) it is
 * a no-op, so existing behavior is unchanged. The mode is checked first so non-OIDC instances skip annotation
 * scanning entirely.
 */
@Component
@RequiredArgsConstructor
public class LocalAuthModeInterceptor implements HandlerInterceptor {

    private final TissueAuthProperties authProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (authProperties.getMode() != TissueAuthProperties.Mode.OIDC) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean localOnly = handlerMethod.hasMethodAnnotation(LocalAuthOnly.class)
                || handlerMethod.getBeanType().isAnnotationPresent(LocalAuthOnly.class);

        if (localOnly) {
            throw new ForbiddenException(AuthenticationErrorCode.LOCAL_AUTH_ONLY);
        }

        return true;
    }
}
