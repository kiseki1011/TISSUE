package com.tissue.shared.vo;

import com.tissue.support.util.TextNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Embeddable
@EqualsAndHashCode(of = "normalizedName")
public class Name {

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "normalized_name", nullable = false, length = 64)
    private String normalizedName;

    @SuppressWarnings("NullAway.Init")
    protected Name() {}

    public static Name of(String raw) {
        String checked = Objects.requireNonNull(raw);

        String display = TextNormalizer.normalizeText(checked);
        String norm = TextNormalizer.normalizeForUniq(checked);

        Name name = new Name();
        name.displayName = display;
        name.normalizedName = norm;

        return name;
    }

    public boolean isSameAs(String name) {
        String otherNormalized = TextNormalizer.normalizeForUniq(name);
        return Objects.equals(this.normalizedName, otherNormalized);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
