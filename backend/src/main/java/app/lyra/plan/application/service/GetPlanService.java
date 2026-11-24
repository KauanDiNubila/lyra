package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.GetPlanUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlanNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class GetPlanService implements GetPlanUseCase {

    private final PlanRepository planRepository;

    @Override
    public DietPlan getPlan(UUID id) {
        return planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
    }
}
