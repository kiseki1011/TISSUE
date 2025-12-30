package com.tissue.issue.domain.service.handler;

import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import java.util.EnumMap;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class IssueFieldTypeHandlerRegistry {

    private final EnumMap<IssueFieldType, FieldTypeHandler> handlers;

    public IssueFieldTypeHandlerRegistry(List<FieldTypeHandler> handlerBeans) {
        this.handlers = new EnumMap<>(IssueFieldType.class);
        for (FieldTypeHandler h : handlerBeans) {
            IssueFieldType prev = (h.type());
            if (handlers.putIfAbsent(prev, h) != null) {
                throw new IllegalStateException("Duplicate handler: " + prev);
            }
        }
    }

    public boolean isBlank(IssueField field, @Nullable Object raw) {
        return requireHandler(field).isBlank(raw);
    }

    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        return requireHandler(field).parse(field, raw);
    }

    public void assign(IssueFieldValue target, @Nullable Object parsed) {
        requireHandler(target.getField()).assign(target, parsed);
    }

    private FieldTypeHandler requireHandler(IssueField field) {
        FieldTypeHandler handler = handlers.get(field.getIssueFieldType());
        if (handler == null) {
            throw new IllegalStateException("Handler not configured for field type: " + field.getIssueFieldType());
        }
        return handler;
    }
}
