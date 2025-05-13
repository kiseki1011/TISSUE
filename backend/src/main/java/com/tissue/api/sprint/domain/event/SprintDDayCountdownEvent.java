package com.tissue.api.sprint.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.sprint.domain.Sprint;

import lombok.Getter;

/**
 * Todo
 *  - SprintDDayCountdownEvent를 처리할 SprintEventSchedular 또는 SprintSchedular 구현하기
 *  - 특정 D-DAY 값을 설정하면, 해당 D-DAY 부터 워크스페이스 전 인원에게 알림 보내기(특정 시간 일괄 처리)
 *  - 모든 워크스페이스의 스프린트를 검색해서 한번에 처리하면 성능 문제 발생
 *    -> 이유는 수 많은 각 워크스페이스의 모든 인원에게 알림을 보내게 될 가능성이 높음
 *    -> 해결 방법 1: 한번에 처리하지 말고 배치 처리
 *    -> 해결 방법 2: 특정 시간대 말고, 시간대를 나눠서 처리
 *    -> 해결 방법 3: 알림 처리 서비스를 마이크로 서비스로 분리
 *  - 다중 백엔드 서버(클러스터)를 사용하는 경우 동시에 스케쥴러가 동작하는 문제를 해결해야 함
 *    -> 해결 방법 1: 분산 락
 *    -> 해결 방법 2:
 */
@Getter
public class SprintDDayCountdownEvent extends SprintEvent {

	private final int dDay;

	public SprintDDayCountdownEvent(
		Long sprintId,
		String sprintKey,
		String workspaceCode,
		Long triggeredByWorkspaceMemberId,
		int dDay
	) {
		super(
			NotificationType.SPRINT_D_DAY_COUNTDOWN,
			ResourceType.SPRINT,
			workspaceCode,
			sprintId,
			sprintKey,
			triggeredByWorkspaceMemberId
		);
		this.dDay = dDay;
	}

	public static SprintDDayCountdownEvent createEvent(
		Sprint sprint,
		Long triggeredByWorkspaceMemberId,
		int dDay
	) {
		return new SprintDDayCountdownEvent(
			sprint.getId(),
			sprint.getSprintKey(),
			sprint.getWorkspaceCode(),
			triggeredByWorkspaceMemberId,
			dDay
		);
	}
}
