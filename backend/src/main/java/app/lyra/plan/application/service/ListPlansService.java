package app.lyra.plan.application.service;

import app.lyra.plan.application.port.in.ListPlansUseCase;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class ListPlansService implements ListPlansUseCase {

    private final PlanRepository planRepository;

    @Override
    public List<DietPlan> listPlans() {
        return planRepository.findAll();
    }
}
