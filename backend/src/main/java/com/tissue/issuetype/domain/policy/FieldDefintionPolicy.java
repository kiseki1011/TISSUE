package com.tissue.issuetype.domain.policy;

import com.tissue.common.vo.Name;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import java.util.List;

// TODO: 그냥 IssueFieldPolicy로 옮겨도 되지 않을까? 어차피 옵션을 추가하는 것도 IssueField의 책임?
public record FieldDefintionPolicy(int maxEnumOptions) {
    // TODO: 해당 메서드 삭제하고 그냥 ensureCanAddOption 사용하는게 좋을까?
    public void ensureOptionsWithinLimit(List<Name> options) {
        if (options.size() > maxEnumOptions) {
            throw IssueTypeExceptions.optionLimitExceeded(maxEnumOptions, options.size());
        }
    }

    public void ensureCanAddOption(int activeCount) {
        if (activeCount >= maxEnumOptions) {
            throw IssueTypeExceptions.optionLimitExceeded(maxEnumOptions, activeCount + 1);
        }
    }
}
