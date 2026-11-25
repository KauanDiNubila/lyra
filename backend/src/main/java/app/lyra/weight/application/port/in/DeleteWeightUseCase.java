package app.lyra.weight.application.port.in;

import app.lyra.shared.domain.UserId;

import java.time.LocalDate;

public interface DeleteWeightUseCase {

    void delete(UserId userId, LocalDate date);
}
