package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.CustomFieldValueInfo;
import java.util.List;

public record IssueDetail(IssueCommonDetail common, List<CustomFieldValueInfo> customFields) {}
