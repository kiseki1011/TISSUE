package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.CustomFieldValueInfo;
import java.util.List;

public record IssueCustomDetail(String issueKey, List<CustomFieldValueInfo> customFields) {

    //    public static IssueCustomDetail from(Issue issue, List<IssueFieldValue> fieldValues) {
    //        return new IssueCustomDetail(
    //                issue.getKey(),
    //                fieldValues.stream().map(CustomFieldValueInfo::from).toList());
    //    }
}
