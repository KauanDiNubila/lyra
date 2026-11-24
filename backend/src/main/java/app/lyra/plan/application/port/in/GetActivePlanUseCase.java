package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DietPlan;

import java.util.Optional;

public interface GetActivePlanUseCase {

    Optional<DietPlan> getActivePlan();
}
