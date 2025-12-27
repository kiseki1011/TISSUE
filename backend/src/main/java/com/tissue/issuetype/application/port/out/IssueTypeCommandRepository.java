package com.tissue.issuetype.application.port.out;

import com.tissue.issuetype.domain.IssueType;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface IssueTypeCommandRepository extends Repository<IssueType, Long> {

    IssueType save(IssueType issueType);

    List<IssueType> saveAll(Iterable<IssueType> issueTypes);

    void delete(IssueType issueType);
}
