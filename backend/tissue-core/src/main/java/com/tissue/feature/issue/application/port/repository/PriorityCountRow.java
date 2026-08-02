package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.enums.IssuePriority;

public interface PriorityCountRow {

    IssuePriority getPriority();

    long getCount();
}
