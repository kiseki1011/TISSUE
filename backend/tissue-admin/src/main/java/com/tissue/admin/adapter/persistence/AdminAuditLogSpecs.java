package com.tissue.admin.adapter.persistence;

import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditLog;
import com.tissue.admin.domain.AdminAuditTargetType;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Passes human written integration test",
        model = "claude-opus-4-8",
        reviewedBy = "kiseki1011")
public final class AdminAuditLogSpecs {

    private static final String ACTOR_MEMBER_ID = "actorMemberId";
    private static final String ACTION = "action";
    private static final String TARGET_TYPE = "targetType";

    private AdminAuditLogSpecs() {}

    public static Specification<AdminAuditLog> filter(
            @Nullable Long actorMemberId,
            @Nullable AdminAuditAction action,
            @Nullable AdminAuditTargetType targetType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorMemberId != null) {
                predicates.add(cb.equal(root.get(ACTOR_MEMBER_ID), actorMemberId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get(ACTION), action));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get(TARGET_TYPE), targetType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
