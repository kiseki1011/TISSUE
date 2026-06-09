package com.tissue.security.adapter.web.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.security.config.TissueAuthProperties;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.auth.OidcAuthOnly;
import com.tissue.shared.exception.base.ForbiddenException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class AuthModeInterceptorTest {

    private AuthModeInterceptor interceptor(TissueAuthProperties.Mode mode) {
        TissueAuthProperties properties = new TissueAuthProperties();
        properties.setMode(mode);
        return new AuthModeInterceptor(properties);
    }

    private HandlerMethod handler(Object bean, String method) throws NoSuchMethodException {
        Method m = bean.getClass().getMethod(method);
        return new HandlerMethod(bean, m);
    }

    @Test
    @DisplayName("OIDC mode rejects a @LocalAuthOnly endpoint")
    void oidcRejectsLocalOnly() throws Exception {
        AuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.OIDC);
        HandlerMethod handler = handler(new Endpoints(), "localOnly");

        assertThatThrownBy(() -> interceptor.preHandle(null, null, handler)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("OIDC mode allows a @OidcAuthOnly endpoint")
    void oidcAllowsOidcOnly() throws Exception {
        AuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.OIDC);
        HandlerMethod handler = handler(new Endpoints(), "oidcOnly");

        assertThat(interceptor.preHandle(null, null, handler)).isTrue();
    }

    @Test
    @DisplayName("LOCAL mode rejects a @OidcAuthOnly endpoint")
    void localRejectsOidcOnly() throws Exception {
        AuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.LOCAL);
        HandlerMethod handler = handler(new Endpoints(), "oidcOnly");

        assertThatThrownBy(() -> interceptor.preHandle(null, null, handler)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("LOCAL mode allows a @LocalAuthOnly endpoint")
    void localAllowsLocalOnly() throws Exception {
        AuthModeInterceptor interceptor = interceptor(TissueAuthProperties.Mode.LOCAL);
        HandlerMethod handler = handler(new Endpoints(), "localOnly");

        assertThat(interceptor.preHandle(null, null, handler)).isTrue();
    }

    @Test
    @DisplayName("an unannotated endpoint passes in both modes")
    void unannotatedPasses() throws Exception {
        HandlerMethod handler = handler(new Endpoints(), "plain");

        assertThat(interceptor(TissueAuthProperties.Mode.OIDC).preHandle(null, null, handler))
                .isTrue();
        assertThat(interceptor(TissueAuthProperties.Mode.LOCAL).preHandle(null, null, handler))
                .isTrue();
    }

    static class Endpoints {
        @LocalAuthOnly
        public void localOnly() {}

        @OidcAuthOnly
        public void oidcOnly() {}

        public void plain() {}
    }
}
