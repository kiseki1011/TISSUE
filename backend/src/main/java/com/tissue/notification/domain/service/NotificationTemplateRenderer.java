package com.tissue.notification.domain.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class NotificationTemplateRenderer {

    private final SpringTemplateEngine stringTemplateEngine;
    private final SpringTemplateEngine htmlTemplateEngine;

    public NotificationTemplateRenderer(
            @Qualifier("stringTemplateEngine") SpringTemplateEngine stringTemplateEngine,
            @Qualifier("htmlTemplateEngine") SpringTemplateEngine htmlTemplateEngine) {
        this.stringTemplateEngine = stringTemplateEngine;
        this.htmlTemplateEngine = htmlTemplateEngine;
    }

    /**
     * Renders a string template (e.g., from DB or messages.properties).
     */
    public String renderString(String template, Map<String, String> data) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        return stringTemplateEngine.process(template, createContext(data));
    }

    /**
     * Renders an HTML file template.
     */
    public String renderHtml(String templateName, Map<String, Object> data) {
        Context context = new Context();
        if (data != null) {
            context.setVariables(data);
        }
        return htmlTemplateEngine.process(templateName, context);
    }

    private Context createContext(Map<String, String> data) {
        Context context = new Context();
        if (data != null) {
            context.setVariables(Map.copyOf(data));
        }
        return context;
    }
}
