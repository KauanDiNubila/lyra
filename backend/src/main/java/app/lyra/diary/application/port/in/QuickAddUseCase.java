package app.lyra.diary.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Nutrients;
import app.lyra.shared.domain.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface QuickAddUseCase {

    DiaryEntry quickAdd(UserId userId, QuickAddCommand command);

    record QuickAddCommand(LocalDate date, String mealName, String name, BigDecimal portions, Nutrients nutrients) {
    }
}
