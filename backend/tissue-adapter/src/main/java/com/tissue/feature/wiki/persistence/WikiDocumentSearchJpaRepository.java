package com.tissue.feature.wiki.persistence;

import com.tissue.feature.wiki.domain.WikiDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WikiDocumentSearchJpaRepository
        extends JpaRepository<WikiDocument, Long>, JpaSpecificationExecutor<WikiDocument> {}
