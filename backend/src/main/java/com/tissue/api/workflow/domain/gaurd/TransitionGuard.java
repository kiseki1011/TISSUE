package com.tissue.api.workflow.domain.gaurd;

public interface TransitionGuard {

	// Guard 조건을 평가 - true면 통과, false면 차단
	boolean evaluate(GuardContext context);

	// Guard 실패 시 사용자에게 보여줄 메시지
	String getFailureMessage(GuardContext context);

	// Guard 타입 식별자 (DB에 저장되는 값)
	GuardType getType();
}
