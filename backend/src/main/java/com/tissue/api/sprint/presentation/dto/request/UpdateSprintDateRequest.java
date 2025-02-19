package com.tissue.api.sprint.presentation.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

public record UpdateSprintDateRequest(

	@NotNull(message = "{valid.notnull}")
	@FutureOrPresent(message = "{valid.startdate}")
	LocalDate startDate,

	@NotNull(message = "{valid.notnull}")
	@Future(message = "{valid.enddate}")
	LocalDate endDate
) {
}
