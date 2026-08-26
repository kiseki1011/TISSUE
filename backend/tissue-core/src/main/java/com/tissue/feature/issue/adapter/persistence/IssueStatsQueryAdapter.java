package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.application.port.repository.HierarchyCountRow;
import com.tissue.feature.issue.application.port.repository.IssueStatsQueryRepository;
import com.tissue.feature.issue.application.port.repository.PriorityCountRow;
import com.tissue.feature.issue.application.port.repository.ProjectAgingRow;
import com.tissue.feature.issue.application.port.repository.ProjectMemberStatsRow;
import com.tissue.feature.issue.application.port.repository.ProjectStatsKpiRow;
import com.tissue.feature.issue.application.port.repository.SprintStateAggregateRow;
import com.tissue.feature.issue.application.port.repository.StateCategoryCountRow;
import com.tissue.feature.issue.application.port.repository.TimestampPairRow;
import com.tissue.feature.issue.application.port.repository.VelocitySprintRow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * JPA-backed adapter for the project statistics port. Thin delegation to a Spring Data fragment: the
 * separation exists so the query mechanism (JPQL now, native SQL / a read model later) can change here
 * without any caller depending on it.
 */
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, model = "claude-opus-4-8")
@Repository
@RequiredArgsConstructor
public class IssueStatsQueryAdapter implements IssueStatsQueryRepository {

    private final IssueStatsJpaRepository jpaRepository;

    @Override
    public List<StateCategoryCountRow> countByStateCategory(Long projectId) {
        return jpaRepository.countByStateCategory(projectId);
    }

    @Override
    public List<HierarchyCountRow> countByHierarchy(Long projectId) {
        return jpaRepository.countByHierarchy(projectId);
    }

    @Override
    public List<PriorityCountRow> countByPriority(Long projectId) {
        return jpaRepository.countByPriority(projectId);
    }

    @Override
    public ProjectStatsKpiRow getKpis(Long projectId, Instant now, Collection<StateCategory> terminalCategories) {
        return jpaRepository.getKpis(projectId, now, terminalCategories);
    }

    @Override
    public List<ProjectMemberStatsRow> getMemberStats(
            Long projectId, StateCategory completed, Collection<StateCategory> openCategories) {
        return jpaRepository.getMemberStats(projectId, completed, openCategories);
    }

    @Override
    public ProjectAgingRow getAgingBuckets(
            Long projectId,
            Instant threshold3d,
            Instant threshold7d,
            Instant threshold14d,
            Collection<StateCategory> openCategories) {
        return jpaRepository.getAgingBuckets(projectId, threshold3d, threshold7d, threshold14d, openCategories);
    }

    @Override
    public long countBlockedOpen(Long projectId, Collection<StateCategory> openCategories) {
        return jpaRepository.countBlockedOpen(projectId, openCategories);
    }

    @Override
    public List<Instant> findCreatedAtBetween(Long projectId, Instant from, Instant to) {
        return jpaRepository.findCreatedAtBetween(projectId, from, to);
    }

    @Override
    public List<Instant> findResolvedAtBetween(Long projectId, Instant from, Instant to, StateCategory completed) {
        return jpaRepository.findResolvedAtBetween(projectId, from, to, completed);
    }

    @Override
    public List<TimestampPairRow> findCycleTimePairs(
            Long projectId, Instant from, Instant to, StateCategory completed) {
        return jpaRepository.findCycleTimePairs(projectId, from, to, completed);
    }

    @Override
    public List<TimestampPairRow> findLeadTimePairs(Long projectId, Instant from, Instant to, StateCategory completed) {
        return jpaRepository.findLeadTimePairs(projectId, from, to, completed);
    }

    @Override
    public List<SprintStateAggregateRow> getSprintStateAggregates(Long sprintId) {
        return jpaRepository.getSprintStateAggregates(sprintId);
    }

    @Override
    public List<VelocitySprintRow> getVelocityBySprint(Long projectId, StateCategory completed) {
        return jpaRepository.getVelocityBySprint(projectId, completed);
    }

    @Override
    public List<Instant> findMemberResolvedAtBetween(
            Long projectId, Long memberId, Instant from, Instant to, StateCategory completed) {
        return jpaRepository.findMemberResolvedAtBetween(projectId, memberId, from, to, completed);
    }
}
