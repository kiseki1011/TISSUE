package com.tissue.config;

import com.tissue.security.adapter.web.annotation.PublicApi;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Collections;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI tissueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tissue API")
                        .description("Issue management and collaboration in the terminal")
                        .version("v0.7.0")
                        .license(new License().name("GPL-3.0").url("https://www.gnu.org/licenses/gpl-3.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer publicApiSecurityCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType().isAnnotationPresent(PublicApi.class)
                    || handlerMethod.hasMethodAnnotation(PublicApi.class)) {
                operation.setSecurity(Collections.emptyList());
            }
            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> openApi.setTags(List.of(
                new Tag().name("Authentication").description("Authentication and token management"),
                new Tag().name("Member Signup").description("Member registration and email verification"),
                new Tag().name("Password Reset").description("Password reset via email verification"),
                new Tag().name("Member Account").description("Account management for authenticated members"),
                new Tag().name("Member Profile").description("Member profile management"),
                new Tag().name("Workspace").description("Workspace management"),
                new Tag().name("Workspace Member").description("Workspace member management"),
                new Tag().name("Workspace Invite Link").description("Workspace invite link management and joining"),
                new Tag().name("Workspace Participation").description("Workspace invitation and leaving"),
                new Tag().name("Invitation").description("Current user's invitation management"),
                new Tag().name("Position").description("Position management within a workspace"),
                new Tag().name("Team").description("Team management within a workspace"),
                new Tag().name("Project").description("Project management within a workspace"),
                new Tag().name("Project Member").description("Project member management"),
                new Tag().name("Custom Issue Type").description("Custom issue type management within a project"),
                new Tag().name("Custom Issue Field").description("Custom field management for issue types"),
                new Tag().name("Workflow").description("Workflow management within a project"),
                new Tag().name("Sprint").description("Sprint management within a project"),
                new Tag().name("Issue").description("Issue operations"),
                new Tag().name("Issue Attachment").description("File attachment management on issues"),
                new Tag().name("Comment").description("Comment management on issues"),
                new Tag().name("Tag").description("Tag management within a project"),
                new Tag().name("Activity Log").description("Activity log for issues and sprints"),
                new Tag().name("Notification").description("Current user's notification management"),
                new Tag()
                        .name("Notification Preference")
                        .description("Current user's notification preference management"),
                new Tag().name("GitHub Integration").description("GitHub VCS integration management for workspaces"),
                new Tag().name("System Info").description("Server configuration and system information")));
    }
}
