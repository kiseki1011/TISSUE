package com.tissue.config;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.config.SystemProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    private final SystemProperties systemProperties;

    @Bean
    public OpenAPI tissueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tissue API")
                        .contact(new Contact()
                                .name("Email")
                                .email("kimseungki1011@gmail.com")
                                .url("https://github.com/kiseki1011/TISSUE"))
                        .description("""
                            Tissue (Terminal Issue) is a free and open source, TUI(Terminal User Interface) \
                            issue management and collaboration software.
                            This is the documentation for the Tissue HTTP API.
                            """)
                        .version(systemProperties.getVersion())
                        .license(new License().name("GPL-3.0").url("https://www.gnu.org/licenses/gpl-3.0.html"))
                        .extensions(Map.of(
                                "x-logo",
                                Map.of(
                                        "url", "/logo-horizontal-lower.svg",
                                        "altText", "Tissue Logo"))))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .extensions(Map.of(
                        "x-tagGroups",
                        List.of(
                                // spotless:off
                                Map.of(
                                        "name",
                                        "Guide",
                                        "tags",
                                        List.of(
                                                "Resource Scoping",
                                                "Authorization",
                                                "Issue Hierarchy",
                                                "Issue Relation",
                                                "Partial Update",
                                                "Error")),
                                Map.of(
                                        "name",
                                        "Authentication",
                                        "tags",
                                        List.of(
                                                "Authentication",
                                                "Member Signup",
                                                "Password Reset")),
                                Map.of(
                                        "name",
                                        "Member Account",
                                        "tags",
                                        List.of(
                                                "Member Account",
                                                "Member Profile")),
                                Map.of(
                                        "name",
                                        "Workspace",
                                        "tags",
                                        List.of(
                                                "Workspace",
                                                "Workspace Member",
                                                "Workspace Invite Link",
                                                "Workspace Participation",
                                                "Invitation",
                                                "Position",
                                                "Team",
                                                "Project Template")),
                                Map.of(
                                        "name",
                                        "Project",
                                        "tags",
                                         List.of(
                                                 "Project",
                                                 "Project Member")),
                                Map.of(
                                        "name",
                                        "Issue",
                                        "tags",
                                        List.of(
                                                "Issue",
                                                "Issue Attachment",
                                                "Comment",
                                                "Tag",
                                                "Activity Log")),
                                Map.of(
                                        "name",
                                        "Issue Configuration",
                                        "tags",
                                        List.of(
                                                "Custom Issue Type",
                                                "Custom Issue Field",
                                                "Workflow")),
                                Map.of(
                                        "name",
                                        "Wiki",
                                        "tags",
                                        List.of(
                                                "Wiki Document",
                                                "Wiki Attachment")),
                                Map.of(
                                        "name",
                                        "Sprint",
                                        "tags",
                                        List.of(
                                            "Sprint")),
                                Map.of(
                                        "name",
                                        "Notification",
                                        "tags",
                                        List.of(
                                                "Notification",
                                                "Notification Preference")),
                                Map.of(
                                        "name",
                                        "VCS",
                                        "tags",
                                        List.of(
                                            "GitHub Integration")),
                                Map.of(
                                        "name",
                                        "System",
                                        "tags",
                                        List.of(
                                            "System Info")))));
                                // spotless:on
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
                new Tag().name("Resource Scoping").description(loadMarkdown("docs/resource-scoping.md")),
                new Tag().name("Authorization").description(loadMarkdown("docs/authorization.md")),
                new Tag().name("Issue Hierarchy").description(loadMarkdown("docs/issue-hierarchy.md")),
                new Tag().name("Issue Relation").description(loadMarkdown("docs/issue-relation.md")),
                new Tag().name("Partial Update").description(loadMarkdown("docs/partial-update.md")),
                new Tag().name("Error").description(loadMarkdown("docs/error.md")),
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
                new Tag().name("Project Template").description("Project template management within a workspace"),
                new Tag().name("Project").description("Project management within a workspace"),
                new Tag().name("Project Member").description("Project member management"),
                new Tag().name("Custom Issue Type").description("Custom issue type management within a project"),
                new Tag().name("Custom Issue Field").description("Custom field management for issue types"),
                new Tag().name("Workflow").description("Workflow management within a project"),
                new Tag().name("Sprint").description("Sprint management within a project"),
                new Tag().name("Issue").description("Issue operations"),
                new Tag().name("Issue Attachment").description("File management on issues"),
                new Tag().name("Comment").description("Comment management on issues and personal comment history"),
                new Tag().name("Tag").description("Tag management within a project"),
                new Tag().name("Activity Log").description("Activity log for issues and sprints"),
                new Tag().name("Wiki Document").description("Wiki management within a workspace"),
                new Tag().name("Wiki Attachment").description("File management on a wiki document"),
                new Tag().name("Notification").description("Current user's notification management"),
                new Tag()
                        .name("Notification Preference")
                        .description("Current user's notification preference management"),
                new Tag().name("GitHub Integration").description("GitHub VCS integration management for workspaces"),
                new Tag().name("System Info").description("Server configuration and system information")));
    }

    private String loadMarkdown(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load documentation: " + path, e);
        }
    }
}
