package com.tissue.api.issue.base.domain.model.vo;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.common.util.TextNormalizer.*;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

	public static Label of(String raw) {
		String checked = requireNotNull(raw, "label");
		String display = requireNotBlank(normalizeLabel(checked), "label");
		String norm = requireNotBlank(normalizeForUniq(checked), "label");

		return new Label(display, norm);
	}
}
