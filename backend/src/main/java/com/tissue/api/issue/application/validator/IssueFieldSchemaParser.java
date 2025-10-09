package com.tissue.api.issue.application.validator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.domain.policy.FieldValuePolicy;
import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.repository.EnumFieldOptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldSchemaParser {

	private final EnumFieldOptionRepository optionRepo;
	private final FieldValuePolicy policy;

	/**
	 * Converts 'raw' input into a domain value based on the field type.
	 * - TEXT        -> String (expects a JSON string)
	 * - INTEGER     -> Integer (expects an integer-like number)
	 * - DECIMAL     -> BigDecimal (expects a decimal number)
	 * - TIMESTAMP   -> Instant (expects an ISO-8601 string)
	 * - DATE        -> LocalDate
	 * - ENUM        -> EnumFieldOption (expects an option 'id'(Long))
	 */
	public Object toDomainValue(IssueField field, Object raw) {
		return switch (field.getFieldType()) {
			case TEXT -> parseTextValue(field, raw);
			case INTEGER -> parseIntegerValue(field, raw);
			case DECIMAL -> parseDecimalValue(field, raw);
			case TIMESTAMP -> parseInstantValue(field, raw);
			case DATE -> parseDateValue(field, raw);
			case ENUM -> findEnumOptionByKey(field, raw);
			default -> throw new InvalidCustomFieldException(
				"Unsupported field type: " + field.getFieldType()
			);
		};
	}

	private String parseTextValue(IssueField field, Object raw) {
		if (raw instanceof String s) {
			return s;
		}
		throw new InvalidCustomFieldException(
			"Field(id: '%d') must be a string.".formatted(field.getId())
		);
	}

	private Integer parseIntegerValue(IssueField field, Object raw) {
		if (raw instanceof Integer i) {
			return i;
		}
		if (raw instanceof Long l) {
			return convertToIntExact(field, l);
		}
		if (raw instanceof String s) {
			return parseIntegerFromString(field, s);
		}
		throw new InvalidCustomFieldException(
			"Field(id: '%d') must be an integer.".formatted(field.getId())
		);
	}

	private BigDecimal parseDecimalValue(IssueField field, Object raw) {
		BigDecimal value;

		switch (raw) {
			case BigDecimal bigDecimalValue -> value = bigDecimalValue;
			case Number numberValue -> value = new BigDecimal(numberValue.toString());
			case String stringValue -> value = parseBigDecimalFromString(field, stringValue);
			default -> throw new InvalidCustomFieldException(
				"Field(id: '%d') must be a decimal number.".formatted(field.getId())
			);
		}

		policy.ensureDigits(value, field.getId());
		return policy.normalizeDecimal(value);
	}

	/**
	 * Converts long to int;
	 * throws if out of 32-bit range.
	 */
	private Integer convertToIntExact(IssueField field, long longVal) {
		try {
			return Math.toIntExact(longVal);
		} catch (ArithmeticException ex) {
			throw new InvalidCustomFieldException(
				"Field(id: '%d') integer is out of 32-bit range.".formatted(field.getId())
			);
		}
	}

	private Instant parseInstantValue(IssueField field, Object raw) {
		if (raw instanceof String s) {
			return parseInstantFromIsoString(field, s);
		}
		throw new InvalidCustomFieldException(
			"Field(id: '%d') must be ISO-8601 string.".formatted(field.getId())
		);
	}

	private LocalDate parseDateValue(IssueField field, Object raw) {
		if (raw instanceof String s) {
			try {
				return LocalDate.parse(s);
			} catch (DateTimeParseException e) {
				throw new InvalidCustomFieldException("must be yyyy-MM-dd");
			}
		}
		if (raw instanceof Number n) {
			Instant inst = Instant.ofEpochMilli(n.longValue());
			return LocalDateTime.ofInstant(inst, ZoneOffset.UTC).toLocalDate();
		}
		throw new InvalidCustomFieldException("must be yyyy-MM-dd or epoch millis");
	}

	/**
	 * Looks up an enum option by key in the given field;
	 * rejects non-string input.
	 */
	private EnumFieldOption findEnumOptionByKey(IssueField field, Object raw) {
		if (raw instanceof Long optionId) {
			return optionRepo.findByFieldAndId(field, optionId)
				.orElseThrow(() -> new InvalidCustomFieldException(
					"Unknown enum option(id: '%d') for field: %s".formatted(optionId, field.getId())
				));
		}
		throw new InvalidCustomFieldException("Field(id: '%d') must be an enum key string.".formatted(field.getId()));
	}

	/**
	 * Parses decimal string as 32-bit integer;
	 * throws on invalid format or overflow.
	 */
	private Integer parseIntegerFromString(IssueField field, String stringVal) {
		try {
			return Integer.parseInt(stringVal);
		} catch (NumberFormatException ex) {
			throw new InvalidCustomFieldException(
				"Field(id: '%d') must be an integer.".formatted(field.getId())
			);
		}
	}

	/**
	 * Parse String to BigDecimal;
	 * throws on invalid format.
	 */
	private BigDecimal parseBigDecimalFromString(IssueField field, String stringVal) {
		try {
			return new BigDecimal(stringVal);
		} catch (NumberFormatException ex) {
			throw new InvalidCustomFieldException(
				"Field(id: '%d') must be a decimal number.".formatted(field.getId())
			);
		}
	}

	/**
	 * Parses an ISO-8601 string into an Instant;
	 * throws if the format is invalid.
	 */
	private Instant parseInstantFromIsoString(IssueField field, String stringVal) {
		try {
			return Instant.parse(stringVal);
		} catch (DateTimeParseException ex) {
			throw new InvalidCustomFieldException(
				"Field(id: '%d') must be ISO-8601 string.".formatted(field.getId())
			);
		}
	}
}
