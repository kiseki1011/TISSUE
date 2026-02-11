package com.tissue.feature.notification.application.port.repository;

import java.util.Map;

// TODO: 이건 port.out에 속하나? domain에 속하나?
public interface NotificationTemplateRenderer {
    String renderString(String template, Map<String, String> data);

    String renderHtml(String templateName, Map<String, Object> data);
}
