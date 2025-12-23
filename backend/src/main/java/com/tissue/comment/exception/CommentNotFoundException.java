package com.tissue.comment.exception;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class CommentNotFoundException extends ResourceNotFoundException {

	private static final String ID_MESSAGE = "Review not found with id: %d";

	public CommentNotFoundException(Long commentId) {
		super(String.format(ID_MESSAGE, commentId));
	}
}
