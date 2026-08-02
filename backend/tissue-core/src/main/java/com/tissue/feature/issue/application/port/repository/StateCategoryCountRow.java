package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.workflow.domain.enums.StateCategory;

public interface StateCategoryCountRow {

    StateCategory getCategory();

    long getCount();
}
