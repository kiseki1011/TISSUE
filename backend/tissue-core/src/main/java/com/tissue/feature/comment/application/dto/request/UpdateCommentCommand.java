package com.tissue.feature.comment.application.dto.request;

import java.util.List;

public record UpdateCommentCommand(String content, List<String> mentionedUsernames) {}
