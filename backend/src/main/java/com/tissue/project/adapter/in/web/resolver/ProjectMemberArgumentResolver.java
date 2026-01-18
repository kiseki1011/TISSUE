package com.tissue.project.adapter.in.web.resolver;

import com.tissue.project.application.dto.ProjectMemberInfo;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.ProjectMember;
import com.tissue.security.authentication.domain.MemberDetails;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class ProjectMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final ProjectMemberFinder projectMemberFinder;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentProjectMember.class)
                && parameter.getParameterType().equals(ProjectMemberInfo.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory)
            throws Exception {

        String workspaceKey = getWorkspaceKey(webRequest);
        String projectKey = getProjectKey(webRequest);
        Long memberId = getMemberId();

        ProjectMember projectMember = projectMemberFinder.getActiveBy(workspaceKey, projectKey,
            memberId);

        return ProjectMemberInfo.from(projectMember);
    }

    @SuppressWarnings("unchecked")
    private String getWorkspaceKey(NativeWebRequest webRequest) {
        Map<String, String> pathVariables = (Map<String, String>) webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (pathVariables == null || !pathVariables.containsKey("workspaceKey")) {
            throw new IllegalStateException("Missing path variable 'workspaceKey' for @CurrentWorkspaceMember");
        }

        return pathVariables.get("workspaceKey");
    }

    @SuppressWarnings("unchecked")
    private String getProjectKey(NativeWebRequest webRequest) {
        Map<String, String> pathVariables = (Map<String, String>) webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (pathVariables == null || !pathVariables.containsKey("projectKey")) {
            throw new IllegalStateException("Missing path variable 'projectKey' for @CurrentProjectMember");
        }

        return pathVariables.get("projectKey");
    }

    private Long getMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof MemberDetails)) {
            throw new IllegalStateException("User is not authenticated or invalid principal type");
        }

        return ((MemberDetails) authentication.getPrincipal()).getMemberId();
    }
}
