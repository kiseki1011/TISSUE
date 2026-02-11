package com.tissue.notification.template;

import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class ThymeleafNotificationTemplateRenderer implements NotificationTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    /**
     * Renders a string template using Apache Commons Text.
     *
     * <p>Use ${key} syntax in templates.</p>
     *
     * <p><strong>Example:</strong>
     *
     * <pre>
     *     "Issue ${issueKey} created by ${actorName}"
     * </pre>
     */
    @Override
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
    @Override
    public String renderHtml(String templateName, Map<String, Object> data) {
        Context context = new Context();
        if (data != null) {
            context.setVariables(data);
        }
        return templateEngine.process(templateName, context);
    }
}
