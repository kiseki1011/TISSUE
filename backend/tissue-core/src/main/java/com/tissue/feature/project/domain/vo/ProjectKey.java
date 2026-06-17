package com.tissue.feature.project.domain.vo;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.project.domain.exception.ReservedProjectKeyException;
import com.tissue.feature.project.domain.policy.ProjectConstraintPolicy;
import com.tissue.feature.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Project Key VO (Value Object)
 *
 * <p>Must be a valid format, non-reserved, globally unique.
 * <pre>
 * example: {@code DEMO}
 * </pre>
 */
@Getter
@Embeddable
@EqualsAndHashCode
public class ProjectKey {

    private static final Pattern KEY_PATTERN = Pattern.compile(ProjectConstraintPolicy.KEY_REGEX);

    @Column(name = "project_key", nullable = false, updatable = false)
    private String value;

    @SuppressWarnings("NullAway.Init")
    protected ProjectKey() {}

    private ProjectKey(String value) {
        this.value = value;
    }

    public static ProjectKey of(String raw) {
        validate(raw);
        return new ProjectKey(normalize(raw));
    }

    public static void validate(String raw) {
        String key = normalize(raw);
        if (key.length() < ProjectConstraintPolicy.KEY_MIN_LENGTH
                || key.length() > ProjectConstraintPolicy.KEY_MAX_LENGTH
                || !KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException(ProjectErrorCode.INVALID_PROJECT_KEY_FORMAT);
        }
        if (ProjectKeyPrefixPolicy.isReserved(key)) {
            throw new ReservedProjectKeyException(raw);
        }
    }

    private static String normalize(String raw) {
        return raw.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return value;
    }
}
