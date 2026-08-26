package com.tissue.feature.vcs.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateVcsSyncRequest(
        @Schema(description = "Whether inbound webhooks for this project are acted on") @NotNull
        Boolean syncEnabled) {}
