package com.tissue.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

@Getter
public abstract class TissueException extends RuntimeException {

    private final ErrorCode errorCode;

    @Nullable
    private final String detailMessage;

    private final Map<String, Object> context = new HashMap<>();

    public abstract HttpStatus getHttpStatus();

    public TissueException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    public TissueException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    public TissueException(ErrorCode errorCode, String detailMessage) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public TissueException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public @Nullable String getLoggingMessage() {
        String logMessage = (detailMessage != null && !detailMessage.isBlank()) ? detailMessage : getMessage();

        if (context.isEmpty()) {
            return logMessage;
        }

        String contextStr = context.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));

        return logMessage + " | context=" + contextStr;
    }

    // TODO: 정적 팩토리 내에서만 사용 가능하다는 javadoc 추가
    @SuppressWarnings("unchecked")
    public <T extends TissueException> T addContext(String key, @Nullable Object value) {
        this.context.put(key, value);
        return (T) this;
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
}
