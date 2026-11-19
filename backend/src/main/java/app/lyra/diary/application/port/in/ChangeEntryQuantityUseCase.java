package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Quantity;

import java.util.UUID;

public interface ChangeEntryQuantityUseCase {

    DiaryEntry changeQuantity(UUID entryId, Quantity quantity);
}
