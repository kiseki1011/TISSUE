package com.tissue.feature.sprint.application.port.out;

import com.tissue.feature.sprint.domain.Sprint;
import org.springframework.data.repository.Repository;

public interface SprintCommandRepository extends Repository<Sprint, Long> {

    Sprint save(Sprint sprint);
}
