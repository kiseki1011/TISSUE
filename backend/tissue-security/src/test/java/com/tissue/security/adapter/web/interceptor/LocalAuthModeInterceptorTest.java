package com.tissue.security.adapter.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.security.config.TissueAuthProperties;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.exception.base.ForbiddenException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class LocalAuthModeInterceptorTest {

    private LocalAuthModeInterceptor interceptor(TissueAuthProperties.Mode mode) {
        TissueAuthProperties properties = new TissueAuthProperties();
        properties.setMode(mode);
        return new LocalAuthModeInterceptor(properties);
    }

    private HandlerMethod handler(Object bean, String method) throws NoSuchMethodException {
        Method m = bean.getClass().getMethod(method);
        return new HandlerMethod(bean, m);
    }

    @Test
    @DisplayName("When OIDC mode, @LocalAuthOnly endpoint is rejected")
    void oidcRejectsMethodAnnotated() throws Exception {
        LocalAuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.OIDC);
        HandlerMethod handler = handler(new MethodAnnotated(), "annotated");

        assertThatThrownBy(() -> interceptor.preHandle(null, null, handler)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("When OIDC mode, unannotated endpoint passes")
    void oidcAllowsUnannotated() throws Exception {
        LocalAuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.OIDC);
        HandlerMethod handler = handler(new MethodAnnotated(), "plain");

        assertThat(interceptor.preHandle(null, null, handler)).isTrue();
    }

    @Test
    @DisplayName("LOCAL mode: a @LocalAuthOnly endpoint passes")
    void localAllowsAnnotated() throws Exception {
        LocalAuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.LOCAL);
        HandlerMethod handler = handler(new MethodAnnotated(), "annotated");

        assertThat(interceptor.preHandle(null, null, handler)).isTrue();
    }

    static class MethodAnnotated {
        @LocalAuthOnly
        public void annotated() {}

        public void plain() {}
    }
}
