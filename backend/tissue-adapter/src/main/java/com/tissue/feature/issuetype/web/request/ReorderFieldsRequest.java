package com.tissue.feature.issuetype.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReorderFieldsRequest(
        @NotEmpty @Size(max = 100) List<Long> orderedIds) {}
