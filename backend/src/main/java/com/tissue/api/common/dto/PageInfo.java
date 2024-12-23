package com.tissue.api.common.dto;

import org.springframework.data.domain.Page;

public record PageInfo(
	int pageNumber,
	int pageSize,
	int totalPages,
	long totalElements,
	boolean hasNext
) {
	public static PageInfo from(Page<?> page) {
		return new PageInfo(
			page.getNumber(),
			page.getSize(),
			page.getTotalPages(),
			page.getTotalElements(),
			page.hasNext()
		);
	}
}
