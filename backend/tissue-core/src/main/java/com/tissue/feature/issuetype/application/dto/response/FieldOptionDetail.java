package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issuetype.domain.FieldOption;

public record FieldOptionDetail(Long id, String name) {

    public static FieldOptionDetail from(FieldOption option) {
        return new FieldOptionDetail(option.getId(), option.getName());
    }
}
