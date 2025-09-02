package com.tissue.api.issue.base.domain.policy;

import com.tissue.api.common.exception.type.InvalidOperationException;

public record IssueFieldPolicy(
	int maxEnumOptions
) {
	public void ensureOptionsWithinLimit(int size) {
		if (size > maxEnumOptions) {
			throw new InvalidOperationException("Too many options. max=" + maxEnumOptions);
		}
	}

	public void ensureCanAddOption(int activeCount) {
		if (activeCount >= maxEnumOptions) {
			throw new InvalidOperationException("Too many options. max=" + maxEnumOptions);
		}
	}
}
