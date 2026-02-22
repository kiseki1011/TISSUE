package com.tissue.shared.vo;

import com.tissue.support.util.TextNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Embeddable
@EqualsAndHashCode(of = "normalized")
public class Name {

    @Column(nullable = false, length = 64)
    private String display;

    @Column(nullable = false, length = 64)
    private String normalized;

    private Name(String display, String normalized) {
        this.display = display;
        this.normalized = normalized;
    }

    @SuppressWarnings("NullAway.Init")
    protected Name() {}

    public static Name of(String raw) {
        String checked = Objects.requireNonNull(raw);

        String display = TextNormalizer.normalizeText(checked);
        String norm = TextNormalizer.normalizeForUniq(checked);

        return new Name(display, norm);
    }

    public boolean isSameAs(String name) {
        String otherNormalized = TextNormalizer.normalizeForUniq(name);
        return Objects.equals(this.normalized, otherNormalized);
    }

    @Override
    public String toString() {
        return display;
    }
}
