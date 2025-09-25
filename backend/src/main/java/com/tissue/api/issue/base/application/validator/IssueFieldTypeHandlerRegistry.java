package com.tissue.api.issue.base.application.validator;

import java.util.EnumMap;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.base.application.validator.handler.FieldTypeHandler;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;

@Component
public class IssueFieldTypeHandlerRegistry {

	private final EnumMap<FieldType, FieldTypeHandler> handlers;

	public IssueFieldTypeHandlerRegistry(List<FieldTypeHandler> handlerBeans) {
		this.handlers = new EnumMap<>(FieldType.class);
		for (FieldTypeHandler h : handlerBeans) {
			FieldType prev = (h.type());
			if (handlers.putIfAbsent(prev, h) != null) {
				throw new IllegalStateException("Duplicate handler for " + prev);
			}
		}
	}

	public boolean isBlank(IssueField field, Object raw) {
		return requireHandler(field).isBlank(raw);
	}

	public Object parse(IssueField field, Object raw) {
		return requireHandler(field).parse(field, raw);
	}

	public void assign(IssueFieldValue target, Object parsed) {
		requireHandler(target.getField()).assign(target, parsed);
	}

	private FieldTypeHandler requireHandler(IssueField field) {
		FieldTypeHandler handler = handlers.get(field.getFieldType());
		if (handler == null) {
			throw new InvalidCustomFieldException("No handler for field type " + field.getFieldType());
		}
		return handler;
	}
}
