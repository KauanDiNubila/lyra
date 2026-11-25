package app.lyra.weight.application.port.out;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.domain.WeightEntry;

import java.time.LocalDate;
import java.util.List;

public interface WeightRepository {

    void save(UserId owner, WeightEntry entry);

    List<WeightEntry> findAll(UserId user);

    boolean delete(UserId user, LocalDate date);
}
