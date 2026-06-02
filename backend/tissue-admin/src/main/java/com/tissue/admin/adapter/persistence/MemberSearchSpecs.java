package com.tissue.admin.adapter.persistence;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Passes integration test which was human written",
        agentName = "claude-opus-4-8",
        reviewedBy = "kiseki1011")
public final class MemberSearchSpecs {

    private static final String STATUS = "status";
    private static final String ROLE = "role";
    private static final String USERNAME = "username";
    private static final String NAME = "name";
    private static final String EMAIL = "email";

    private MemberSearchSpecs() {}

    public static Specification<Member> forAdminDirectory(
            @Nullable MemberStatus status, @Nullable SystemRole role, @Nullable String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get(STATUS), status));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get(ROLE), role));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get(USERNAME)), like),
                        cb.like(cb.lower(root.<String>get(NAME)), like),
                        cb.like(cb.lower(root.<String>get(EMAIL)), like)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
