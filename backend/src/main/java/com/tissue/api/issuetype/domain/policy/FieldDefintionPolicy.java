package com.tissue.api.issuetype.domain.policy;

import java.util.List;

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.common.vo.Label;

// TODO: 그냥 IssueFieldPolicy로 옮겨도 되지 않을까? 어차피 옵션을 추가하는 것도 IssueField의 책임?
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
