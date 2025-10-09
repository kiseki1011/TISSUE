package com.tissue.api.issue.domain.model.vo;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.common.util.TextNormalizer.*;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Embeddable
@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(of = "normalized")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Label {

	@Column(name = "label", nullable = false, length = 32)
	@ToString.Include
	private String display;

	@Column(name = "label_normalized", nullable = false, length = 32)
	private String normalized;

	private Label(String display, String normalized) {
		this.display = display;
		this.normalized = normalized;
	}

	public static Label of(@NonNull String raw) {
		String checked = Objects.requireNonNull(raw);
		String display = requireNotBlank(normalizeLabel(checked));
		String norm = requireNotBlank(normalizeForUniq(checked));

		return new Label(display, norm);
	}
}
