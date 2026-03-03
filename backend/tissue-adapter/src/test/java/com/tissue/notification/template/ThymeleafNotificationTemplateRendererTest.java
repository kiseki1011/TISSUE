package com.tissue.notification.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import com.tissue.feature.notification.template.ThymeleafNotificationTemplateRenderer;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.thymeleaf.spring6.SpringTemplateEngine;

@SpringJUnitConfig
@ContextConfiguration(
        classes = {ThymeleafNotificationTemplateRenderer.class, ThymeleafNotificationTemplateRendererTest.Config.class},
        initializers = ConfigDataApplicationContextInitializer.class)
class ThymeleafNotificationTemplateRendererTest {

    @Autowired
    NotificationTemplateRenderer renderer;

    @Autowired
    MessageSource messageSource;

    @MockBean
    SpringTemplateEngine templateEngine;

    static class Config {
        @org.springframework.context.annotation.Bean
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages");
            messageSource.setDefaultEncoding("UTF-8");
            messageSource.setFallbackToSystemLocale(false);
            return messageSource;
        }
    }

    @Test
    @DisplayName("Should render ISSUE_CREATED title and content correctly using English template")
    void renderIssueCreated_En() {
        String titleKey = "event.ISSUE_CREATED.title";
        String contentKey = "event.ISSUE_CREATED.content";
        Locale locale = Locale.ENGLISH;

        Map<String, String> data = Map.of(
                "workspaceKey", "TEST-WS",
                "projectKey", "PROJ-1",
                "issueKey", "ISSUE-123",
                "actorName", "Gildong");

        String titleTemplate = messageSource.getMessage(titleKey, null, locale);
        String contentTemplate = messageSource.getMessage(contentKey, null, locale);

        String renderedTitle = renderer.renderString(titleTemplate, data);
        String renderedContent = renderer.renderString(contentTemplate, data);

        assertThat(renderedTitle).isEqualTo("[TEST-WS:ISSUE-123] Issue Created");
        assertThat(renderedContent).isEqualTo("Gildong created a new issue ISSUE-123 in project PROJ-1.");
    }

    @Test
    @DisplayName("Should render literal brackets correctly in title")
    void renderLiteralBrackets() {
        // Updated syntax: ${key}
        String template = "[${workspaceKey}] Title";
        Map<String, String> data = Map.of("workspaceKey", "MY-WS");

        String result = renderer.renderString(template, data);

        assertThat(result).isEqualTo("[MY-WS] Title");
    }
}
