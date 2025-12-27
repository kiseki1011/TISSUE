package com.tissue.global.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class MdcContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // && !authentication.getPrincipal().equals("anonymousUser")
        if (authentication != null && authentication.isAuthenticated()) {
            String memberId = authentication.getName();
            MDC.put("memberId", memberId);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables =
                (Map<String, String>)
                        request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (pathVariables != null) {
            if (pathVariables.containsKey("workspaceKey")) {
                MDC.put("workspaceKey", pathVariables.get("workspaceKey"));
            }
            if (pathVariables.containsKey("projectKey")) {
                MDC.put("projectKey", pathVariables.get("projectKey"));
            }
        }

        return true;
    }
}
