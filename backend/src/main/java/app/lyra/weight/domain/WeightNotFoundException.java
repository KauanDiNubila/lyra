package app.lyra.weight.domain;

import app.lyra.shared.domain.NotFoundException;

import java.time.LocalDate;

public class WeightNotFoundException extends NotFoundException {

    public WeightNotFoundException(LocalDate date) {
        super("Weight not found for " + date);
    }
}
