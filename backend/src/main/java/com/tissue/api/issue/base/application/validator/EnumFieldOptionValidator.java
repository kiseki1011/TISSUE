package com.tissue.api.issue.base.application.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldOptionValidator {

	private final EnumFieldOptionRepository optionRepo;

	public void ensureLabelUnique(IssueField field, String label) {
		if (optionRepo.existsByFieldAndLabel(field, label)) {
			throw new ResourceConflictException("Option label already exists in this field.");
		}
	}

	public void ensureNotInUse(EnumFieldOption opt) {
		if (optionRepo.isInUse(opt)) {
			throw new InvalidOperationException("Cannot delete/archive: option is in use.");
		}
	}

	// TODO: EnumFieldOptions라는 일급객체를 사용하도록 리팩토링하는 경우,
	//  아래 메서드들을 EnumFieldOptions로 이동
	public void ensureValidReorder(List<EnumFieldOption> active, List<Long> orderedIds) {
		ensureSameSize(active, orderedIds);
		ensureNoDuplicates(orderedIds);
		ensureKeysMatch(active, orderedIds);
	}

	private void ensureSameSize(List<EnumFieldOption> active, List<Long> orderedIds) {
		if (orderedIds.size() != active.size()) {
			throw new InvalidOperationException("Order size mismatch.");
		}
	}

	private void ensureNoDuplicates(List<Long> orderedIds) {
		Set<Long> uniq = new HashSet<>(orderedIds);
		if (uniq.size() != orderedIds.size()) {
			throw new InvalidOperationException("Order contains duplicates.");
		}
	}

	private void ensureKeysMatch(List<EnumFieldOption> active, List<Long> orderedIds) {
		Set<Long> provided = new HashSet<>(orderedIds);
		Set<Long> existing = extractIds(active);

		boolean keysNotMatch = !existing.equals(provided);
		if (keysNotMatch) {
			throw new InvalidOperationException("Order keys must match exactly.");
		}
	}

	private Set<Long> extractIds(List<EnumFieldOption> options) {
		return options.stream()
			.map(EnumFieldOption::getId)
			.collect(Collectors.toSet());
	}
}
