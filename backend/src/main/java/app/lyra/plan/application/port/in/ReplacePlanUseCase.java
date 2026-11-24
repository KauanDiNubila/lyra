package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;

import java.util.List;
import java.util.UUID;

public interface ReplacePlanUseCase {

    DietPlan replace(UserId userId, UUID id, String name, DailyTargets targets, List<PlannedMeal> meals);
}
