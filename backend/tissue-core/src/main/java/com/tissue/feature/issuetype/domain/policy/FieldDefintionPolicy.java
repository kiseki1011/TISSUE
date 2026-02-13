package com.tissue.feature.issuetype.domain.policy;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.OPTION_LIMIT_EXCEEDED;

import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import java.util.List;

// TODO: Integrate this into IssueFieldPolicy
public record FieldDefintionPolicy(int maxEnumOptions) {

    // TODO: Should i just use ensureCanAddOption?
    public void ensureOptionsWithinLimit(List<Name> options) {
        if (options.size() > maxEnumOptions) {
            throw new BadRequestException(OPTION_LIMIT_EXCEEDED).addContext("maxOptions", maxEnumOptions);
        }
    }

    public void ensureCanAddOption(int activeCount) {
        if (activeCount >= maxEnumOptions) {
            throw new BadRequestException(OPTION_LIMIT_EXCEEDED).addContext("maxOptions", maxEnumOptions);
        }
    }
}
