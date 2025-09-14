package com.tissue.api.common.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainPreconditions {

	public static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public static <T> T requireNotNull(T val, String field) {
		if (val == null) {
			throw new RuntimeException(field + " must not be null");
		}

		return val;
	}

	public static String requireNotBlank(String val, String field) {
		String strVal = requireNotNull(val, field);

		if (strVal.isEmpty()) {
			throw new RuntimeException(field + " must not be blank");
		}

		return strVal;
	}

	public static String requireLength(String val, int minLength, int maxLength, String field) {
		String strVal = requireNotBlank(val, field);

		int len = strVal.length();
		if (minLength < 0) {
			throw new RuntimeException(field + " invalid length constraint(min < 0)");
		}
		if (minLength > maxLength) {
			throw new RuntimeException(field + " invalid length constraint(min > max)");
		}
		if (len < minLength || len > maxLength) {
			throw new RuntimeException(field + " length must be between " + minLength + " and " + maxLength);
		}

		return strVal;
	}

	public static String requirePattern(String val, Pattern pattern, String field) {
		String strVal = requireNotBlank(val, field);

		if (pattern == null) {
			throw new RuntimeException(field + " pattern must not be null");
		}
		if (!pattern.matcher(strVal).matches()) {
			throw new RuntimeException(field + " has invalid format");
		}

		return strVal;
	}

	public static <T extends Comparable<T>> T requireInRange(T val, T minRange, T maxRange, String field) {
		requireNotNull(val, field);
		requireNotNull(minRange, field + ".min");
		requireNotNull(maxRange, field + ".max");

		if (minRange.compareTo(maxRange) > 0) {
			throw new RuntimeException(field + " invalid range(min > max)");
		}
		if (val.compareTo(minRange) < 0 || val.compareTo(maxRange) > 0) {
			throw new RuntimeException(field + " must be between " + minRange + " and " + maxRange);
		}

		return val;
	}

	public static <T, C extends Collection<T>> C requireNotEmpty(C collection, String field) {
		if (collection == null) {
			throw new RuntimeException(field + " must not be null");
		}
		if (collection.isEmpty()) {
			throw new RuntimeException(field + " must not be empty");
		}

		return collection;
	}

	public static <T, C extends Collection<T>> C requireNoNulls(C collection, String field) {
		requireNotNull(collection, field);

		int idx = 0;
		for (T t : collection) {
			if (t == null) {
				throw new RuntimeException(field + " must not contain nulls (null at index " + idx + ")");
			}
			idx++;
		}

		return collection;
	}

	public static <T, C extends Collection<T>> C requireDistinct(C collection, String field) {
		requireNotNull(collection, field);

		Set<T> set = new HashSet<>(collection);
		if (set.size() != collection.size()) {
			throw new RuntimeException(field + " must not contain duplicates");
		}

		return collection;
	}

	public static <T, C extends Collection<T>> C requireSize(C collection, int minSize, int maxSize, String field) {
		requireNotNull(collection, field);

		if (minSize < 0) {
			throw new RuntimeException(field + " invalid size constraint(min < 0)");
		}
		if (minSize > maxSize) {
			throw new RuntimeException(field + " invalid size constraint(min > max)");
		}
		int size = collection.size();
		if (size < minSize || size > maxSize) {
			throw new RuntimeException(field + " size must be between " + minSize + " and " + maxSize);
		}

		return collection;
	}

	public static <K, V, M extends Map<K, V>> M requireNotEmpty(M map, String field) {
		if (map == null) {
			throw new RuntimeException(field + " must not be null");
		}
		if (map.isEmpty()) {
			throw new RuntimeException(field + " must not be empty");
		}

		return map;
	}

	public static <K, V, M extends Map<K, V>> M requireNoNullKeys(M map, String field) {
		requireNotNull(map, field);

		for (Map.Entry<K, V> e : map.entrySet()) {
			if (e.getKey() == null) {
				throw new RuntimeException(field + " must not contain null keys");
			}
		}

		return map;
	}

	public static <K, V, M extends Map<K, V>> M requireNoNullValues(M map, String field) {
		requireNotNull(map, field);

		for (Map.Entry<K, V> e : map.entrySet()) {
			if (e.getValue() == null) {
				throw new RuntimeException(field + " must not contain null values");
			}
		}

		return map;
	}

	public static <K, V, M extends Map<K, V>> M requireContainsKeys(M map, Collection<K> requiredKeys, String field) {
		requireNotNull(map, field);
		requireNotEmpty(requiredKeys, field + ".requiredKeys");

		if (!map.keySet().containsAll(requiredKeys)) {
			throw new RuntimeException(field + " must contain required keys " + requiredKeys);
		}

		return map;
	}
}
