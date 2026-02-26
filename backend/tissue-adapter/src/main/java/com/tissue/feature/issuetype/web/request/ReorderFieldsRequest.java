package com.tissue.feature.issuetype.web.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderFieldsRequest(@NotEmpty List<Long> orderedIds) {}
