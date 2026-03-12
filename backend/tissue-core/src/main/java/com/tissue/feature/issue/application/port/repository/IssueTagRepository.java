package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.IssueTag;
import com.tissue.feature.tag.domain.Tag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface IssueTagRepository extends Repository<IssueTag, Long> {

    @Modifying
    @Query("DELETE FROM IssueTag it WHERE it.tag = :tag")
    void deleteAllByTag(Tag tag);
}
