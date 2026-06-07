package com.tissue.feature.issue.adapter.persistence;

import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registers PostgreSQL full-text search operators as named Hibernate functions
 * so they can be called from JPA Criteria / Specification via {@code cb.function(...)}.
 *
 * <p>Discovered through {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 * Hibernate calls {@link #contributeFunctions} once at SessionFactory boot.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        model = "claude-opus-4-7",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Integration test passes",
        reviewedBy = "kiseki1011")
public class IssueFtsFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions contributions) {
        var registry = contributions.getFunctionRegistry();
        var typeConfig = contributions.getTypeConfiguration();

        BasicTypeReference<Boolean> booleanType = StandardBasicTypes.BOOLEAN;
        BasicTypeReference<Float> floatType = StandardBasicTypes.FLOAT;

        registry.patternDescriptorBuilder("fts_match", "(?1 @@ plainto_tsquery('simple', ?2))")
                .setExactArgumentCount(2)
                .setInvariantType(typeConfig.getBasicTypeRegistry().resolve(booleanType))
                .register();

        registry.patternDescriptorBuilder("fts_rank", "ts_rank(?1, plainto_tsquery('simple', ?2))")
                .setExactArgumentCount(2)
                .setInvariantType(typeConfig.getBasicTypeRegistry().resolve(floatType))
                .register();
    }
}
