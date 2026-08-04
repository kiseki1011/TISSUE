package com.tissue.feature.agent.model.application.service;

import com.tissue.feature.agent.model.application.dto.response.AiModelSummary;
import com.tissue.feature.agent.model.application.port.repository.AiModelRepository;
import com.tissue.feature.agent.model.application.port.usecase.AiModelQueryUseCase;
import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.feature.member.application.service.MemberFinder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AiModelQueryService implements AiModelQueryUseCase {

    private final AiModelRepository aiModelRepository;
    private final MemberFinder memberFinder;

    @Override
    public List<AiModelSummary> getModels(Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        List<AiModel> models = aiModelRepository.findAllOrderById();

        return models.stream().map(AiModelSummary::from).toList();
    }
}
