package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;

import java.util.UUID;

public interface ChangeEntryUseCase {

    DiaryEntry change(UUID entryId, ChangeEntryCommand command);

    record ChangeEntryCommand(UUID foodId, Quantity quantity, String mealName) {

        public ChangeEntryCommand {
            if (foodId == null && quantity == null && mealName == null) {
                throw new InvalidInputException("Nothing to change: send a quantity, a food or a meal");
            }
            if (foodId != null && quantity == null) {
                throw new InvalidInputException("Changing the food needs the quantity");
            }
        }
    }
}
