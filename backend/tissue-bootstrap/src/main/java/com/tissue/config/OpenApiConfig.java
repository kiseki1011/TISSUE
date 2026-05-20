package com.tissue.config;

import com.tissue.global.openapi.AuthenticationErrors;
import com.tissue.global.openapi.CommentErrors;
import com.tissue.global.openapi.CommonErrors;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.IssueTypeErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.NotificationErrors;
import com.tissue.global.openapi.PositionErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.ProjectTemplateErrors;
import com.tissue.global.openapi.SprintErrors;
import com.tissue.global.openapi.TagErrors;
import com.tissue.global.openapi.TeamErrors;
import com.tissue.global.openapi.VcsErrors;
import com.tissue.global.openapi.WikiErrors;
import com.tissue.global.openapi.WorkflowErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.config.SystemProperties;
import com.tissue.shared.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;

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
                            Tissue (Terminal-Issue) is an open source issue management and collaboration software.
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

    /**
     * Picks up enum error annotations on a controller method, groups the
     * declared {@link ErrorCode} values by HTTP status, and appends a Markdown
     * bullet list to the matching {@code ApiResponse} description.
     *
     * <pre>
     *  "`XXX_ERROR_CODE`: {description}"
     * </pre>
     *
     * <p>Each supported annotation is registered explicitly via an
     * {@link #addIfPresent} call below. To support a new error code enum:
     * <ol>
     *   <li>Create a paired {@code @XxxErrors} annotation with
     *       {@code XxxErrorCode[] value()}.</li>
     *   <li>Add a dedicated {@code addIfPresent} line here.</li>
     * </ol>
     */
    @Bean
    public OperationCustomizer apiErrorsCustomizer() {
        return (operation, handlerMethod) -> {
            List<ErrorCode> codes = new ArrayList<>();
            addIfPresent(handlerMethod, WorkspaceErrors.class, WorkspaceErrors::value, codes);
            addIfPresent(handlerMethod, MemberErrors.class, MemberErrors::value, codes);
            addIfPresent(handlerMethod, ProjectErrors.class, ProjectErrors::value, codes);
            addIfPresent(handlerMethod, PositionErrors.class, PositionErrors::value, codes);
            addIfPresent(handlerMethod, TeamErrors.class, TeamErrors::value, codes);
            addIfPresent(handlerMethod, ProjectTemplateErrors.class, ProjectTemplateErrors::value, codes);
            addIfPresent(handlerMethod, IssueErrors.class, IssueErrors::value, codes);
            addIfPresent(handlerMethod, IssueTypeErrors.class, IssueTypeErrors::value, codes);
            addIfPresent(handlerMethod, CommentErrors.class, CommentErrors::value, codes);
            addIfPresent(handlerMethod, TagErrors.class, TagErrors::value, codes);
            addIfPresent(handlerMethod, SprintErrors.class, SprintErrors::value, codes);
            addIfPresent(handlerMethod, WorkflowErrors.class, WorkflowErrors::value, codes);
            addIfPresent(handlerMethod, WikiErrors.class, WikiErrors::value, codes);
            addIfPresent(handlerMethod, NotificationErrors.class, NotificationErrors::value, codes);
            addIfPresent(handlerMethod, VcsErrors.class, VcsErrors::value, codes);
            addIfPresent(handlerMethod, AuthenticationErrors.class, AuthenticationErrors::value, codes);
            addIfPresent(handlerMethod, CommonErrors.class, CommonErrors::value, codes);
            // Add a new addIfPresent for a new error enum

            if (codes.isEmpty()) {
                return operation;
            }
            applyErrorCodesToResponses(operation, codes);
            return operation;
        };
    }

    private static <A extends Annotation, E extends ErrorCode> void addIfPresent(
            HandlerMethod handlerMethod, Class<A> annotationType, Function<A, E[]> getter, List<ErrorCode> dest) {
        A anno = handlerMethod.getMethodAnnotation(annotationType);
        if (anno != null) {
            Collections.addAll(dest, getter.apply(anno));
        }
    }

    private static void applyErrorCodesToResponses(
            io.swagger.v3.oas.models.Operation operation, List<ErrorCode> codes) {
        Map<HttpStatus, List<ErrorCode>> byStatus = codes.stream()
                .collect(Collectors.groupingBy(ErrorCode::getHttpStatus, LinkedHashMap::new, Collectors.toList()));

        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        final ApiResponses responses = operation.getResponses();

        byStatus.forEach((status, statusCodes) -> {
            String generated = statusCodes.stream()
                    .map(c -> "- `" + c.name() + "`: " + c.getDefaultMessage())
                    .collect(Collectors.joining("\n"));

            ApiResponse resp = responses.computeIfAbsent(
                    String.valueOf(status.value()), k -> new ApiResponse().description(status.getReasonPhrase()));

            String existing = resp.getDescription();
            String base = (existing == null || existing.isBlank()) ? status.getReasonPhrase() : existing;
            resp.setDescription(base + "\n\n" + generated);
        });
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
