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

@Embeddable
@Getter
@EqualsAndHashCode(of = "normalized")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Label {

	@Column(name = "label", nullable = false, length = 32)
	private String display;

	@Column(name = "label_normalized", nullable = false, length = 32)
	private String normalized;

	private Label(String display, String normalized) {
		this.display = display;
		this.normalized = normalized;
	}

	public static Label of(@NonNull String raw) {
		String checked = Objects.requireNonNull(raw);
		String display = TextNormalizer.normalizeText(checked);
		String norm = TextNormalizer.normalizeForUniq(checked);

		return new Label(display, norm);
	}

	@Override
	public String toString() {
		return display;
	}
}
