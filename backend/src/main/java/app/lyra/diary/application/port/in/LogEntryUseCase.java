package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;

import java.time.LocalDate;
import java.util.UUID;

public interface LogEntryUseCase {

    DiaryEntry log(UserId userId, LogEntryCommand command);

    record LogEntryCommand(LocalDate date, String mealName, UUID foodId, Quantity quantity) {
    }
}
