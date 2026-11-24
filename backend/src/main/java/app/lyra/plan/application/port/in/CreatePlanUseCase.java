package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;

import java.util.List;

public interface CreatePlanUseCase {

    DietPlan create(String name, DailyTargets targets, List<PlannedMeal> meals);
}
