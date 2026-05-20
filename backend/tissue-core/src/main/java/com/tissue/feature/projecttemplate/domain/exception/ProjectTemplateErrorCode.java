package com.tissue.feature.projecttemplate.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProjectTemplateErrorCode implements ErrorCode {
    PROJECT_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Project template not found");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
