package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.ActivatePlanUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlanNotFoundException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class ActivatePlanService implements ActivatePlanUseCase {

    private final PlanRepository planRepository;

    @Override
    @Transactional
    public DietPlan activate(UserId userId, UUID id) {
        planRepository.lockPlanChanges();
        DietPlan plan = planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
        if (plan.isActive()) {
            return plan;
        }
        planRepository.findActive().ifPresent(current -> planRepository.save(current.deactivated()));
        DietPlan activated = plan.activated();
        planRepository.save(activated);
        return activated;
    }
}
