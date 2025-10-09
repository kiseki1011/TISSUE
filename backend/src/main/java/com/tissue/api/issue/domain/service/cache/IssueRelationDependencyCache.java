package com.tissue.api.issue.domain.service.cache;

import java.util.Set;

public interface IssueRelationDependencyCache {
	/**
	 * Retrieves the list of dependencies associated with the given key.
	 */
	Set<String> get(String key);

	/**
	 * Stores a list of dependencies for the given key.
	 */
	void put(String key, Set<String> dependencies);

	/**
	 * Clears the cache.
	 */
	void clear();
}
