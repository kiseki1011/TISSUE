package com.tissue.security.config;

import com.tissue.security.adapter.web.interceptor.AuthModeInterceptor;
import com.tissue.security.adapter.web.interceptor.RequireEmailInterceptor;
import com.tissue.security.adapter.web.interceptor.WebhookRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityWebMvcConfig implements WebMvcConfigurer {

    private final RequireEmailInterceptor requireEmailInterceptor;
    private final AuthModeInterceptor authModeInterceptor;
    private final WebhookRateLimitInterceptor webhookRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireEmailInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(authModeInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(webhookRateLimitInterceptor)
                .addPathPatterns("/api/v1/projects/*/integrations/*/webhook");
    }
}
