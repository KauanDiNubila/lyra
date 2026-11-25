package app.lyra.identity.application.port.in;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Food;
import app.lyra.identity.domain.User;
import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;
import app.lyra.weight.domain.WeightEntry;

import java.util.List;

public interface ExportAccountUseCase {

    AccountExport export(UserId userId);

    record AccountExport(User user, List<Food> customFoods, List<DietPlan> plans, List<DiaryEntry> diaryEntries,
                         List<WeightEntry> weights) {
    }
}
