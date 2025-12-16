package com.tissue.issuetype.domain.policy;

import java.util.List;

import com.tissue.common.vo.Label;

// TODO: 그냥 IssueFieldPolicy로 옮겨도 되지 않을까? 어차피 옵션을 추가하는 것도 IssueField의 책임?
public record FieldDefintionPolicy(
	int maxEnumOptions
) {
	// TODO: 해당 메서드 삭제하고 그냥 ensureCanAddOption 사용하는게 좋을까?
	public void ensureOptionsWithinLimit(List<Label> options) {
		if (options.size() > maxEnumOptions) {
			// TODO: OptionLimitExceededException extends BadRequestException
			throw new RuntimeException("Too many options. max=" + maxEnumOptions);
		}
	}

	public void ensureCanAddOption(int activeCount) {
		if (activeCount >= maxEnumOptions) {
			// TODO: OptionLimitExceededException extends BadRequestException
			throw new RuntimeException("Too many options. max=" + maxEnumOptions);
		}
	}
}
