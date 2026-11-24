package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.GetActivePlanUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class GetActivePlanService implements GetActivePlanUseCase {

    private final PlanRepository planRepository;

    @Override
    public Optional<DietPlan> getActivePlan() {
        return planRepository.findActive();
    }
}
