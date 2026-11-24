package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;

import java.util.List;

public interface CreatePlanUseCase {

    DietPlan create(UserId userId, String name, DailyTargets targets, List<PlannedMeal> meals);
}
