package com.tissue.workspace.adapter.in.web.resolver;

import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.workspace.adapter.in.web.annotation.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;
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
public class WorkspaceMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final WorkspaceMemberFinder workspaceMemberFinder;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentWorkspaceMember.class)
                && parameter.getParameterType().equals(WorkspaceMemberInfo.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        String workspaceKey = getWorkspaceKey(webRequest);
        Long memberId = getMemberId();

        // TODO: WorkspaceMemberQueryRepository에 의존하는게 더 좋을까? 아니면 지금도 ㄱㅊ?
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(memberId, workspaceKey);

        return WorkspaceMemberInfo.from(workspaceMember);
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
