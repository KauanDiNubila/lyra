package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;

import java.util.List;
import java.util.UUID;

public interface ReplacePlanUseCase {

    DietPlan replace(UUID id, String name, DailyTargets targets, List<PlannedMeal> meals);
}
