package com.tissue.feature.issuetype.domain.policy;

import com.tissue.feature.issuetype.domain.exception.OptionLimitExceededException;
import com.tissue.shared.vo.Name;
import java.util.List;

// TODO: Integrate this into IssueFieldPolicy
public record FieldDefintionPolicy(int maxEnumOptions) {

    // TODO: Should i just use ensureCanAddOption?
    public void ensureOptionsWithinLimit(List<Name> options) {
        if (options.size() > maxEnumOptions) {
            throw new OptionLimitExceededException(maxEnumOptions, options.size());
        }
    }

    public void ensureCanAddOption(int activeCount) {
        if (activeCount >= maxEnumOptions) {
            throw new OptionLimitExceededException(maxEnumOptions, activeCount + 1);
        }
    }
}
