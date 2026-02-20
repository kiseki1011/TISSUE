package com.tissue.feature.organization.domain.policy;

public interface OrganizationConstraintPolicy {
    int NAME_MIN_LENGTH = 2;
    int NAME_MAX_LENGTH = 50;

    int DESCRIPTION_MAX_LENGTH = 255;
}
