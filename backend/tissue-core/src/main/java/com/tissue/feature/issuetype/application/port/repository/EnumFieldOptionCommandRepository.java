package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.EnumFieldOption;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface EnumFieldOptionCommandRepository extends Repository<EnumFieldOption, Long> {

    EnumFieldOption save(EnumFieldOption option);

    List<EnumFieldOption> saveAll(Iterable<EnumFieldOption> options);

    void delete(EnumFieldOption enumFieldOption);
}
