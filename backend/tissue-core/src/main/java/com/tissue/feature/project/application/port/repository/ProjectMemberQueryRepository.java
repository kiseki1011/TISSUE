package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberQueryRepository extends Repository<ProjectMember, Long> {

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.member
            WHERE pm.projectKey = :projectKey
              AND pm.member.id = :memberId
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findWithMemberByProjectKeyAndMemberId(
            @Param("projectKey") String projectKey, @Param("memberId") Long memberId);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.member m
            WHERE m.email = :email
              AND pm.projectKey = :projectKey
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findWithMemberByEmailAndProjectKey(
            @Param("email") String email, @Param("projectKey") String projectKey);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.project p
            WHERE pm.projectKey = :projectKey
              AND pm.member.id = :memberId
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findWithProjectByProjectKeyAndMemberId(
            @Param("projectKey") String projectKey, @Param("memberId") Long memberId);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.member.id = :memberId
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findByProjectAndMemberId(
            @Param("project") Project project, @Param("memberId") Long memberId);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.project
            WHERE pm.member.id = :memberId
              AND pm.softDeleted = false
            """)
    List<ProjectMember> findAllWithProjectByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT pm.project.id
            FROM ProjectMember pm
            WHERE pm.member.id = :memberId
              AND pm.softDeleted = false
            """)
    Set<Long> findProjectIdsByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT pm.member.id
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.member.id IN :memberIds
              AND pm.softDeleted = false
            """)
    Set<Long> findMemberIdsByProjectAndMemberIds(
            @Param("project") Project project, @Param("memberIds") Collection<Long> memberIds);

    @Query("""
            SELECT pm.member.id
            FROM ProjectMember pm
            WHERE pm.projectKey = :projectKey
              AND pm.softDeleted = false
            """)
    Set<Long> findMemberIdsByProjectKey(@Param("projectKey") String projectKey);

    @Query("""
            SELECT
                m.id as memberId,
                m.email as email,
                m.language as language
            FROM ProjectMember pm
            JOIN pm.member m
            WHERE pm.projectKey = :projectKey
            AND pm.softDeleted = false
            """)
    List<MemberContactInfo> findAllContactsByProjectKey(@Param("projectKey") String projectKey);

    @Query("""
            SELECT
                m.id as memberId,
                m.email as email,
                m.language as language
            FROM ProjectMember pm
            JOIN pm.member m
            WHERE pm.projectKey = :projectKey
            AND m.id <> :excludedMemberId
            AND pm.softDeleted = false
            """)
    List<MemberContactInfo> findAllContactsByProjectKeyExcluding(
            @Param("projectKey") String projectKey, @Param("excludedMemberId") Long excludedMemberId);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.member.id = :memberId
            """)
    Optional<ProjectMember> findByProjectAndMemberIdIncludingSoftDeleted(
            @Param("project") Project project, @Param("memberId") Long memberId);

    @Query("""
            SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.member.id = :memberId
            """)
    boolean existsByProjectAndMemberIdIncludingSoftDeleted(
            @Param("project") Project project, @Param("memberId") Long memberId);

    @Query(value = """
            SELECT pm FROM ProjectMember pm
            JOIN FETCH pm.member m
            WHERE pm.project = :project
              AND pm.softDeleted = false
              AND (:role IS NULL OR pm.role = :role)
            """, countQuery = """
            SELECT COUNT(pm) FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.softDeleted = false
              AND (:role IS NULL OR pm.role = :role)
            """)
    Page<ProjectMember> findAllByProject(
            @Param("project") Project project, @Param("role") @Nullable ProjectRole role, Pageable pageable);

    @Query(value = """
            SELECT pm FROM ProjectMember pm
            JOIN FETCH pm.member m
            WHERE pm.project = :project
              AND pm.softDeleted = false
              AND (:role IS NULL OR pm.role = :role)
              AND (LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.name)  LIKE LOWER(CONCAT('%', :keyword, '%')))
            """, countQuery = """
            SELECT COUNT(pm) FROM ProjectMember pm
            JOIN pm.member m
            WHERE pm.project = :project
              AND pm.softDeleted = false
              AND (:role IS NULL OR pm.role = :role)
              AND (LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.name)  LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<ProjectMember> findAllByProjectAndKeyword(
            @Param("project") Project project,
            @Param("role") @Nullable ProjectRole role,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Active members who can be added to the project.
     *
     * <p>A removed (soft-deleted) member stays a candidate, since adding restores them.
     */
    @Query(value = """
            SELECT m FROM Member m
            WHERE m.status = com.tissue.feature.member.domain.MemberStatus.ACTIVE
              AND NOT EXISTS (
                  SELECT 1 FROM ProjectMember pm
                  WHERE pm.project = :project AND pm.member = m AND pm.softDeleted = false)
            """, countQuery = """
            SELECT COUNT(m) FROM Member m
            WHERE m.status = com.tissue.feature.member.domain.MemberStatus.ACTIVE
              AND NOT EXISTS (
                  SELECT 1 FROM ProjectMember pm
                  WHERE pm.project = :project AND pm.member = m AND pm.softDeleted = false)
            """)
    Page<Member> findActiveMemberCandidatesByProject(@Param("project") Project project, Pageable pageable);

    /**
     * Active candidates whose username, name or email matches the keyword.
     *
     * <p>Kept separate from the no-keyword query so a null keyword never reaches the LIKE clauses.
     */
    @Query(value = """
            SELECT m FROM Member m
            WHERE m.status = com.tissue.feature.member.domain.MemberStatus.ACTIVE
              AND NOT EXISTS (
                  SELECT 1 FROM ProjectMember pm
                  WHERE pm.project = :project AND pm.member = m AND pm.softDeleted = false)
              AND (LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """, countQuery = """
            SELECT COUNT(m) FROM Member m
            WHERE m.status = com.tissue.feature.member.domain.MemberStatus.ACTIVE
              AND NOT EXISTS (
                  SELECT 1 FROM ProjectMember pm
                  WHERE pm.project = :project AND pm.member = m AND pm.softDeleted = false)
              AND (LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.name)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Member> findActiveMemberCandidatesByProjectAndKeyword(
            @Param("project") Project project, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT pm.projectKey AS projectKey, COUNT(pm) AS memberCount
            FROM ProjectMember pm
            WHERE pm.projectKey IN :projectKeys
              AND pm.softDeleted = false
            GROUP BY pm.projectKey
            """)
    List<ProjectMemberCountRow> countByProjectKeys(@Param("projectKeys") Collection<String> projectKeys);

    @Query("""
            SELECT pm.projectKey AS projectKey, pm.role AS role
            FROM ProjectMember pm
            WHERE pm.projectKey IN :projectKeys
              AND pm.member.id = :memberId
              AND pm.softDeleted = false
            """)
    List<ProjectMemberRoleRow> findRolesByProjectKeys(
            @Param("projectKeys") Collection<String> projectKeys, @Param("memberId") Long memberId);
}
