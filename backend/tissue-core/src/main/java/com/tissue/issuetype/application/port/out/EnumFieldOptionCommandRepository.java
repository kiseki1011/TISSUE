package com.tissue.issuetype.application.port.out;

import com.tissue.issuetype.domain.EnumFieldOption;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface EnumFieldOptionCommandRepository extends Repository<EnumFieldOption, Long> {

    EnumFieldOption save(EnumFieldOption option);

    List<EnumFieldOption> saveAll(Iterable<EnumFieldOption> options);

    void delete(EnumFieldOption enumFieldOption);
}
