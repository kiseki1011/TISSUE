package com.tissue.feature.issue.application.dto.response;

public record IssueAttachmentUploadResponse(String issueKey, Long attachmentId, String originalFilename) {}
