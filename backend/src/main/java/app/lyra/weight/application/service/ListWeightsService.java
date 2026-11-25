package app.lyra.weight.application.service;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.in.ListWeightsUseCase;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.domain.WeightEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ListWeightsService implements ListWeightsUseCase {

    private final WeightRepository weightRepository;

    @Override
    public List<WeightEntry> listWeights(UserId userId) {
        return weightRepository.findAll(userId);
    }
}
