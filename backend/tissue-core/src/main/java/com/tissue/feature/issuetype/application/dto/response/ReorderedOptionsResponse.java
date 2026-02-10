package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issuetype.domain.EnumFieldOption;
import java.util.List;

public record ReorderedOptionsResponse(Long issueFieldId, List<OptionDetail> options) {

    public record OptionDetail(Long id, String name, int position) {}

    public static ReorderedOptionsResponse from(Long fieldId, List<EnumFieldOption> options) {
        List<OptionDetail> details = options.stream()
                .map(opt -> new OptionDetail(opt.getId(), opt.getName(), opt.getPosition()))
                .toList();
        return new ReorderedOptionsResponse(fieldId, details);
    }
}
