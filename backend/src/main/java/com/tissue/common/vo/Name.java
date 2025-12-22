package com.tissue.common.vo;

import java.util.Objects;

import com.tissue.common.util.TextNormalizer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// TODO: change class name to Name
@Embeddable
@Getter
@EqualsAndHashCode(of = "normalized")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Name {

	// TODO: change field name to value
	@Column(nullable = false, length = 64)
	private String display;

	@Column(nullable = false, length = 64)
	private String normalized;

	private Name(String display, String normalized) {
		this.display = display;
		this.normalized = normalized;
	}

	// TODO: change raw -> name
	public static Name of(@NonNull String raw) {
		String checked = Objects.requireNonNull(raw);

		// TODO: add length check if(>64)

		String display = TextNormalizer.normalizeText(checked);
		String norm = TextNormalizer.normalizeForUniq(checked);

		return new Name(display, norm);
	}

	public boolean isSameAs(@NonNull String name) {
		String otherNormalized = TextNormalizer.normalizeForUniq(name);
		return this.normalized.equals(otherNormalized);
	}

	@Override
	public String toString() {
		return display;
	}

	// TODO: consider adding Label equal compare method?
}
