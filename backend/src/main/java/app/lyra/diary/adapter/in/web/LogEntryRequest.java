package app.lyra.diary.adapter.in.web;

import app.lyra.diary.application.port.in.LogEntryUseCase.LogEntryCommand;
import app.lyra.shared.domain.InvalidInputException;

import java.time.LocalDate;
import java.util.UUID;

record LogEntryRequest(UUID foodId, String mealName, QuantityJson quantity) {

    LogEntryCommand toCommand(LocalDate date) {
        if (foodId == null) {
            throw new InvalidInputException("food id is required");
        }
        if (quantity == null) {
            throw new InvalidInputException("quantity is required");
        }
        return new LogEntryCommand(date, mealName, foodId, quantity.toDomain());
    }
}
