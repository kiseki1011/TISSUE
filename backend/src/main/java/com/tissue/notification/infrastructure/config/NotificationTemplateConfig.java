package com.tissue.notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@Configuration
public class NotificationTemplateConfig {

    @Bean
    public SpringTemplateEngine notificationTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(stringTemplateResolver());
        return templateEngine;
    }

    private StringTemplateResolver stringTemplateResolver() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCacheable(false);
        return templateResolver;
    }
}
