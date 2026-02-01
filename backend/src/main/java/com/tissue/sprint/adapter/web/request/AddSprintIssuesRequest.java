package com.tissue.sprint.adapter.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AddSprintIssuesRequest(
    @NotEmpty @Size(max = 100, message = "Cannot add more than 100 issues to the sprint.")
    List<String> issueKeys) {

}
