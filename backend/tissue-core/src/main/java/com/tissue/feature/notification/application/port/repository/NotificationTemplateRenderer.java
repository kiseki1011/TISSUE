package com.tissue.feature.notification.application.port.repository;

import java.util.Map;

public interface NotificationTemplateRenderer {
    String renderString(String template, Map<String, String> data);

    String renderHtml(String templateName, Map<String, Object> data);
}
