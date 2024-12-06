package com.uranus.taskmanager.api.common;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
	List<T> content, // 실제 데이터 목록
	PageInfo pageInfo // 페이징 메타 정보
) {
	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(
			page.getContent(),
			PageInfo.from(page)
		);
	}
}
