package com.tissue.global.webmvc;

import com.tissue.global.logging.MdcContextInterceptor;
import com.tissue.feature.project.web.resolver.ProjectMemberArgumentResolver;
import com.tissue.feature.workspace.web.resolver.WorkspaceMemberArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MdcContextInterceptor mdcContextInterceptor;
    private final WorkspaceMemberArgumentResolver workspaceMemberArgumentResolver;
    private final ProjectMemberArgumentResolver projectMemberArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mdcContextInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(workspaceMemberArgumentResolver);
        resolvers.add(projectMemberArgumentResolver);
    }
}
