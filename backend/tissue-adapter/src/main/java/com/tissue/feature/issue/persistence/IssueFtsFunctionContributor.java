package com.tissue.feature.issue.persistence;

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
 *
 * <p>Functions:
 * <ul>
 *   <li>{@code fts_match(searchVector, query) → boolean}
 *       — wraps {@code col @@ plainto_tsquery('simple', :q)}</li>
 *   <li>{@code fts_rank(searchVector, query) → float}
 *       — wraps {@code ts_rank(col, plainto_tsquery('simple', :q))}</li>
 * </ul>
 *
 * <p>'simple' configuration is used (no stemming, no stop words) to match the seed
 * vocabulary. Production should pick the language-aware config that fits the data.
 */
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
