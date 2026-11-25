package app.lyra.weight.application.port.in;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.domain.WeightEntry;

import java.util.List;

public interface ListWeightsUseCase {

    List<WeightEntry> listWeights(UserId userId);
}
