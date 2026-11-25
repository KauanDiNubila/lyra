package app.lyra.weight.application.service;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.in.DeleteWeightUseCase;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.domain.WeightNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
class DeleteWeightService implements DeleteWeightUseCase {

    private final WeightRepository weightRepository;

    @Override
    public void delete(UserId userId, LocalDate date) {
        if (!weightRepository.delete(userId, date)) {
            throw new WeightNotFoundException(date);
        }
    }
}
