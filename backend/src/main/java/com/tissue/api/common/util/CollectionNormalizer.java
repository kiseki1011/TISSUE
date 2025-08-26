package com.tissue.api.common.util;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionNormalizer {

	/**
	 * If the collection is null returns an empty list;
	 * otherwise returns a collection where leading and trailing Unicode whitespace of each string is removed,
	 * and preserves order
	 * and limits the size to 100.
	 */
	// TODO: Consider changing the name to normalizeList and introduce a parameter for limit;
	//  Make a normalizeOptions method at IssueFieldRules or IssueFieldNormalizer and call this from there.
	public static List<String> normalizeOptions(List<String> raw) {
		if (raw == null) {
			return List.of();
		}
		return raw.stream()
			.map(TextNormalizer::stripToEmpty)
			.filter(s -> !s.isEmpty())
			.distinct()
			.limit(100) // TODO: Use application.yml
			.toList();
	}
}
