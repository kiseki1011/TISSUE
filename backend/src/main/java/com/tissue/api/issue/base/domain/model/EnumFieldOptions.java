package com.tissue.api.issue.base.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.tissue.api.common.exception.type.InvalidOperationException;

public final class EnumFieldOptions {

	private static final int DEFAULT_OFFSET = 1_000_000;

	private final IssueField field;
	private final List<EnumFieldOption> active;

	private EnumFieldOptions(IssueField field, List<EnumFieldOption> currentOptions) {
		this.field = Objects.requireNonNull(field);
		this.active = List.copyOf(Objects.requireNonNull(currentOptions));
		ensureSameField();
		ensureNonDecreasingOrder();
	}

	public static EnumFieldOptions fromCurrentOptions(IssueField field, List<EnumFieldOption> currentOptions) {
		return new EnumFieldOptions(field, currentOptions);
	}

	public void ensureExactActiveIds(List<Long> orderedIds) {
		Objects.requireNonNull(orderedIds, "orderedIds");
		ensureSameSizeAsActive(orderedIds);
		ensureNoNullElements(orderedIds);
		ensureNoDuplicateIds(orderedIds);
		ensureValidActiveIds(orderedIds);
	}

	public void bumpPositions() {
		for (EnumFieldOption o : active) {
			o.movePositionTo(o.getPosition() + DEFAULT_OFFSET);
		}
	}

	public void reorderTo(List<Long> orderedIds) {
		Map<Long, EnumFieldOption> byId = active.stream()
			.collect(Collectors.toMap(EnumFieldOption::getId, x -> x));
		for (int i = 0; i < orderedIds.size(); i++) {
			Long id = orderedIds.get(i);
			EnumFieldOption option = byId.get(id);
			if (option == null) {
				throw new InvalidOperationException("Unknown option id: " + id);
			}
			if (option.getPosition() != i) {
				option.movePositionTo(i);
			}
		}
	}

	private void ensureSameSizeAsActive(List<Long> orderedIds) {
		if (orderedIds.size() != active.size()) {
			throw new InvalidOperationException("Order size mismatch.");
		}
	}

	private void ensureNoNullElements(List<Long> orderedIds) {
		if (orderedIds.contains(null)) {
			throw new InvalidOperationException("Order contains null id.");
		}
	}

	private void ensureNoDuplicateIds(List<Long> orderedIds) {
		Set<Long> uniq = new HashSet<>(orderedIds);
		if (uniq.size() != orderedIds.size()) {
			throw new InvalidOperationException("Order contains duplicates.");
		}
	}

	private void ensureValidActiveIds(List<Long> orderedIds) {
		Set<Long> actual = active.stream()
			.map(EnumFieldOption::getId)
			.collect(Collectors.toSet());
		Set<Long> uniq = new HashSet<>(orderedIds);
		if (!uniq.equals(actual)) {
			throw new InvalidOperationException("Order keys must match the active option set exactly.");
		}
	}

	private void ensureSameField() {
		Long fid = field.getId();
		for (EnumFieldOption o : active) {
			if (!Objects.equals(o.getField().getId(), fid)) {
				throw new IllegalStateException("Option belongs to another field.");
			}
		}
	}

	private void ensureNonDecreasingOrder() {
		for (int i = 1; i < active.size(); i++) {
			if (active.get(i - 1).getPosition() > active.get(i).getPosition()) {
				throw new IllegalStateException("Active options must be ordered by position (non-decreasing).");
			}
		}
	}
}
