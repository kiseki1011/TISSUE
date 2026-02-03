package com.tissue.project.adapter.web.request;

import java.util.Set;

public record AddProjectMembersRequest(Set<Long> targetMemberIds) {}
