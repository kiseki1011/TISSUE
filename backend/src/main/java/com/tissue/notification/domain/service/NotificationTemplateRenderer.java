package com.tissue.notification.domain.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

    private final SpringTemplateEngine templateEngine; // Auto-configured by Spring Boot

    /**
     * Renders a string template using Apache Commons Text.
     * Use ${key} syntax in your templates.
     * e.g., "Issue ${issueKey} created by ${actorName}"
     */
    public String renderString(String template, Map<String, String> data) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (data == null) {
            return template;
        }
        return StringSubstitutor.replace(template, data);
    }

    /**
     * Renders an HTML file template using Thymeleaf.
     * Used for email bodies.
     */
    public String renderHtml(String templateName, Map<String, Object> data) {
        Context context = new Context();
        if (data != null) {
            context.setVariables(data);
        }
        return templateEngine.process(templateName, context);
    }
}
