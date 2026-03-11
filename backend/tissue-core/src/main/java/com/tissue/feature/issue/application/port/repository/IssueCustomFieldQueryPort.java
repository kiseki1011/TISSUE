package com.tissue.feature.issue.application.port.repository;

public interface IssueCustomFieldQueryPort {

    boolean existsWithCustomField(String fieldIdStr);

    boolean isOptionInUse(String fieldIdStr, String optionIdStr);
}
