package com.tissue.api.review.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.review.domain.Review;
import com.tissue.api.review.domain.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewReader {

	private final ReviewRepository reviewRepository;

	public Review findReview(Long id) {
		return reviewRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				"Review was not found with review id: %d", id)));
	}
}
