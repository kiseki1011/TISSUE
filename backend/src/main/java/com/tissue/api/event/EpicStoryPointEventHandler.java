package com.tissue.api.event;

import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueParentAssignedEvent;
import com.tissue.api.issue.domain.event.IssueParentRemovedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.domain.types.Epic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Epic 이슈의 스토리 포인트를 자식 이슈들의 스토리 포인트 합계로 자동 계산하는 이벤트 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EpicStoryPointEventHandler {

	private final IssueRepository issueRepository;

	/**
	 * 이슈 내용 업데이트 이벤트를 처리합니다.
	 * 자식 이슈의 스토리 포인트가 변경되었을 때 부모 Epic의 스토리 포인트를 재계산합니다.
	 */
	@EventListener
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueContentUpdated(IssueUpdatedEvent event) {
		// Epic이 아니고, SubTask도 아니며, 스토리 포인트가 변경된 이슈만 처리
		if (event.getIssueType() == IssueType.SUB_TASK || event.getIssueType() == IssueType.EPIC) {
			return;
		}
		if (!event.isStoryPointChanged()) {
			return;
		}

		// 부모 Epic을 찾아 업데이트
		issueRepository.findById(event.getIssueId())
			.filter(Issue::hasParent)
			.filter(issue -> issue.getParentIssue().getType() == IssueType.EPIC)
			.map(issue -> issue.getParentIssue().getId())
			.ifPresent(epicId -> {
				updateEpicStoryPoint(epicId);
				log.debug("Parent issue(EPIC) story point updated: {}", event.getIssueKey());
			});
	}

	/**
	 * 이슈에 부모가 할당되는 이벤트를 처리합니다.
	 * 이슈가 Epic에 자식으로 추가되거나, 다른 Epic으로 부모가 변경될 때 관련 Epic들의 스토리 포인트를 재계산합니다.
	 */
	@EventListener
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentAssigned(IssueParentAssignedEvent event) {
		// SubTask이거나 스토리 포인트를 가질 수 없는 타입이면 처리하지 않음
		if (event.getIssueType() == IssueType.SUB_TASK || event.getIssueType() == IssueType.EPIC) {
			return;
		}

		// 새 부모가 Epic인 경우 업데이트
		if (event.isNewParentEpic()) {
			updateEpicStoryPoint(event.getParentIssueId());
			log.debug("자식 이슈 할당 후 Epic 스토리 포인트 업데이트: parentEpic={}, childIssue={}",
				event.getParentIssueKey(), event.getIssueKey());
		}

		// 이전 부모가 Epic인 경우 업데이트
		if (event.wasOldParentEpic()) {
			updateEpicStoryPoint(event.getOldParentIssueId());
			log.debug("자식 이슈 재할당 후 이전 부모 Epic 스토리 포인트 업데이트: oldParentEpic={}, childIssue={}",
				event.getOldParentIssueKey(), event.getIssueKey());
		}
	}

	/**
	 * 이슈에서 부모가 제거되는 이벤트를 처리합니다.
	 * 이슈가 Epic에서 제거될 때 해당 Epic의 스토리 포인트를 재계산합니다.
	 */
	@EventListener
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueParentRemoved(IssueParentRemovedEvent event) {
		// SubTask이거나 스토리 포인트를 가질 수 없는 타입이면 처리하지 않음
		if (event.getIssueType() == IssueType.SUB_TASK || event.getIssueType() == IssueType.EPIC) {
			return;
		}

		// 제거된 부모가 Epic인 경우 업데이트
		if (event.wasRemovedParentEpic()) {
			updateEpicStoryPoint(event.getRemovedParentIssueId());
			log.debug("자식 이슈 제거 후 Epic 스토리 포인트 업데이트: epicIssue={}, childIssue={}",
				event.getRemovedParentIssueKey(), event.getIssueKey());
		}
	}

	/**
	 * 이슈 상태 변경 이벤트를 처리합니다.
	 * 특히 이슈가 CLOSED 상태로 변경되면 부모 Epic의 스토리 포인트 계산에 영향을 줍니다.
	 */
	@EventListener
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleIssueStatusUpdated(IssueStatusChangedEvent event) {
		// SubTask이거나 스토리 포인트를 가질 수 없는 타입이면 처리하지 않음 -> 그냥 이슈가 SubTask나 Epic이면 return하는 걸로 리팩토링?
		// 굳이 notStoryPointChangeable가 필요할까?
		if (event.getIssueType() == IssueType.SUB_TASK || event.getIssueType() == IssueType.EPIC) {
			return;
		}

		// CLOSED 상태로 변경된 경우에만 처리 (Epic 계산 메서드에서 CLOSED 이슈는 제외됨)
		if (event.isClosedNow()) {
			updateEpicStoryPoint(event.getParentIssueId());
			log.debug("자식 이슈 상태 변경(CLOSED) 후 Epic 스토리 포인트 업데이트: epicIssue={}, childIssue={}",
				event.getParentIssueKey(), event.getIssueKey());
		}
	}

	/**
	 * Epic 이슈의 스토리 포인트를 자식 이슈들의 합계로 업데이트합니다.
	 */
	@Transactional
	public void updateEpicStoryPoint(Long epicId) {
		issueRepository.findById(epicId)
			.filter(issue -> issue.getType() == IssueType.EPIC)
			.map(issue -> (Epic)issue)
			.ifPresent(epic -> {
				Integer oldStoryPoint = epic.getStoryPoint();
				epic.updateStoryPoint();

				// 로깅을 위해 새 값 가져오기
				Integer newStoryPoint = epic.getStoryPoint();

				// 변경 사항이 있으면 로그 남기기
				if (!Objects.equals(oldStoryPoint, newStoryPoint)) {
					log.info("Epic '{}' 스토리 포인트 업데이트: {} -> {}", epic.getIssueKey(), oldStoryPoint, newStoryPoint);
				}
			});
	}
}
