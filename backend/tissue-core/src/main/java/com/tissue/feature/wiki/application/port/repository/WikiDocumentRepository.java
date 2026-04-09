package com.tissue.feature.wiki.application.port.repository;

import com.tissue.feature.wiki.domain.WikiDocument;
import org.springframework.data.repository.Repository;

public interface WikiDocumentRepository extends Repository<WikiDocument, Long> {}
