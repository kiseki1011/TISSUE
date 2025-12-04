package com.tissue.ui.home.controller;

import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JobTypeController {

	private final MessageSource messageSource;

	// @GetMapping("/api/v1/jobtypes")
	// public ApiResponse<List<JobTypeResponse>> getAllJobTypes(Locale locale) {
	// 	log.info("JobType list requested with locale: {}", locale);
	//
	// 	List<JobTypeResponse> jobTypes = Arrays.stream(JobType.values())
	// 		.map(jobType -> JobTypeResponse.builder()
	// 			.name(jobType.name())
	// 			.description(jobType.getDescription(messageSource, locale))
	// 			.build())
	// 		.collect(Collectors.toList());
	//
	// 	return ApiResponse.ok("Found all job types.", jobTypes);
	// }

	@Builder
	public record JobTypeResponse(
		String name,
		String description
	) {
	}
}
