package com.tissue.api.common.util;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionNormalizer {

	/**
	 * If the collection is null returns an empty list;
	 * otherwise returns a collection where leading and trailing Unicode whitespace of each string is removed,
	 * and preserves order.
	 */
	public static List<String> normalizeOptions(List<String> options) {
		if (options == null) {
			return List.of();
		}
		return options.stream()
			.map(TextNormalizer::normalizeLabel)
			.filter(s -> !s.isEmpty())
			.distinct()
			.toList();
	}
}
