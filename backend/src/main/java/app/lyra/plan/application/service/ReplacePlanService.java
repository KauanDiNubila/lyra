package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.ReplacePlanUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlanNotFoundException;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class ReplacePlanService implements ReplacePlanUseCase {

    private final PlanRepository planRepository;
    private final PlanFoodChecker foodChecker;

    @Override
    @Transactional
    public DietPlan replace(UserId userId, UUID id, String name, DailyTargets targets, List<PlannedMeal> meals) {
        planRepository.lockPlanChanges();
        DietPlan plan = planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
        DietPlan changed = plan.withContent(name, targets, meals);
        foodChecker.check(userId, changed);
        planRepository.save(changed);
        return changed;
    }
}
