package com.tissue.global.webmvc;

import com.tissue.global.logging.MdcContextInterceptor;
import com.tissue.security.adapter.web.interceptor.RequireEmailInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MdcContextInterceptor mdcContextInterceptor;
    private final RequireEmailInterceptor requireEmailInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mdcContextInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(requireEmailInterceptor).addPathPatterns("/api/**");
    }
}
