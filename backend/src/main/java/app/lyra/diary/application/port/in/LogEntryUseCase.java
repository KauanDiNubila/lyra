package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Quantity;

import java.time.LocalDate;
import java.util.UUID;

public interface LogEntryUseCase {

    DiaryEntry log(LogEntryCommand command);

    record LogEntryCommand(LocalDate date, String mealName, UUID foodId, Quantity quantity) {
    }
}
