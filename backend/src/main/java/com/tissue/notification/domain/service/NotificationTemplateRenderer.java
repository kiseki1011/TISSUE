package com.tissue.notification.domain.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

    private final SpringTemplateEngine notificationTemplateEngine;

    public String render(String template, Map<String, String> data) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Context context = new Context();
        if (data != null) {
            context.setVariables(Map.copyOf(data));
        }

        return notificationTemplateEngine.process(template, context);
    }
}
