package com.tissue.api.common.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;

import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionNormalizer {

	public static List<Label> toUniqueLabels(@Nullable List<String> raw) {
		return Optional.ofNullable(raw).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(s -> !s.isBlank())
			.map(Label::of)
			.distinct()
			.toList();
	}
}
