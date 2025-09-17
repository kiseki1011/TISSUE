package com.tissue.api.issue.base.domain.model.vo;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.common.util.TextNormalizer.*;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Embeddable
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "normalized")
@ToString(of = "display")
public class Label {

	@Column(name = "label", nullable = false, length = 32)
	private String display;

	@Column(name = "label_normalized", nullable = false, length = 32)
	private String normalized;

	public static Label of(String raw) {
		String checked = requireNotNull(raw, "label");
		String display = requireNotBlank(normalizeLabel(checked), "label");
		String norm = requireNotBlank(normalizeForUniq(checked), "label");

		return new Label(display, norm);
	}
}
