package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.GetActivePlanUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class GetActivePlanService implements GetActivePlanUseCase {

    private final PlanRepository planRepository;

    @Override
    public Optional<DietPlan> getActivePlan(UserId userId) {
        return planRepository.findActive(userId);
    }
}
