package app.lyra.weight.application.service;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.in.RecordWeightUseCase;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.domain.WeightEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
class RecordWeightService implements RecordWeightUseCase {

    private final WeightRepository weightRepository;

    @Override
    public WeightEntry recordWeight(UserId userId, LocalDate date, BigDecimal kilograms) {
        WeightEntry entry = new WeightEntry(date, kilograms);
        weightRepository.save(userId, entry);
        return entry;
    }
}
