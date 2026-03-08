package com.tissue.feature.issue.config;

import java.math.RoundingMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.issue.policy")
public class IssueProperties {

    private int maxReviewers = 10;
    private final Field field = new Field();

    @Data
    public static class Field {
        private int maxSelectOptions = 50;
        private final Decimal decimal = new Decimal();

        @Data
        public static class Decimal {
            private int scale = 6;
            private RoundingMode rounding = RoundingMode.HALF_UP;
            private int maxIntegerDigits = 14;
            private int maxFractionDigits = 6;
        }
    }
}
