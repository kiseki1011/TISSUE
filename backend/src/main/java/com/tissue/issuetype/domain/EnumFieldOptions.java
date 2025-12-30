package com.tissue.issuetype.domain;

import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<EnumFieldOption> getSortedOptions() {
        return active.stream()
                .sorted(Comparator.comparingInt(EnumFieldOption::getPosition))
                .toList();
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
        Map<Long, EnumFieldOption> byId = active.stream().collect(Collectors.toMap(EnumFieldOption::getId, x -> x));
        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            EnumFieldOption option = byId.get(id);
            if (option == null) {
                throw IssueTypeExceptions.optionReorderUnknownId(id);
            }
            if (option.getPosition() != i) {
                option.movePositionTo(i);
            }
        }
    }

    private void ensureSameSizeAsActive(List<Long> orderedIds) {
        if (orderedIds.size() != active.size()) {
            throw IssueTypeExceptions.optionReorderSizeMismatch(active.size(), orderedIds.size());
        }
    }

    private void ensureNoNullElements(List<Long> orderedIds) {
        if (orderedIds.contains(null)) {
            throw IssueTypeExceptions.optionReorderUnknownId(null);
        }
    }

    private void ensureNoDuplicateIds(List<Long> orderedIds) {
        Set<Long> uniq = new HashSet<>(orderedIds);
        if (uniq.size() != orderedIds.size()) {
            throw IssueTypeExceptions.optionReorderDuplicateId();
        }
    }

    private void ensureValidActiveIds(List<Long> orderedIds) {
        Set<Long> actual = active.stream().map(EnumFieldOption::getId).collect(Collectors.toSet());
        Set<Long> uniq = new HashSet<>(orderedIds);
        if (!uniq.equals(actual)) {
            uniq.removeAll(actual);
            Long unknown = uniq.isEmpty() ? null : uniq.iterator().next();
            throw IssueTypeExceptions.optionReorderUnknownId(unknown);
        }
    }

    private void ensureSameField() {
        Long fieldId = field.getId();
        for (EnumFieldOption o : active) {
            if (!Objects.equals(o.getIssueField().getId(), fieldId)) {
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
