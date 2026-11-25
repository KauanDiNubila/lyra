package app.lyra.weight.application.port.in;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.domain.WeightEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RecordWeightUseCase {

    WeightEntry recordWeight(UserId userId, LocalDate date, BigDecimal kilograms);
}
