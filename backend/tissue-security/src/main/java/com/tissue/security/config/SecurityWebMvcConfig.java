package com.tissue.security.config;

import com.tissue.security.adapter.web.interceptor.LocalAuthModeInterceptor;
import com.tissue.security.adapter.web.interceptor.RequireEmailInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers security-owned MVC interceptors. Kept inside {@code tissue-security} (rather than the
 * generic web config) so that the merged web layer never needs a compile dependency on this module:
 * {@link RequireEmailInterceptor} and its {@link com.tissue.security.adapter.web.annotation.RequireEmail}
 * annotation stay here, and Spring merges this {@link WebMvcConfigurer} with the others at runtime.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityWebMvcConfig implements WebMvcConfigurer {

    private final RequireEmailInterceptor requireEmailInterceptor;
    private final LocalAuthModeInterceptor localAuthModeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireEmailInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(localAuthModeInterceptor).addPathPatterns("/api/**");
    }
}
