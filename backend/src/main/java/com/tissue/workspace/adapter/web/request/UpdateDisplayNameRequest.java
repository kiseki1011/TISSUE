package com.tissue.workspace.adapter.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
    @Size(min = 2, max = 24) @NotBlank
    String displayName) {

}
