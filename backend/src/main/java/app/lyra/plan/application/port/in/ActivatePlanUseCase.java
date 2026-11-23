package app.lyra.plan.application.port.in;

import app.lyra.plan.domain.DietPlan;

import java.util.UUID;

public interface ActivatePlanUseCase {

    DietPlan activate(UUID id);
}
