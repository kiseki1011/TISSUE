package com.tissue.security.adapter.web.interceptor;

import com.tissue.security.adapter.web.annotation.RequireEmail;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequireEmailInterceptor implements HandlerInterceptor {

    private final TissueSecurityProperties tissueSecurityProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean required = handlerMethod.hasMethodAnnotation(RequireEmail.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireEmail.class);

        if (required && !tissueSecurityProperties.isEmailRequired()) {
            throw new BadRequestException(AuthenticationErrorCode.EMAIL_FEATURE_DISABLED);
        }

        return true;
    }
}
