package com.tissue.api.issue.base.infrastructure;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tissue.api.issue.base.domain.service.cache.IssueRelationDependencyCache;

public class CaffeineDependencyCache implements IssueRelationDependencyCache {

	private final Cache<String, Set<String>> cache;

	public CaffeineDependencyCache(int maximumSize, int expirationHours) {
		this.cache = Caffeine.newBuilder()
			.maximumSize(maximumSize)
			.expireAfterWrite(expirationHours, TimeUnit.HOURS)
			.build();
	}

	@Override
	public Set<String> get(String key) {
		return cache.getIfPresent(key);
	}

	@Override
	public void put(String key, Set<String> dependencies) {
		cache.put(key, dependencies);
	}

	@Override
	public void clear() {
		cache.invalidateAll();
	}
}
