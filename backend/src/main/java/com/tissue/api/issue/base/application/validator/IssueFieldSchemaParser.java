package com.tissue.api.issue.base.application.validator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.policy.IssueFieldPolicy;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldSchemaParser {

	private final EnumFieldOptionRepository optionRepo;
	private final IssueFieldPolicy issueFieldPolicy;

	/**
	 * Converts 'rawInput' into a domain value based on the field type.
	 * - TEXT    -> String (expects a JSON string)
	 * - INTEGER -> Integer (expects an integer-like number)
	 * - DECIMAL -> BigDecimal (expects a decimal number)
	 * - DATE    -> Instant (expects an ISO-8601 string)
	 * - ENUM    -> EnumFieldOption (expects an option 'key', not a label)
	 */
	public Object toDomainValue(IssueField field, Object rawInput) {
		return switch (field.getFieldType()) {
			case TEXT -> parseTextValue(field, rawInput);
			case INTEGER -> parseIntegerValue(field, rawInput);
			case DECIMAL -> parseDecimalValue(field, rawInput);
			case DATE -> parseInstantValue(field, rawInput);
			case ENUM -> findEnumOptionByKey(field, rawInput);
			default -> throw new InvalidCustomFieldException(
				"Unsupported field type: " + field.getFieldType()
			);
		};
	}

	private String parseTextValue(IssueField field, Object rawInput) {
		if (rawInput instanceof String stringValue) {
			return stringValue;
		}
		throw new InvalidCustomFieldException(
			"Field '%s' must be a string.".formatted(field.getKey())
		);
	}

	private Integer parseIntegerValue(IssueField field, Object rawInput) {
		if (rawInput instanceof Integer integerValue) {
			return integerValue;
		}
		if (rawInput instanceof Long longValue) {
			return convertLongToInt(field, longValue);
		}
		if (rawInput instanceof Number numberValue) {
			return convertWholeNumberToInt(field, numberValue);
		}
		if (rawInput instanceof String stringValue) {
			return parseIntegerFromString(field, stringValue);
		}
		throw new InvalidCustomFieldException(
			"Field '%s' must be an integer.".formatted(field.getKey())
		);
	}

	/**
	 * Converts long to int;
	 * throws if out of 32-bit range.
	 */
	private Integer convertLongToInt(IssueField field, long longValue) {
		try {
			return Math.toIntExact(longValue);
		} catch (ArithmeticException ex) {
			throw new InvalidCustomFieldException(
				"Field '%s' integer is out of 32-bit range.".formatted(field.getKey())
			);
		}
	}

	/**
	 * Converts a Number to int if it represents an integral value (e.g., 3.0 → 3).
	 * Rejects fractional numbers (e.g., 3.14).
	 */
	private Integer convertWholeNumberToInt(IssueField field, Number numberValue) {
		double doubleValue = numberValue.doubleValue();
		if (Double.isFinite(doubleValue) && Math.floor(doubleValue) == doubleValue) {
			return (int)doubleValue;
		}
		throw new InvalidCustomFieldException(
			"Field '%s' must be an integer.".formatted(field.getKey())
		);
	}

	/**
	 * Parses decimal string as 32-bit integer;
	 * throws on invalid format or overflow.
	 */
	private Integer parseIntegerFromString(IssueField field, String stringValue) {
		try {
			return Integer.parseInt(stringValue);
		} catch (NumberFormatException ex) {
			throw new InvalidCustomFieldException(
				"Field '%s' must be an integer.".formatted(field.getKey())
			);
		}
	}

	private BigDecimal parseDecimalValue(IssueField field, Object rawInput) {
		BigDecimal value;

		switch (rawInput) {
			case BigDecimal bigDecimalValue -> value = bigDecimalValue;
			case Number numberValue -> value = new BigDecimal(numberValue.toString());
			case String stringValue -> value = parseBigDecimalFromString(field, stringValue);
			default -> throw new InvalidCustomFieldException(
				"Field '%s' must be a decimal number.".formatted(field.getKey())
			);
		}

		issueFieldPolicy.ensureDigits(value, field.getKey());
		return issueFieldPolicy.normalizeDecimal(value);
	}

	/**
	 * Parse String to BigDecimal;
	 * throws on invalid format.
	 */
	private BigDecimal parseBigDecimalFromString(IssueField field, String stringValue) {
		try {
			return new BigDecimal(stringValue);
		} catch (NumberFormatException ex) {
			throw new InvalidCustomFieldException(
				"Field '%s' must be a decimal number.".formatted(field.getKey())
			);
		}
	}

	private Instant parseInstantValue(IssueField field, Object rawInput) {
		if (rawInput instanceof String stringValue) {
			return parseInstantFromIsoString(field, stringValue);
		}
		throw new InvalidCustomFieldException(
			"Field '%s' must be ISO-8601 string.".formatted(field.getKey())
		);
	}

	/**
	 * Parses an ISO-8601 string into an Instant;
	 * throws if the format is invalid.
	 */
	private Instant parseInstantFromIsoString(IssueField field, String stringValue) {
		try {
			return Instant.parse(stringValue);
		} catch (DateTimeParseException ex) {
			throw new InvalidCustomFieldException(
				"Field '%s' must be ISO-8601 string.".formatted(field.getKey())
			);
		}
	}

	/**
	 * Looks up an enum option by key in the given field;
	 * rejects non-string input.
	 */
	private EnumFieldOption findEnumOptionByKey(IssueField field, Object rawInput) {
		if (rawInput instanceof String optionKey) {
			return optionRepo.findByFieldAndKey(field, optionKey)
				.orElseThrow(() -> new InvalidCustomFieldException(
					"Unknown enum option key for field '%s': %s".formatted(field.getKey(), optionKey)
				));
		}
		throw new InvalidCustomFieldException("Field '%s' must be an enum key string.".formatted(field.getKey()));
	}
}
