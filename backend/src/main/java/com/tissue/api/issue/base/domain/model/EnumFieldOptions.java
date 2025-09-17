package com.tissue.api.issue.base.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.tissue.api.common.exception.type.InvalidOperationException;

public final class EnumFieldOptions {

	private final IssueField field;
	private final List<EnumFieldOption> active;

	private EnumFieldOptions(IssueField field, List<EnumFieldOption> activeOrdered) {
		this.field = Objects.requireNonNull(field);
		this.active = List.copyOf(Objects.requireNonNull(activeOrdered));
		ensureSameField();
		ensureNonDecreasingOrder();
	}

	public static EnumFieldOptions fromActiveOrdered(IssueField field, List<EnumFieldOption> activeOrdered) {
		return new EnumFieldOptions(field, activeOrdered);
	}

	public int count() {
		return active.size();
	}

	public List<Long> keys() {
		return active.stream().map(EnumFieldOption::getId).toList();
	}

	public void softDeleteAll() {
		for (EnumFieldOption o : active) {
			o.softDelete();
		}
	}

	public void resequenceTo(List<Long> orderedKeys) {
		ensurePermutationOf(orderedKeys);
		if (isAlignedWith(orderedKeys)) {
			return;
		}
		applyIndices(orderedKeys);
	}

	private boolean isAlignedWith(List<Long> orderedKeys) {
		if (orderedKeys == null || orderedKeys.size() != active.size()) {
			return false;
		}
		for (int i = 0; i < active.size(); i++) {
			if (!Objects.equals(active.get(i).getId(), orderedKeys.get(i))) {
				return false;
			}
		}
		return true;
	}

	public int nextIndex() {
		return active.isEmpty() ? 0 : active.get(active.size() - 1).getPosition() + 1;
	}

	private void ensurePermutationOf(List<Long> orderedKeys) {
		Objects.requireNonNull(orderedKeys, "orderedKeys");
		if (orderedKeys.size() != active.size()) {
			throw new InvalidOperationException("Order size mismatch.");
		}
		Set<Long> uniq = new HashSet<>(orderedKeys);
		if (uniq.size() != orderedKeys.size()) {
			throw new InvalidOperationException("Order contains duplicates.");
		}
		if (!uniq.equals(new HashSet<>(keys()))) {
			throw new InvalidOperationException("Order keys must match exactly.");
		}
	}

	private void applyIndices(List<Long> orderedKeys) {
		Map<Long, EnumFieldOption> byId = active.stream()
			.collect(Collectors.toMap(EnumFieldOption::getId, x -> x));
		for (int idx = 0; idx < orderedKeys.size(); idx++) {
			EnumFieldOption option = byId.get(orderedKeys.get(idx));
			if (option.getPosition() != idx) {
				option.movePositionTo(idx);
			}
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
