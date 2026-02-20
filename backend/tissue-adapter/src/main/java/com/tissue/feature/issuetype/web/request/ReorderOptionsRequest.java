package com.tissue.feature.issuetype.web.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderOptionsRequest(@NotEmpty List<Long> targetOrderedIds) {}
