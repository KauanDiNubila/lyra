package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.CreatePlanUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class CreatePlanService implements CreatePlanUseCase {

    private final PlanRepository planRepository;
    private final PlanFoodChecker foodChecker;

    @Override
    public DietPlan create(UserId userId, String name, DailyTargets targets, List<PlannedMeal> meals) {
        DietPlan plan = DietPlan.create(name, targets, meals);
        foodChecker.check(plan);
        planRepository.save(plan);
        return plan;
    }
}
