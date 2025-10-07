package com.tissue.api.sprint.presentation.dto.request;

import java.time.Instant;

import com.tissue.api.common.validator.annotation.size.text.LongText;
import com.tissue.api.common.validator.annotation.size.text.ShortText;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateSprintRequest(

	@ShortText
	String title,

	@LongText
	String goal,

	@NotNull(message = "{valid.notnull}")
	@FutureOrPresent(message = "{valid.startdate}")
	Instant plannedStartDate,

	@NotNull(message = "{valid.notnull}")
	@Future(message = "{valid.enddate}")
	Instant plannedEndDate
) {
}
