package com.tissue.issuetype.application.dto.response;

import com.tissue.issuetype.domain.EnumFieldOption;
import java.util.List;

public record ReorderedOptionsResponse(Long issueFieldId, List<OptionDetail> options) {
    public record OptionDetail(Long id, String name, int position) {}

    public static ReorderedOptionsResponse from(Long fieldId, List<EnumFieldOption> options) {
        List<OptionDetail> details = options.stream()
                .map(opt -> new OptionDetail(opt.getId(), opt.getDisplayName(), opt.getPosition()))
                .toList();
        return new ReorderedOptionsResponse(fieldId, details);
    }
}
