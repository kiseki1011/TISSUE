package com.tissue.api.issuetype.domain.policy;

import java.util.List;

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.common.vo.Label;

public record FieldDefintionPolicy(
	int maxEnumOptions
) {
	public void ensureOptionsWithinLimit(List<Label> options) {
		if (options.size() > maxEnumOptions) {
			throw new BadRequestException("Too many options. max=" + maxEnumOptions);
		}
	}

	public void ensureCanAddOption(int activeCount) {
		if (activeCount >= maxEnumOptions) {
			throw new BadRequestException("Too many options. max=" + maxEnumOptions);
		}
	}
}
